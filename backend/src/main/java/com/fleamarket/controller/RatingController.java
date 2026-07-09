package com.fleamarket.controller;

import com.fleamarket.dto.request.CreateRatingRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.RatingResponse;
import com.fleamarket.security.SecurityUtils;
import com.fleamarket.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/ratings")
    public ApiResponse<RatingResponse> rate(@RequestParam Long orderId,
                                              @Valid @RequestBody CreateRatingRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        RatingResponse rating = ratingService.rate(userId, orderId, request.getScore(), request.getComment());
        return ApiResponse.success("评价已提交", rating);
    }

    @GetMapping("/users/{id}/ratings")
    public ApiResponse<List<RatingResponse>> getUserRatings(@PathVariable Long id) {
        return ApiResponse.success(ratingService.getUserRatings(id));
    }
}
