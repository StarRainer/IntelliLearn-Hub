package edu.jlu.intellilearnhub.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.dto.QuestionDto;
import edu.jlu.intellilearnhub.server.entity.Category;
import edu.jlu.intellilearnhub.server.entity.Question;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.CategoryMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<Category> listCategories() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
        );
        Map<Long, Long> idToCount = questionMapper.countByCategoryId().stream()
                .collect(Collectors.toMap(QuestionDto::getCategoryId, QuestionDto::getCount));
        categories.forEach(category ->
           category.setCount(idToCount.get(category.getId()))
        );
        return categories;
    }

    @Override
    public List<Category> getCategoryTree() {
        List<Category> categories = listCategories();
        Map<Long, List<Category>> parentIdToChildrenChildren = categories.stream().collect(Collectors.groupingBy(Category::getParentId));
        return categories.stream()
                .filter(category -> category.getParentId() == 0)
                .map(category -> {
                    category.setChildren(getChildren(category.getId(), parentIdToChildrenChildren));
                    return category;
                })
                .toList();
    }

    @Override
    public void saveCategory(Category category) {
        Category parentCategory = getOne(new LambdaQueryWrapper<Category>()
                .select(Category::getParentId)
                .eq(Category::getId, category.getParentId())
        );
        if (parentCategory == null || parentCategory.getParentId() != 0) {
            throw new CommonException("添加分类失败，只允许添加二级分类");
        }
        long count = count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, category.getParentId())
                .eq(Category::getName, category.getName())
        );
        if (count > 0) {
            throw new CommonException("添加分类失败：%s父分类下，已存在名为：%s的子分类信息".formatted(category.getName(), category.getParentId()));
        }
        save(category);
    }

    @Override
    public void updateCategory(Category category) {
        long count = count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, category.getParentId())
                .eq(Category::getName, category.getName())
                .ne(Category::getId, category.getId())
        );
        if (count > 0) {
            throw new CommonException("更新分类失败：%s父分类下，已存在名为：%s的子分类信息".formatted(category.getName(), category.getParentId()));
        }
        updateById(category);
    }

    @Override
    public void removeCategory(Long id) {
        Category category = getById(id);

        if (category == null) {
            log.debug("该分类在删除前已经被删除：id={}", id);
            return;
        }

        if (category.getParentId() == 0) {
            throw new CommonException("分类删除失败，id=%s的分类是一级分类，不允许删除".formatted(id));
        }

        Long count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, category.getId())
        );
        if (count > 0) {
            throw new CommonException("id为%s的分类删除失败，请先删除与分类关联的%s道题目".formatted(id, count));
        }


    }

    private List<Category> getChildren(Long parentId, Map<Long,List<Category>> parentIdToChildrenChildren) {
        List<Category> children = parentIdToChildrenChildren.get(parentId);
        if (CollectionUtils.isEmpty(children)) {
            return new ArrayList<>();
        }
        for (Category child : children) {
            child.setChildren(getChildren(child.getId(), parentIdToChildrenChildren));
        }
        return children;
    }
}