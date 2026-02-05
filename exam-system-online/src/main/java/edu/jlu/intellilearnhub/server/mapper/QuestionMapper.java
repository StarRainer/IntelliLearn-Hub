package edu.jlu.intellilearnhub.server.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jlu.intellilearnhub.server.dto.QuestionDto;
import edu.jlu.intellilearnhub.server.entity.Question;

import java.util.List;

/**
 * 题目Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
public interface QuestionMapper extends BaseMapper<Question> {

    List<QuestionDto> countByCategoryId();
}