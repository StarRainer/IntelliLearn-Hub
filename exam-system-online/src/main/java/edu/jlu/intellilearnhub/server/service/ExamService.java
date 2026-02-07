package edu.jlu.intellilearnhub.server.service;

import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.vo.StartExamVo;
import edu.jlu.intellilearnhub.server.vo.SubmitAnswerVo;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {

    ExamRecord saveExam(StartExamVo startExamVo);

    ExamRecord getExamRecordById(Long id);

    void submitAnswers(Long examRecordId, List<SubmitAnswerVo> submitAnswerVos);

    ExamRecord gradeExam(Long examRecordId);

    ExamRecord getExamRecordWithAnswerById(Long id);
}
 