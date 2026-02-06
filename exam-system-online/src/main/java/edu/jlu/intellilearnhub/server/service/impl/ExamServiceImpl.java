package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jlu.intellilearnhub.server.common.BusinessConstants;
import edu.jlu.intellilearnhub.server.entity.*;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.AnswerRecordMapper;
import edu.jlu.intellilearnhub.server.mapper.ExamRecordMapper;
import edu.jlu.intellilearnhub.server.mapper.PaperMapper;
import edu.jlu.intellilearnhub.server.service.AnswerRecordService;
import edu.jlu.intellilearnhub.server.service.ExamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.service.PaperService;
import edu.jlu.intellilearnhub.server.vo.StartExamVo;
import edu.jlu.intellilearnhub.server.vo.SubmitAnswerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperService paperService;
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    @Autowired
    private AnswerRecordService answerRecordService;

    @Override
    public ExamRecord saveExam(StartExamVo startExamVo) {
        ExamRecord examRecord = getOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, startExamVo.getPaperId())
                .eq(ExamRecord::getStudentName, startExamVo.getStudentName())
                .last("limit 1")
        );
        // 第一次考试
        if (examRecord == null) {
            examRecord = new ExamRecord();
            examRecord.setExamId(startExamVo.getPaperId());
            examRecord.setStudentName(startExamVo.getStudentName());
            examRecord.setStatus(BusinessConstants.ExamRecordStatus.DOING.getStatus());
            examRecord.setStartTime(LocalDateTime.now());
            examRecord.setWindowSwitches(0);

            Paper paper = paperMapper.selectById(examRecord.getExamId());
            LocalDateTime endTime = examRecord.getStartTime().plusMinutes(paper.getDuration());
            examRecord.setEndTime(endTime);

            save(examRecord);
            return examRecord;
        }

        // 如果已经交卷，禁止考试
        if (BusinessConstants.ExamRecordStatus.FINISHED.getStatus().equals(examRecord.getStatus()) ||
            BusinessConstants.ExamRecordStatus.CHECKED.getStatus().equals(examRecord.getStatus())) {
            return examRecord;
        }

        // 计算是否超时，如果超时更新考试状态
        if (examRecord.getEndTime().isBefore(LocalDateTime.now())) {
            examRecord.setStatus(BusinessConstants.ExamRecordStatus.FINISHED.getStatus());
            updateById(examRecord);
        }
        return examRecord;
    }

    @Override
    public ExamRecord getExamRecordById(Long id) {
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new CommonException("考试记录已经被删除，无法查看");
        }

        // 获取试卷
        Paper paper = paperService.getPaperById(examRecord.getExamId());
        examRecord.setPaper(paper);

        // 获取答题记录
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getExamRecordId, id)
        );

        if (!CollectionUtils.isEmpty(answerRecords)) {
            List<Long> questionIds = paper.getQuestions().stream()
                    .map(Question::getId)
                    .toList();

            Map<Long, AnswerRecord> answerMap = answerRecords.stream()
                    .collect(Collectors.toMap(AnswerRecord::getQuestionId, Function.identity()));

            List<AnswerRecord> sortedAnswerRecords = questionIds.stream()
                    .map(answerMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            examRecord.setAnswerRecords(sortedAnswerRecords);
        }

        return examRecord;
    }

    @Override
    @Transactional(rollbackFor = CommonException.class)
    public void submitAnswers(Long examRecordId, List<SubmitAnswerVo> submitAnswerVos) {
        ExamRecord examRecord = getById(examRecordId);
        if (!BusinessConstants.ExamRecordStatus.DOING.getStatus().equals(examRecord.getStatus())) {
            return;
        }

        if (!CollectionUtils.isEmpty(submitAnswerVos)) {
            List<AnswerRecord> answerRecords = submitAnswerVos.stream()
                    .map(submitAnswerVo -> {
                        AnswerRecord answerRecord = new AnswerRecord();
                        BeanUtils.copyProperties(submitAnswerVo, answerRecord);
                        answerRecord.setExamRecordId(examRecordId);
                        return answerRecord;
                    })
                    .toList();
            answerRecordService.saveBatch(answerRecords);
        }

        LocalDateTime now = LocalDateTime.now();
        if (examRecord.getEndTime().isAfter(now)) {
            examRecord.setEndTime(now);
        }
        examRecord.setStatus(BusinessConstants.ExamRecordStatus.FINISHED.getStatus());
        updateById(examRecord);
    }
}