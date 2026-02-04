package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.entity.QuestionAnswer;
import edu.jlu.intellilearnhub.server.entity.QuestionChoice;
import edu.jlu.intellilearnhub.server.mapper.QuestionAnswerMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionChoiceMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
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
}