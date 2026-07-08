package com.fleamarket.controller;

import com.fleamarket.dto.request.CreateCategoryRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.CategoryResponse;
import com.fleamarket.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listEnabled() {
        return ApiResponse.success(categoryService.getAllEnabled());
    }

    @GetMapping("/admin/categories")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return ApiResponse.success(categoryService.getAll());
    }

    @GetMapping("/admin/categories/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }

    @PostMapping("/admin/categories")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("分类创建成功", categoryService.create(request));
    }

    @PutMapping("/admin/categories/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("分类更新成功", categoryService.update(id, request));
    }

    @PutMapping("/admin/categories/{id}/toggle")
    public ApiResponse<CategoryResponse> toggleEnabled(@PathVariable Long id) {
        CategoryResponse category = categoryService.toggleEnabled(id);
        String msg = category.getEnabled() ? "分类已启用" : "分类已停用";
        return ApiResponse.success(msg, category);
    }
}
