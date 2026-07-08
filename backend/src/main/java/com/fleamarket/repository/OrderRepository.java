package com.fleamarket.repository;

import com.fleamarket.domain.Order;
import com.fleamarket.domain.enums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    Page<Order> findByBuyerIdAndOrderType(Long buyerId, OrderType orderType, Pageable pageable);

    Page<Order> findByBuyerIdAndOrderTypeAndStatus(Long buyerId, OrderType orderType, String status, Pageable pageable);

    Page<Order> findBySellerIdAndOrderType(Long sellerId, OrderType orderType, Pageable pageable);

    Page<Order> findBySellerIdAndOrderTypeAndStatus(Long sellerId, OrderType orderType, String status, Pageable pageable);

    Page<Order> findByOrderTypeAndStatus(OrderType orderType, String status, Pageable pageable);

    List<Order> findByStatusAndOrderType(String status, OrderType orderType);
}
