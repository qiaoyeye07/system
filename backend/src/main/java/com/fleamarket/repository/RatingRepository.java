package com.fleamarket.repository;

import com.fleamarket.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByRatedUserId(Long ratedUserId);

    Optional<Rating> findByOrderIdAndRaterId(Long orderId, Long raterId);

    boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.ratedUser.id = :userId")
    Double getAverageScoreByRatedUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.ratedUser.id = :userId")
    Long countByRatedUser(@Param("userId") Long userId);

    List<Rating> findByOrderId(Long orderId);
}
