package edu.jlu.intellilearnhub.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.dto.QuestionDto;
import edu.jlu.intellilearnhub.server.entity.Category;
import edu.jlu.intellilearnhub.server.mapper.CategoryMapper;
import edu.jlu.intellilearnhub.server.mapper.QuestionMapper;
import edu.jlu.intellilearnhub.server.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
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
        for (Category category : categories) {
            if (category.getParentId() != 0) {
                continue;
            }
            category.setChildren(getChildren(category.getId(), parentIdToChildrenChildren));
        }
        return categories;
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