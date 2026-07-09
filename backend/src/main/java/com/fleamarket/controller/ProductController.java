package com.fleamarket.controller;

import com.fleamarket.dto.request.PublishProductRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.ProductResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Page<ProductResponse>> listActive(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.listActiveProducts(categoryId, sort, pageable));
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
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.search(keyword, categoryId, sort, pageable));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PostMapping("/products")
    public ApiResponse<ProductResponse> publish(
            @RequestParam String title,
            @RequestParam BigDecimal price,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String tags,
            @RequestParam String condition,
            @RequestParam String tradeType,
            @RequestParam String tradeMode,
            @RequestParam String location,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        Long userId = SecurityUtils.getCurrentUserId();
        PublishProductRequest request = new PublishProductRequest();
        request.setTitle(title);
        request.setPrice(price);
        request.setDescription(description);
        request.setCategoryId(categoryId);
        request.setTags(tags);
        request.setCondition(condition);
        request.setTradeType(tradeType);
        request.setTradeMode(tradeMode);
        request.setLocation(location);

        // 保存上传的图片
        String imagePaths = null;
        if (images != null && !images.isEmpty()) {
            imagePaths = productService.saveImages(images);
        }
        request.setImages(imagePaths);

        ProductResponse product = productService.publish(userId, request);
        return ApiResponse.success("商品发布成功，当前状态为在售", product);
    }

    @PatchMapping("/products/{id}/off")
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
