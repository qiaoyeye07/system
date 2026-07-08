package com.fleamarket.component;

import com.fleamarket.domain.Category;
import com.fleamarket.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        initCategories();
    }

    private void initCategories() {
        if (categoryRepository.count() == 0) {
            String[] defaultCategories = {"数码", "服饰", "家居", "图书", "其他"};
            for (String name : defaultCategories) {
                Category category = Category.builder()
                        .name(name)
                        .enabled(true)
                        .build();
                categoryRepository.save(category);
            }
            log.info("默认分类初始化完成: {} 个分类", defaultCategories.length);
        } else {
            log.info("分类数据已存在，跳过初始化。当前分类数: {}", categoryRepository.count());
        }
    }
}
