package com.fleamarket.controller;

import com.fleamarket.dto.request.PublishProductRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ProductResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Page<ProductResponse>> listActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.listActiveProducts(pageable));
    }

    @GetMapping("/products/category/{categoryId}")
    public ApiResponse<Page<ProductResponse>> listByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.listByCategory(categoryId, sort, pageable));
    }

    @GetMapping("/products/search")
    public ApiResponse<Page<ProductResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.search(keyword, pageable));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PostMapping("/products")
    public ApiResponse<ProductResponse> publish(@Valid @RequestBody PublishProductRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ProductResponse product = productService.publish(userId, request);
        return ApiResponse.success("商品发布成功，当前状态为在售", product);
    }

    @PutMapping("/products/{id}/off")
    public ApiResponse<ProductResponse> offProduct(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ProductResponse product = productService.offProduct(userId, id);
        return ApiResponse.success("商品已下架", product);
    }

    @GetMapping("/my/products")
    public ApiResponse<Page<ProductResponse>> listMyProducts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.listBySeller(userId, status, pageable));
    }

    @GetMapping("/users/{id}/products")
    public ApiResponse<Page<ProductResponse>> listUserProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.listBySeller(id, "ACTIVE", pageable));
    }
}
