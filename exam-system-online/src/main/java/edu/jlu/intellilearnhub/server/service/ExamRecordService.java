package edu.jlu.intellilearnhub.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import edu.jlu.intellilearnhub.server.vo.ExamRankingVO;

import java.util.List;

/**
 * 考试记录Service接口
 * 定义考试记录相关的业务方法
 */
public interface ExamRecordService extends IService<ExamRecord> {

    void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName, String studentNumber, Integer status, String startDate, String endDate);

    void fixUpdateRecordStatus(Page<ExamRecord> examRecordPage);

    void removeExamRecordById(Long id);

    List<ExamRankingVO> getExamRanking(Long paperId, Integer limit);
}