package edu.jlu.intellilearnhub.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.langchain4j.agent.tool.P;
import edu.jlu.intellilearnhub.server.common.BusinessConstants;
import edu.jlu.intellilearnhub.server.entity.AnswerRecord;
import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import edu.jlu.intellilearnhub.server.entity.Paper;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.AnswerRecordMapper;
import edu.jlu.intellilearnhub.server.mapper.ExamRecordMapper;
import edu.jlu.intellilearnhub.server.mapper.PaperMapper;
import edu.jlu.intellilearnhub.server.service.ExamRecordService;
import edu.jlu.intellilearnhub.server.vo.ExamRankingVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    private final AnswerRecordMapper answerRecordMapper;

    public ExamRecordServiceImpl(PaperMapper paperMapper, AnswerRecordMapper answerRecordMapper) {
        this.paperMapper = paperMapper;
        this.answerRecordMapper = answerRecordMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeExamRecordById(Long id) {
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            return;
        }
        if (BusinessConstants.ExamRecordStatus.DOING.getStatus().equals(examRecord.getStatus())) {
            throw new CommonException("id=%s的考试记录正在进行中，无法删除！".formatted(id));
        }
        removeById(id);
        answerRecordMapper.delete(new LambdaUpdateWrapper<AnswerRecord>()
                .eq(AnswerRecord::getExamRecordId, id)
        );
    }

    @Override
    public List<ExamRankingVO> getExamRanking(Long paperId, Integer limit) {
        List<ExamRecord> examRecords = list(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, paperId)
                .eq(ExamRecord::getStatus, BusinessConstants.ExamRecordStatus.CHECKED.getStatus())
                .orderByDesc(ExamRecord::getScore)
                .orderByAsc(ExamRecord::getEndTime)
                .orderByAsc(ExamRecord::getEndTime)
                .last("limit " + limit)
        );
        Paper paper = paperMapper.selectById(paperId);
        return examRecords.stream().map(examRecord -> {
            ExamRankingVO examRankingVO = new ExamRankingVO();
            examRankingVO.setId(examRecord.getId());
            examRankingVO.setStudentName(examRecord.getStudentName());
            examRankingVO.setScore(examRecord.getScore());
            examRankingVO.setExamId(examRecord.getExamId());
            examRankingVO.setPaperName(paper.getName());
            examRankingVO.setPaperTotalScore(paper.getTotalScore());
            examRankingVO.setStartTime(examRecord.getStartTime());
            examRankingVO.setEndTime(examRecord.getEndTime());
            examRankingVO.setDuration(Duration.between(examRecord.getStartTime(), examRecord.getEndTime()).toMinutes());
            return examRankingVO;
        }).toList();
    }
}