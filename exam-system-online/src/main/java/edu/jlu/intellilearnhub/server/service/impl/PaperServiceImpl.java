package edu.jlu.intellilearnhub.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jlu.intellilearnhub.server.entity.Paper;
import edu.jlu.intellilearnhub.server.mapper.PaperMapper;
import edu.jlu.intellilearnhub.server.service.PaperService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {


    @Override
    public List<Paper> listPapers(String name, String status) {
        return list(new LambdaQueryWrapper<Paper>()
                .like(!ObjectUtils.isEmpty(name), Paper::getName, name)
                .eq(!ObjectUtils.isEmpty(status), Paper::getStatus, status)
        );
    }
}