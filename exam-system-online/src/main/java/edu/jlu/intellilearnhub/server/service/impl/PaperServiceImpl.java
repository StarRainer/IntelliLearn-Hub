package edu.jlu.intellilearnhub.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jlu.intellilearnhub.server.common.BusinessConstants;
import edu.jlu.intellilearnhub.server.entity.Paper;
import edu.jlu.intellilearnhub.server.entity.PaperQuestion;
import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.mapper.PaperMapper;
import edu.jlu.intellilearnhub.server.mapper.PaperQuestionMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.PaperQuestionService;
import edu.jlu.intellilearnhub.server.service.PaperService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.vo.PaperVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.List;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private PaperQuestionService paperQuestionService;

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
        if (ObjectUtils.isEmpty(paper.getQuestions())) {
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
}