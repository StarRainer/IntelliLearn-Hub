package edu.jlu.intellilearnhub.server.service;


import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import edu.jlu.intellilearnhub.server.vo.AiGenerateRequestVo;
import edu.jlu.intellilearnhub.server.vo.GradeExamVo;
import edu.jlu.intellilearnhub.server.vo.QuestionImportVo;

import java.util.List;

/**
 * AI服务接口
 */
@AiService
public interface AIService {
    String chat(String userMessage);

    @UserMessage(fromResource = "prompts/generate-import-question.txt")
    List<QuestionImportVo> generateImportQuestion(@V("topic") String topic,
                                                  @V("count") Integer count,
                                                  @V("types") String types,
                                                  @V("difficulty") String difficulty,
                                                  @V("categoryId") Long categoryId,
                                                  @V("includeMultiple") Boolean includeMultiple,
                                                  @V("requirements") String requirements);

    @UserMessage(fromResource = "prompts/grade-exam.txt")
    GradeExamVo gradeExam(@V("title") String title,
                          @V("answer") String answer,
                          @V("keywords") String keywords,
                          @V("score") Integer score,
                          @V("studentAnswer") String studentAnswer);
} 