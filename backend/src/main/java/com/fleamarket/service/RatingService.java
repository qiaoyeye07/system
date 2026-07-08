package com.fleamarket.service;

import com.fleamarket.domain.Order;
import com.fleamarket.domain.Rating;
import com.fleamarket.domain.User;
import com.fleamarket.dto.response.RatingResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.OrderRepository;
import com.fleamarket.repository.RatingRepository;
import com.fleamarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponse rate(Long raterId, Long orderId, int score) {
        if (score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评分范围为 1-5 星");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        if (!order.getBuyer().getId().equals(raterId) && !order.getSeller().getId().equals(raterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (ratingRepository.existsByOrderIdAndRaterId(orderId, raterId)) {
            throw new BusinessException(ErrorCode.ALREADY_RATED);
        }

        Long ratedUserId = order.getBuyer().getId().equals(raterId)
                ? order.getSeller().getId() : order.getBuyer().getId();
        User ratedUser = userRepository.findById(ratedUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Rating rating = Rating.builder()
                .order(order)
                .rater(rater)
                .ratedUser(ratedUser)
                .score(score)
                .build();

        rating = ratingRepository.save(rating);
        return toResponse(rating);
    }

    public List<RatingResponse> getUserRatings(Long userId) {
        return ratingRepository.findByRatedUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .orderId(rating.getOrder().getId())
                .orderNo(rating.getOrder().getOrderNo())
                .raterId(rating.getRater().getId())
                .raterName(rating.getRater().getUsername())
                .ratedUserId(rating.getRatedUser().getId())
                .ratedUserName(rating.getRatedUser().getUsername())
                .score(rating.getScore())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
