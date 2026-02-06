package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jlu.intellilearnhub.server.common.BusinessConstants;
import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import edu.jlu.intellilearnhub.server.entity.Paper;
import edu.jlu.intellilearnhub.server.mapper.ExamRecordMapper;
import edu.jlu.intellilearnhub.server.mapper.PaperMapper;
import edu.jlu.intellilearnhub.server.service.ExamRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考试记录Service实现类
 * 实现考试记录相关的业务逻辑
 */
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {


    private final PaperMapper paperMapper;

    public ExamRecordServiceImpl(PaperMapper paperMapper) {
        this.paperMapper = paperMapper;
    }

    @Override
    public void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName,  String studentNumber, Integer status, String startDate, String endDate) {

        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(studentName)) {
            queryWrapper.like(ExamRecord::getStudentName, studentName);
        }

        if (!ObjectUtils.isEmpty(status)) {
            String statusStr = switch (status) {
                case 0 -> BusinessConstants.ExamRecordStatus.DOING.getStatus();
                case 1 -> BusinessConstants.ExamRecordStatus.FINISHED.getStatus();
                case 2 -> BusinessConstants.ExamRecordStatus.CHECKED.getStatus();
                default -> null;
            };
            queryWrapper.eq(ExamRecord::getStatus, statusStr);
        }

        if (!ObjectUtils.isEmpty(startDate)) {
            queryWrapper.ge(ExamRecord::getStartTime, startDate);
        }

        if (!ObjectUtils.isEmpty(endDate)) {
            queryWrapper.le(ExamRecord::getEndTime, endDate);
        }
        page(examRecordPage, queryWrapper);
        if (CollectionUtils.isEmpty(examRecordPage.getRecords())) {
            return;
        }
        List<Long> paperIds = examRecordPage.getRecords().stream().map(ExamRecord::getExamId).toList();
        Map<Long, Paper> paperIdToPaper = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                .in(Paper::getId, paperIds)
        ).stream().collect(Collectors.toMap(Paper::getId, Function.identity()));
        examRecordPage.getRecords().forEach(paperRecord -> {
            paperRecord.setPaper(paperIdToPaper.get(paperRecord.getExamId()));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fixUpdateRecordStatus(Page<ExamRecord> examRecordPage) {
        List<ExamRecord> examRecords = examRecordPage.getRecords();
        if (CollectionUtils.isEmpty(examRecords)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = examRecords.stream().filter(examRecord -> {
            if (!BusinessConstants.ExamRecordStatus.DOING.getStatus().equals(examRecord.getStatus())) {
                return false;
            }
            if (now.isAfter(examRecord.getEndTime())) {
                examRecord.setStatus(BusinessConstants.ExamRecordStatus.FINISHED.getStatus());
                return true;
            }
            return false;
        }).map(ExamRecord::getId).toList();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ExamRecord examRecord = new ExamRecord();
        examRecord.setStatus(BusinessConstants.ExamRecordStatus.FINISHED.getStatus());
        update(examRecord, new LambdaQueryWrapper<ExamRecord>().in(ExamRecord::getId, ids));
    }
}