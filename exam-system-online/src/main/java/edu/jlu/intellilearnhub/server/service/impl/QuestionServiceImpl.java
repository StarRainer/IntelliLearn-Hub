package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {
    

} 