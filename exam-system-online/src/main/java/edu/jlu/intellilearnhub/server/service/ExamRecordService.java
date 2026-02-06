package edu.jlu.intellilearnhub.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.List;

/**
 * 考试记录Service接口
 * 定义考试记录相关的业务方法
 */
public interface ExamRecordService extends IService<ExamRecord> {

    void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName, String studentNumber, Integer status, String startDate, String endDate);

    void fixUpdateRecordStatus(Page<ExamRecord> examRecordPage);
}