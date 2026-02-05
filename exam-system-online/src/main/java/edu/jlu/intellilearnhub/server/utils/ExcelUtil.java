package edu.jlu.intellilearnhub.server.utils;

import edu.jlu.intellilearnhub.server.vo.QuestionImportVo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel文件处理工具类
 * 用于解析题目导入的Excel文件
 */
public final class ExcelUtil {
    private ExcelUtil() {

    }
    
    /**
     * 解析Excel文件并转换为题目导入DTO列表
     * 
     * Excel格式要求：
     * 第一行为标题行：题目内容|题目类型|是否多选|分类ID|难度|分值|选项A|选项B|选项C|选项D|正确答案|解析
     * 
     * @param file Excel文件
     * @return 题目导入DTO列表
     * @throws IOException 文件读取异常
     */
    public static List<QuestionImportVo> parseExcel(MultipartFile file) throws IOException {
        // 获取文件输入流
        Workbook workbook = null;
        try (InputStream inputStream = file.getInputStream()) {
            // 根据文件扩展名选择对应的工作簿类型
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream); // Excel 2007+
            } else {
                workbook = new HSSFWorkbook(inputStream); // Excel 97-2003
            }
            List<QuestionImportVo> questions = new ArrayList<>();
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            // 从第二行开始读取数据（第一行是标题）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                QuestionImportVo question = new QuestionImportVo();
                
                // 读取基本信息
                question.setTitle(getCellValue(row.getCell(0))); // 题目内容
                question.setType(getCellValue(row.getCell(1))); // 题目类型
                question.setMulti("是".equals(getCellValue(row.getCell(2))) || "true".equalsIgnoreCase(getCellValue(row.getCell(2)))); // 是否多选
                
                // 分类ID
                String categoryIdStr = getCellValue(row.getCell(3));
                if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                    try {
                        question.setCategoryId(Long.parseLong(categoryIdStr));
                    } catch (NumberFormatException e) {
                        question.setCategoryId(1L); // 默认分类
                    }
                }
                
                question.setDifficulty(getCellValue(row.getCell(4))); // 难度
                
                // 分值
                String scoreStr = getCellValue(row.getCell(5));
                if (scoreStr != null && !scoreStr.isEmpty()) {
                    try {
                        question.setScore(Integer.parseInt(scoreStr));
                    } catch (NumberFormatException e) {
                        question.setScore(5); // 默认分值
                    }
                }
                
                // 处理选择题选项
                if ("CHOICE".equals(question.getType())) {
                    List<QuestionImportVo.ChoiceImportDto> choices = new ArrayList<>();
                    String correctAnswer = getCellValue(row.getCell(10)); // 正确答案列
                    
                    // 读取选项A-D
                    for (int j = 0; j < 4; j++) {
                        String optionContent = getCellValue(row.getCell(6 + j));
                        if (optionContent != null && !optionContent.trim().isEmpty()) {
                            QuestionImportVo.ChoiceImportDto choice = new QuestionImportVo.ChoiceImportDto();
                            choice.setContent(optionContent);
                            choice.setSort(j + 1);
                            
                            // 判断是否为正确答案
                            char optionLabel = (char) ('A' + j);
                            boolean isCorrect = correctAnswer != null && correctAnswer.contains(String.valueOf(optionLabel));
                            choice.setIsCorrect(isCorrect);
                            
                            choices.add(choice);
                        }
                    }
                    question.setChoices(choices);
                } else {
                    // 判断题和简答题直接设置答案
                    question.setAnswer(getCellValue(row.getCell(10)));
                }
                
                question.setAnalysis(getCellValue(row.getCell(11))); // 解析
                
                // 验证必填字段
                if (question.getTitle() != null && !question.getTitle().trim().isEmpty() &&
                    question.getType() != null && !question.getType().trim().isEmpty()) {
                    questions.add(question);
                }
            }
            return questions;
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }
    
    /**
     * 获取单元格的字符串值
     * @param cell 单元格
     * @return 字符串值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 如果是数字，转换为字符串
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 生成Excel模板文件的字节数组
     * @return Excel模板文件的字节数组
     */
    public static byte[] generateTemplate(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }
} 