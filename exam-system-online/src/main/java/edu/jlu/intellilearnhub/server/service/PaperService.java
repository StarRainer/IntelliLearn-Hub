package edu.jlu.intellilearnhub.server.service;

import edu.jlu.intellilearnhub.server.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.vo.AiPaperVo;
import edu.jlu.intellilearnhub.server.vo.PaperVo;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    List<Paper> listPapers(String name, String status);

    Paper createPaper(PaperVo paperVo);

    Paper getPaperWithOutAnswerById(Long id);

    void deletePaperById(Long id);

    void updatePaperStatus(Long id, String status);

    Paper updatePaper(Long id, PaperVo paperVo);

    Paper createPaperByAI(AiPaperVo aiPaperVo);

    Paper getPaperById(Long examId);
}