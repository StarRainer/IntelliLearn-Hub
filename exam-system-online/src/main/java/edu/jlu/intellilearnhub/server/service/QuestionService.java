package edu.jlu.intellilearnhub.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jlu.intellilearnhub.server.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.vo.QuestionImportVo;
import edu.jlu.intellilearnhub.server.vo.QuestionQueryVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 题目业务服务接口 - 定义题目相关的业务逻辑
 * 
 * Spring Boot三层架构教学要点：
 * 1. Service层：业务逻辑层，位于Controller和Mapper之间
 * 2. 接口设计：定义业务方法规范，便于不同实现类的切换
 * 3. 继承IService：使用MyBatis Plus提供的通用服务接口，减少重复代码
 * 4. 事务管理：Service层是事务的边界，复杂业务操作应该加@Transactional
 * 5. 业务封装：将复杂的数据操作封装成有业务意义的方法
 *
 * 设计原则：
 * - 单一职责：专门处理题目相关的业务逻辑
 * - 开闭原则：通过接口定义，便于扩展新的实现
 * - 依赖倒置：Controller依赖接口而不是具体实现
 * @version 1.0
 */
public interface QuestionService extends IService<Question> {


    void listQuestion(Page<Question> questionPage, QuestionQueryVo questionQueryVo);

    Question getQuestionById(Long id);

    void saveQuestion(Question question);

    void updateQuestion(Question question);

    void removeQuestion(Long id);

    List<Question> listPopularQuestions(Integer size);

    byte[] getDefaultExcelTemplate() throws IOException;

    List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException;
}