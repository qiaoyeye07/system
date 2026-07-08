package com.fleamarket.service;

import com.fleamarket.domain.Category;
import com.fleamarket.domain.Product;
import com.fleamarket.domain.User;
import com.fleamarket.domain.enums.ProductCondition;
import com.fleamarket.domain.enums.ProductStatus;
import com.fleamarket.domain.enums.TradeMode;
import com.fleamarket.domain.enums.TradeType;
import com.fleamarket.dto.request.PublishProductRequest;
import com.fleamarket.dto.response.ProductResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.CategoryRepository;
import com.fleamarket.repository.ProductRepository;
import com.fleamarket.repository.RatingRepository;
import com.fleamarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    @Transactional
    public ProductResponse publish(Long sellerId, PublishProductRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(Category::getEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        ProductCondition condition = parseCondition(request.getCondition());
        TradeType tradeType = parseTradeType(request.getTradeType());
        TradeMode tradeMode = parseTradeMode(request.getTradeMode());

        String tags = null;
        if (request.getTags() != null && !request.getTags().trim().isEmpty()) {
            String[] tagArray = request.getTags().trim().split(",");
            if (tagArray.length > 5) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "标签最多 5 个");
            }
            for (String tag : tagArray) {
                if (tag.trim().length() > 20) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "每个标签不超过 20 个字符");
                }
            }
            tags = Arrays.stream(tagArray).map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.joining(","));
        }

        Product product = Product.builder()
                .seller(seller)
                .title(request.getTitle().trim())
                .price(request.getPrice())
                .description(request.getDescription().trim())
                .category(category)
                .tags(tags)
                .condition(condition)
                .tradeType(tradeType)
                .tradeMode(tradeMode)
                .location(request.getLocation().trim())
                .status(ProductStatus.ACTIVE)
                .build();

        return toResponse(productRepository.save(product));
    }

    public Page<ProductResponse> listActiveProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> listByCategory(Long categoryId, String sort, Pageable pageable) {
        Sort sortObj = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);

        return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageRequest)
                .map(this::toResponse);
    }

    public Page<ProductResponse> search(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, ProductStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    public ProductResponse getById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return toResponse(product);
    }

    public Page<ProductResponse> listBySeller(Long sellerId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
            return productRepository.findBySellerIdAndStatus(sellerId, productStatus, pageable)
                    .map(this::toResponse);
        }
        return productRepository.findBySellerId(sellerId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProductResponse offProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        product.setStatus(ProductStatus.OFF);
        return toResponse(productRepository.save(product));
    }

    public ProductResponse toResponse(Product product) {
        Double sellerRating = ratingRepository.getAverageScoreByRatedUser(product.getSeller().getId());

        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getUsername())
                .sellerRating(sellerRating != null ? Math.round(sellerRating * 10.0) / 10.0 : 0.0)
                .title(product.getTitle())
                .price(product.getPrice())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .tags(product.getTags())
                .condition(product.getCondition().name())
                .tradeType(product.getTradeType().name())
                .tradeMode(product.getTradeMode().name())
                .location(product.getLocation())
                .images(product.getImages())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductCondition parseCondition(String condition) {
        try {
            return ProductCondition.valueOf(condition.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的成色值，可选: NEW, LIKE_NEW, USED");
        }
    }

    private TradeType parseTradeType(String tradeType) {
        try {
            return TradeType.valueOf(tradeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的交易方式，可选: PICKUP, EXPRESS, BOTH");
        }
    }

    private TradeMode parseTradeMode(String tradeMode) {
        try {
            return TradeMode.valueOf(tradeMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的交易模式，可选: SELL, SWAP, BOTH");
        }
    }

    private Sort parseSort(String sort) {
        if ("price_asc".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
