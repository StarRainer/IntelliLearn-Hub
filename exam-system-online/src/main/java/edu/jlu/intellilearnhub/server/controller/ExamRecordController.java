package edu.jlu.intellilearnhub.server.controller;

import edu.jlu.intellilearnhub.server.common.Result;
import edu.jlu.intellilearnhub.server.entity.Exam;
import edu.jlu.intellilearnhub.server.entity.ExamRecord;
import edu.jlu.intellilearnhub.server.service.ExamRecordService;
import edu.jlu.intellilearnhub.server.service.ExamService;
import edu.jlu.intellilearnhub.server.vo.ExamRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试记录控制器 - 处理考试记录管理相关的HTTP请求
 * 包括考试记录查询、分页展示、成绩排行榜等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/exam-records")  // 考试记录API路径前缀
@Tag(name = "考试记录管理", description = "考试记录相关操作，包括记录查询、成绩管理、排行榜展示等功能")  // Swagger API分组
@CrossOrigin
@Slf4j
public class ExamRecordController {


    private final ExamRecordService examRecordService;
    private final ExamService examService;

    public ExamRecordController(ExamRecordService examRecordService, ExamService examService) {
        this.examRecordService = examRecordService;
        this.examService = examService;
    }

    /**
     * 分页查询考试记录
     */
    @GetMapping("/list")  // 处理GET请求
    @Operation(summary = "分页查询考试记录", description = "支持多条件筛选的考试记录分页查询，包括按姓名、状态、时间范围等筛选")  // API描述
    public Result<Page<ExamRecord>> getExamRecords(
            @Parameter(description = "当前页码，从1开始", example = "1") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页显示数量", example = "20") @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "学生姓名筛选条件") @RequestParam(value = "studentName", required = false) String studentName,
            @Parameter(description = "学号筛选条件") @RequestParam(value = "studentNumber", required = false) String studentNumber,
            @Parameter(description = "考试状态，0-进行中，1-已完成，2-已批阅") @RequestParam(value = "status", required = false) Integer status,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam(value = "startDate", required = false) String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam(value = "endDate", required = false) String endDate
    ) {
        Page<ExamRecord> examRecordPage = new Page<>(page, size);
        examRecordService.pageExamRecords(examRecordPage, studentName, studentNumber, status, startDate, endDate);
        examRecordService.fixUpdateRecordStatus(examRecordPage);
        return Result.success(examRecordPage);
    }

    /**
     * 删除考试记录
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除考试记录", description = "根据ID删除指定的考试记录")  // API描述
    public Result<Void> deleteExamRecord(
            @Parameter(description = "考试记录ID") @PathVariable("id") Long id) {
        examRecordService.removeExamRecordById(id);
        log.info("删除id={}的考试记录成功", id);
        return Result.success("删除考试记录成功");
    }

    /**
     * 获取考试排行榜 - 优化版本
     * 使用SQL关联查询，一次性获取所有需要的数据，性能提升数百倍
     * 
     * @param paperId 试卷ID，可选参数
     * @param limit 显示数量限制，可选参数
     * @return 排行榜列表
     */
    @GetMapping("/ranking")  // 处理GET请求
    @Operation(summary = "获取考试排行榜", description = "获取考试成绩排行榜，支持按试卷筛选和限制显示数量，使用优化的SQL关联查询提升性能")  // API描述
    public Result<List<ExamRankingVO>> getExamRanking(
            @Parameter(description = "试卷ID，可选，不传则显示所有试卷的排行") @RequestParam(value = "paperId", required = false) Long paperId,
            @Parameter(description = "显示数量限制，可选，不传则返回所有记录") @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<ExamRankingVO> examRankingVOs = examRecordService.getExamRanking(paperId, limit);
        log.info("获取考试排行榜信息成功：examRankingVOs={}", examRankingVOs);
        return Result.success(examRankingVOs);
    }
} 