package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.entity.AnswerRecord;
import edu.jlu.intellilearnhub.server.mapper.AnswerRecordMapper;
import edu.jlu.intellilearnhub.server.service.AnswerRecordService;
import org.springframework.stereotype.Service;

@Service
public class AnswerRecordServiceImpl extends ServiceImpl<AnswerRecordMapper, AnswerRecord> implements AnswerRecordService {
}
