package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.common.CacheConstants;
import edu.jlu.intellilearnhub.server.entity.PaperQuestion;
import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.entity.QuestionAnswer;
import edu.jlu.intellilearnhub.server.entity.QuestionChoice;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.PaperQuestionMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionAnswerMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionChoiceMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.QuestionService;
import edu.jlu.intellilearnhub.server.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;


    @Override
    public void listQuestion(Page<Question> questionPage, QuestionQueryVo questionQueryVo) {
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        if (!ObjectUtils.isEmpty(questionQueryVo.getCategoryId())) {
            queryWrapper.eq(Question::getCategoryId, questionQueryVo.getCategoryId());
        }
        if (StringUtils.hasText(questionQueryVo.getDifficulty())) {
            queryWrapper.eq(Question::getDifficulty, questionQueryVo.getDifficulty());
        }
        if (StringUtils.hasText(questionQueryVo.getType())) {
            queryWrapper.eq(Question::getType, questionQueryVo.getType());
        }
        if (StringUtils.hasText(questionQueryVo.getKeyword())) {
            queryWrapper.like(Question::getTitle, questionQueryVo.getKeyword());
        }
        page(questionPage, queryWrapper);
        if (CollectionUtils.isEmpty(questionPage.getRecords())) {
            return;
        }
        List<Question> questions = questionPage.getRecords();
        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        Map<Long, QuestionAnswer> questionIdToQuestionAnswer = questionAnswerMapper.selectList(new LambdaQueryWrapper<QuestionAnswer>()
                .in(QuestionAnswer::getQuestionId, questionIds)
        ).stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity(), (a, b) -> a));

        Map<Long, List<QuestionChoice>> questionIdToQuestionChoices = questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>()
                .in(QuestionChoice::getQuestionId, questionIds)
                .orderByAsc(QuestionChoice::getSort)
        ).stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

       questions.forEach(question -> {
            question.setAnswer(questionIdToQuestionAnswer.getOrDefault(question.getId(), null));
            question.setChoices(questionIdToQuestionChoices.getOrDefault(question.getId(), null));
        });
    }

    @Override
    public Question getQuestionById(Long id) {
        Question question = getById(id);
        if (question == null) {
            return null;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Double score = redisTemplate.opsForZSet().incrementScore(CacheConstants.POPULAR_QUESTIONS_KEY, id, 1);
                log.debug("完成id={}的题目的热榜分数统计：score:{}", id, score);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, threadPoolExecutor);

        List<QuestionChoice> questionChoices = Collections.emptyList();
        if ("CHOICE".equals(question.getType())) {
             questionChoices = questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>()
                    .eq(QuestionChoice::getQuestionId, id)
                    .orderByAsc(QuestionChoice::getSort)
            );
        }
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(new LambdaQueryWrapper<QuestionAnswer>()
                .eq(QuestionAnswer::getQuestionId, id)
                .last("limit 1")
        );
        question.setChoices(questionChoices);
        question.setAnswer(questionAnswer);
        return question;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuestion(Question question) {
        long count = count(new LambdaQueryWrapper<Question>()
                .eq(Question::getType, question.getType())
                .eq(Question::getTitle, question.getTitle())
        );
        if (count > 0) {
            throw new CommonException("保存失败，在%s类型下，已经存在内容为%s的题目".formatted(question.getType(), question.getTitle()));
        }
        save(question);
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> questionChoices = question.getChoices();
            if (CollectionUtils.isEmpty(questionChoices)) {
                throw new CommonException("保存失败，选择题必须有选项可供选择");
            }

            String answer = generateAnswerAndInsertQuestionChoices(question, questionChoices);

            QuestionAnswer questionAnswer = question.getAnswer();
            if (questionAnswer == null) {
                throw new CommonException("保存失败，必须为题目提供答案");
            }
            questionAnswer.setQuestionId(question.getId());
            questionAnswer.setAnswer(answer);
        }
        if (question.getAnswer() == null || !StringUtils.hasText(question.getAnswer().getAnswer())) {
            throw new CommonException("保存失败，题目必须有答案");
        }
        question.getAnswer().setQuestionId(question.getId());
        questionAnswerMapper.insert(question.getAnswer());
    }

    private String generateAnswerAndInsertQuestionChoices(Question question, List<QuestionChoice> questionChoices) {
        StringJoiner stringJoiner = new StringJoiner(",");
        for (int i = 0; i < questionChoices.size(); i++) {
            QuestionChoice questionChoice = questionChoices.get(i);
            questionChoice.setSort(i);
            questionChoice.setQuestionId(question.getId());
            if (Boolean.TRUE.equals(questionChoice.getIsCorrect())) {
                stringJoiner.add(String.valueOf((char)('A' + i)));
            }
            if (questionChoice.getId() != null) {
                questionChoice.setId(null);
                questionChoice.setCreateTime(null);
                questionChoice.setUpdateTime(null);
            }
            questionChoiceMapper.insert(questionChoice);
        }

        if (Boolean.TRUE.equals(question.getMulti())) {
            if (stringJoiner.length() < 2) {
                throw new CommonException("保存失败，多选题至少设置两个正确的选项");
            }
        } else {
            if (stringJoiner.length() != 1) {
                throw new CommonException("保存失败，单选题必须设置一个正确的选项");
            }
        }
        return stringJoiner.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Question question) {
        long count = count(new LambdaQueryWrapper<Question>()
                .eq(Question::getType, question.getType())
                .eq(Question::getTitle, question.getTitle())
                .ne(Question::getId, question.getId())
        );
        if (count > 0) {
            throw new CommonException("修改失败，在%s类型下，已经存在内容为%s的题目".formatted(question.getType(), question.getTitle()));
        }
        updateById(question);
        if ("CHOICE".equals(question.getType())) {
            questionChoiceMapper.delete(new LambdaUpdateWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, question.getId()));
            List<QuestionChoice> questionChoices = question.getChoices();
            if (CollectionUtils.isEmpty(questionChoices)) {
                throw new CommonException("修改失败，选择题必须有选项可供选择");
            }

            String answer = generateAnswerAndInsertQuestionChoices(question, questionChoices);

            QuestionAnswer questionAnswer = question.getAnswer();
            if (questionAnswer == null) {
                throw new CommonException("修改失败，必须为题目提供答案");
            }
            questionAnswer.setAnswer(answer);
        }

        if (question.getAnswer() == null || !StringUtils.hasText(question.getAnswer().getAnswer())) {
            throw new CommonException("保存失败，题目必须有答案");
        }
        questionAnswerMapper.updateById(question.getAnswer());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestion(Long id) {
        Long count = paperQuestionMapper.selectCount(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getQuestionId, id)
        );
        if (count > 0) {
            throw new CommonException("删除失败，id为%s的试题正在被%s份试卷引用".formatted(id, count));
        }
        removeById(id);
        questionChoiceMapper.delete(new LambdaUpdateWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, id));
        questionAnswerMapper.delete(new LambdaUpdateWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId, id));
    }
}