package edu.jlu.intellilearnhub.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listCategories();

    List<Category> getCategoryTree();

    void saveCategory(Category category);

    void updateCategory(Category category);

    void removeCategory(Long id);
}