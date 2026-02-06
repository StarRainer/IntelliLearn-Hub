package edu.jlu.intellilearnhub.server.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 题目批量导入Vo - 用于Excel导入和AI生成题目的数据传输
 */
@Data
@Schema(description = "题目导入数据传输对象")
public class QuestionImportVo {

    @Description("这个字段填写题目的内容")
    @Schema(description = "题目标题内容", 
            example = "以下关于Java面向对象编程的说法正确的是？", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title; // 题目标题

    @Description("这个字段填写题目的类型，选择题是CHOICE，判断题是JUDGE，简答题是TEXT")
    @Schema(description = "题目类型", 
            example = "CHOICE", 
            allowableValues = {"CHOICE", "JUDGE", "TEXT"})
    private String type; // 题目类型：CHOICE、JUDGE、TEXT

    @Description("这个字段填写当前题目是否为多选题，对于非选择题直接填false就行了")
    @Schema(description = "是否为多选题（仅选择题有效）", 
            example = "false")
    private Boolean multi; // 是否为多选题（仅选择题有效）

    @Description("这个字段填题目分类ID，指的就是本次生成的题目要求的分类ID")
    @Schema(description = "题目分类ID", 
            example = "1")
    private Long categoryId; // 分类ID

    @Description("这个字段不用填写，直接给个空字符串就行")
    @Schema(description = "分类名称（仅用于显示，不会保存到数据库）", 
            example = "Java基础")
    private String categoryName; // 分类名称（用于显示，不入库）

    @Description("这个字段填写题目的难度级别，只能填写EASY或者MEDIUM或者HARD")
    @Schema(description = "题目难度级别", 
            example = "MEDIUM", 
            allowableValues = {"EASY", "MEDIUM", "HARD"})
    private String difficulty; // 难度：EASY、MEDIUM、HARD

    @Description("这个字段填写题目默认分值；如果我之前没有给出额外要求，那么选择题和判断题都是一道5分；一道简答题只能是10-30分，并且分值必须是5的倍数，具体设置的多一点少一点取决于这道题评分的关键词个数")
    @Schema(description = "题目默认分值", 
            example = "5")
    private Integer score; // 默认分值

    @JsonProperty(required = true)
    @Description("这个字段填写题目的答案解析，语言表述尽量简洁凝练，但要确保能让学生理解，每道题的解析字数不宜超过200字")
    @Schema(description = "题目解析说明", 
            example = "Java是面向对象编程语言，支持封装、继承、多态三大特性...")
    private String analysis; // 题目解析

    @Description("这个字段填写选择题的选项列表，如果本道题目不是选择题，给一个空数组就行")
    @Schema(description = "选择题选项列表（仅选择题需要）")
    private List<ChoiceImportDto> choices; // 选择题选项

    @Description("这个字段填写题目答案，注意判断题只能填TRUE或者FALSE；如果这道题是选择题，那么你不需要填写这个字段")
    @Schema(description = "题目答案（判断题和简答题使用）",
            example = "正确")
    private String answer; // 答案（判断题和简答题）

    @Description("这个字段填写批阅得分点的关键词，主要用于简答题AI评分进行判断")
    @Schema(description = "答题关键词（用于简答题AI评分）", 
            example = "面向对象,封装,继承,多态")
    private String keywords; // 关键词（用于简答题评分）
    
    /**
     * 选择题选项导入DTO
     */
    @Data
    @Schema(description = "选择题选项数据")
    public static class ChoiceImportDto {

        @Description("这个字段填写选项的内容")
        @Schema(description = "选项内容", 
                example = "Java支持多重继承",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String content; // 选项内容


        @Description("这个字段填写是否为正确答案，正确为true，错误为false")
        @Schema(description = "是否为正确答案", 
                example = "false")
        private Boolean isCorrect; // 是否正确答案

        @Description("这个字段填写选项排序序号，选项A为0，选项B为1，以此类推")
        @Schema(description = "选项排序序号", 
                example = "1")
        private Integer sort; // 排序
    }
} 