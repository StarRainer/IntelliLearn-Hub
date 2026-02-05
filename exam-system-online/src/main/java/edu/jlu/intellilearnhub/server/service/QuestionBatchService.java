package edu.jlu.intellilearnhub.server.service;

import edu.jlu.intellilearnhub.server.vo.QuestionImportVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface QuestionBatchService {
    byte[] getDefaultExcelTemplate() throws IOException;

    List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException;

    String importQuestions(List<QuestionImportVo> questionImportVos);
}
