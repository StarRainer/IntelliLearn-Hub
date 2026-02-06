package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.entity.PaperQuestion;
import edu.jlu.intellilearnhub.server.mapper.PaperQuestionMapper;
import edu.jlu.intellilearnhub.server.service.PaperQuestionService;
import org.springframework.stereotype.Service;

@Service
public class PaperQuestionServiceImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements PaperQuestionService {

}
