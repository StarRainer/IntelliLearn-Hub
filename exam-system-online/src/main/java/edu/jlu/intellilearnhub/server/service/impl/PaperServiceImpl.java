package edu.jlu.intellilearnhub.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.common.BusinessConstants;
import edu.jlu.intellilearnhub.server.entity.*;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.*;
import edu.jlu.intellilearnhub.server.service.PaperQuestionService;
import edu.jlu.intellilearnhub.server.service.PaperService;
import edu.jlu.intellilearnhub.server.vo.AiPaperVo;
import edu.jlu.intellilearnhub.server.vo.PaperVo;
import edu.jlu.intellilearnhub.server.vo.RuleVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private PaperQuestionService paperQuestionService;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Override
    public List<Paper> listPapers(String name, String status) {
        return list(new LambdaQueryWrapper<Paper>()
                .like(!ObjectUtils.isEmpty(name), Paper::getName, name)
                .eq(!ObjectUtils.isEmpty(status), Paper::getStatus, status)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Paper createPaper(PaperVo paperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperVo, paper);
        paper.setStatus(BusinessConstants.PaperStatus.DRAFT.getStatus());
        if (ObjectUtils.isEmpty(paperVo.getQuestions())) {
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            save(paper);
            log.warn("该试卷没有组装题目，暂时不能用于考试：paper={}", paper);
            return paper;
        }
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        paper.setQuestionCount(paperVo.getQuestions().size());
        log.debug("当前试卷勾选了题目，能够正常进行计算和保存paper={}", paper);
        save(paper);
        List<PaperQuestion> paperQuestions = paperVo.getQuestions().entrySet().stream().map(entry -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paper.getId());
            paperQuestion.setQuestionId(entry.getKey());
            paperQuestion.setScore(entry.getValue());
            return paperQuestion;
        }).toList();
        paperQuestionService.saveBatch(paperQuestions);
        return paper;
    }

    @Override
    public Paper getPaperById(Long id) {
        Paper paper = getById(id);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, id)
        );
        if (CollectionUtils.isEmpty(paperQuestions)) {
            paper.setQuestions(Collections.emptyList());
            return paper;
        }
        List<Long> questionIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, BigDecimal> questionIdToScore = paperQuestions.stream().collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        Map<Long, List<QuestionChoice>> questionIdToQuestionChoices = questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>()
                .in(QuestionChoice::getQuestionId, questionIds)
                .orderByAsc(QuestionChoice::getSort)
        ).stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .in(Question::getId, questionIds)
        ).stream().map(question -> {
            question.setPaperScore(questionIdToScore.get(question.getId()));
            question.setChoices(questionIdToQuestionChoices.get(question.getId()));
            return question;
        }).sorted(
                Comparator.comparingInt((Question q) -> {
                    String type = q.getType();
                    if ("CHOICE".equals(type)) return 1;
                    if ("JUDGE".equals(type)) return 2;
                    if ("TEXT".equals(type)) return 3;
                    return 4;
                })
                .thenComparing(Question::getId)
        )
        .toList();
        paper.setQuestions(questions);
        return paper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaperById(Long id) {
        Paper paper = getById(id);
        if (BusinessConstants.PaperStatus.PUBLISHED.getStatus().equals(paper.getStatus())) {
            throw new CommonException("id=%s的试卷处于发布状态，禁止删除".formatted(id));
        }
        Long count = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, id)
        );
        if (count > 0) {
            throw new CommonException("id=%s的试卷已经关联了%s条考试记录，无法直接删除".formatted(id, count));
        }
        removeById(id);
        examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, id)
        );
    }

    @Override
    public void updatePaperStatus(Long id, String status) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setStatus(status);
        updateById(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Paper updatePaper(Long id, PaperVo paperVo) {
        Paper paper = getById(id);
        if (BusinessConstants.PaperStatus.PUBLISHED.getStatus().equals(paper.getStatus())) {
            throw new CommonException("更新失败，发布状态的试卷不可以被更新");
        }
        long count = count(new LambdaQueryWrapper<Paper>()
                .eq(Paper::getName, paperVo.getName())
                .ne(Paper::getId, id)
        );
        if (count > 0) {
            throw new CommonException("更新失败，更新后的试卷名称%s与其他试卷名称相同".formatted(paperVo.getName()));
        }
        BeanUtils.copyProperties(paperVo, paper);
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        paper.setQuestionCount(paperVo.getQuestions().size());
        updateById(paper);
        paperQuestionMapper.delete(new LambdaUpdateWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, id)
        );
        List<PaperQuestion> paperQuestions = paperVo.getQuestions().entrySet().stream().map(entry -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paper.getId());
            paperQuestion.setQuestionId(entry.getKey());
            paperQuestion.setScore(entry.getValue());
            return paperQuestion;
        }).toList();
        paperQuestionService.saveBatch(paperQuestions);
        return paper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Paper createPaperByAI(AiPaperVo aiPaperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo, paper);
        paper.setStatus(BusinessConstants.PaperStatus.DRAFT.getStatus());
        save(paper);
        int questionCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        for (RuleVo ruleVo : aiPaperVo.getRules()) {
            if (ruleVo.getCount() <= 0) {
                continue;
            }
            List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .eq(Question::getType, ruleVo.getType())
                    .in(!CollectionUtils.isEmpty(ruleVo.getCategoryIds()), Question::getCategoryId, ruleVo.getCategoryIds())
            );
            if (CollectionUtils.isEmpty(questions)) {
                continue;
            }
            int count = Math.min(ruleVo.getCount(), questions.size());
            questionCount += count;
            BigDecimal singleScore = BigDecimal.valueOf(ruleVo.getScore());
            totalScore = totalScore.add(singleScore.multiply(BigDecimal.valueOf(count)));
            Collections.shuffle(questions);
            paperQuestions.addAll(questions.subList(0, count).stream()
                    .map(question -> {
                        PaperQuestion paperQuestion = new PaperQuestion();
                        paperQuestion.setPaperId(paper.getId());
                        paperQuestion.setQuestionId(question.getId());
                        paperQuestion.setScore(BigDecimal.valueOf(ruleVo.getScore()));
                        return paperQuestion;
                    })
                    .toList()
            );
        }
        if (CollectionUtils.isEmpty(paperQuestions)) {
            throw new CommonException("未匹配到符合条件的题目，试卷生成失败");
        }

        paperQuestionService.saveBatch(paperQuestions);
        paper.setQuestionCount(questionCount);
        paper.setTotalScore(totalScore);
        updateById(paper);
        return paper;
    }
}