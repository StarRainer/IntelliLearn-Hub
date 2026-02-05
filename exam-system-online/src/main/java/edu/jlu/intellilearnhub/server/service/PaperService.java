package edu.jlu.intellilearnhub.server.service;

import edu.jlu.intellilearnhub.server.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    List<Paper> listPapers(String name, String status);
}