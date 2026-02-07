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
        Paper paper = paperService.getPaperWithOutAnswerById(examRecord.getExamId());
        examRecord.setPaper(paper);

        // 获取答题记录
        fillWithExamAnswerRecords(id, paper, examRecord);

        return examRecord;
    }

    private void fillWithExamAnswerRecords(Long id, Paper paper, ExamRecord examRecord) {
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

        gradeExam(examRecordId);
    }

    @Override
    public ExamRecord getExamRecordWithAnswerById(Long id) {
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new CommonException("考试记录已经被删除，无法查看");
        }
        if (BusinessConstants.ExamRecordStatus.DOING.getStatus().equals(examRecord.getStatus())) {
            throw new CommonException("考试正在进行中，无法查看考试结果");
        }
        Paper paper = paperService.getPaperById(examRecord.getExamId());
        examRecord.setPaper(paper);
        // 获取答题记录
        fillWithExamAnswerRecords(id, paper, examRecord);
        return examRecord;
    }

    @Override
    public ExamRecord gradeExam(Long examRecordId) {
        ExamRecord examRecord = getExamRecordWithAnswerById(examRecordId);
        if (!BusinessConstants.ExamRecordStatus.FINISHED.getStatus().equals(examRecord.getStatus())) {
            throw new CommonException("试卷批阅失败，只有已提交的试卷才可以批阅");
        }
        Paper paper = examRecord.getPaper();
        if (paper == null) {
            examRecord.setStatus(BusinessConstants.ExamRecordStatus.CHECKED.getStatus());
            examRecord.setAnswers("考试记录对应的试卷已经被删除，无法正常判卷");
            examRecord.setScore(0);
            updateById(examRecord);
            log.warn("id={}的考试记录没有正常判卷，因为其对应的试卷已经被删除", examRecordId);
            return examRecord;
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (CollectionUtils.isEmpty(answerRecords)) {
            examRecord.setStatus(BusinessConstants.ExamRecordStatus.CHECKED.getStatus());
            examRecord.setAnswers("学生提交了白卷，直接零分");
            examRecord.setScore(0);
            updateById(examRecord);
            log.warn("id={}的考试记录直接判为0分，因为学生没有答卷", examRecordId);
            return examRecord;
        }
        int correctQuestionCount = 0, totalScore = 0;
        Map<Long, Question> questionIdToQuestion = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        for (AnswerRecord answerRecord : answerRecords) {
            Question question = questionIdToQuestion.get(answerRecord.getQuestionId());
            // 如果题目已经被删除，那么直接判下一题
            if (question == null) {
                continue;
            }

            String correctAnswer = question.getAnswer().getAnswer();
            String studentAnswer = answerRecord.getUserAnswer();
            if ("JUDGE".equals(question.getType())) {
                studentAnswer = studentAnswerToTureOrFalse(studentAnswer);
            }

            try {
                if (!"TEXT".equals(question.getType())) {
                    if (studentAnswer.equalsIgnoreCase(correctAnswer)) {
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                    } else {
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                    }
                } else {
                    answerRecord.setIsCorrect(1);
                    answerRecord.setScore(question.getPaperScore().intValue());
                }
            } catch (Exception e) {
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
                answerRecord.setAiCorrection("判断过程中报错了，所以此题直接0分");
            }
            totalScore += answerRecord.getScore();
            if (answerRecord.getIsCorrect() == 1) {
                correctQuestionCount++;
            }
        }
        answerRecordService.updateBatchById(answerRecords);
        examRecord.setScore(totalScore);
        examRecord.setStatus(BusinessConstants.ExamRecordStatus.CHECKED.getStatus());
        updateById(examRecord);
        return examRecord;
    }

    private String studentAnswerToTureOrFalse(String studentAnswer) {
        studentAnswer = studentAnswer.toUpperCase();
        return switch (studentAnswer) {
            case "T", "正确", "对", "TRUE" -> "TRUE";
            case "F", "错误", "错", "不对", "FALSE" -> "FALSE";
            default -> studentAnswer;
        };
    }
}