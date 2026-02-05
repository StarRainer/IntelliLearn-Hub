package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.entity.Category;
import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.entity.QuestionAnswer;
import edu.jlu.intellilearnhub.server.entity.QuestionChoice;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.CategoryMapper;
import edu.jlu.intellilearnhub.server.service.AIService;
import edu.jlu.intellilearnhub.server.service.QuestionBatchService;
import edu.jlu.intellilearnhub.server.service.QuestionService;
import edu.jlu.intellilearnhub.server.utils.ExcelUtil;
import edu.jlu.intellilearnhub.server.vo.AiGenerateRequestVo;
import edu.jlu.intellilearnhub.server.vo.QuestionImportVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionBatchServiceImpl implements QuestionBatchService {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private AIService aiService;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public byte[] getDefaultExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目导入模板");
            String[] headers = {
                    "题目内容", "题目类型", "是否多选", "分类ID", "难度", "分值",
                    "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("以下哪个是Spring框架的核心特性？");
            exampleRow.createCell(1).setCellValue("CHOICE");
            exampleRow.createCell(2).setCellValue("否");
            exampleRow.createCell(3).setCellValue("1");
            exampleRow.createCell(4).setCellValue("MEDIUM");
            exampleRow.createCell(5).setCellValue("5");
            exampleRow.createCell(6).setCellValue("依赖注入");
            exampleRow.createCell(7).setCellValue("面向切面编程");
            exampleRow.createCell(8).setCellValue("事务管理");
            exampleRow.createCell(9).setCellValue("以上都是");
            exampleRow.createCell(10).setCellValue("D");
            exampleRow.createCell(11).setCellValue("Spring框架的核心特性包括依赖注入、面向切面编程和事务管理等。");

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            return ExcelUtil.generateTemplate(workbook);
        }
    }

    @Override
    public List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new CommonException("预览失败，上传的表格文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new CommonException("预览失败，上传的表格文件应该有文件名");
        }
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new CommonException("预览失败，上传的表格文件的后缀只能为.xlsx或者.xls");
        }
        return ExcelUtil.parseExcel(file);
    }

    @Override
    public String importQuestions(List<QuestionImportVo> questionImportVos) {
        if (CollectionUtils.isEmpty(questionImportVos)) {
            return "因为没有选择任何要导入的题目，本次导入题目数量为0";
        }
        int successNumber = 0;
        for (QuestionImportVo questionImportVo : questionImportVos) {
            try {
                Question question = new Question();
                BeanUtils.copyProperties(questionImportVo, question);
                if ("CHOICE".equals(question.getType()) && !CollectionUtils.isEmpty(questionImportVo.getChoices())) {
                    List<QuestionChoice> questionChoices = questionImportVo.getChoices().stream()
                            .map(choiceImportDto -> {
                                QuestionChoice questionChoice = new QuestionChoice();
                                BeanUtils.copyProperties(choiceImportDto, questionChoice);
                                return questionChoice;
                            }).toList();
                    question.setChoices(questionChoices);
                }
                QuestionAnswer questionAnswer = new QuestionAnswer();
                BeanUtils.copyProperties(questionImportVo, questionAnswer);
                if ("JUDGE".equals(question.getType())) {
                    String answer = questionImportVo.getAnswer();
                    if (StringUtils.hasText(answer)) {
                        questionAnswer.setAnswer(answer.toUpperCase());
                    }
                }
                question.setAnswer(questionAnswer);
                questionService.saveQuestion(question);
                successNumber++;
            } catch (Exception e) {
                log.error("保存内容为[{}]的题目失败了，原因是：{}", questionImportVo.getTitle(), e.getMessage());
            }
        }
        return "题目批量导入结束，%s条数据一共成功导入了%s条".formatted(questionImportVos.size(), successNumber);
    }

    @Override
    public List<QuestionImportVo> generateQuestionByAi(AiGenerateRequestVo request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        return aiService.generateImportQuestion(
                request.getTopic(),
                request.getCount(),
                request.getTypes(),
                request.getDifficulty(),
                request.getCategoryId(),
                request.getIncludeMultiple(),
                request.getRequirements()
        )
        .stream()
        .map(questionImportVo -> {
          questionImportVo.setCategoryName(category.getName());
          return questionImportVo;
        }).toList();
    }
}
