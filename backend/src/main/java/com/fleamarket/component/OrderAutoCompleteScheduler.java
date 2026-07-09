package com.fleamarket.component;

import com.fleamarket.domain.Order;
import com.fleamarket.domain.OrderLog;
import com.fleamarket.domain.enums.ActionType;
import com.fleamarket.domain.enums.OrderType;
import com.fleamarket.repository.OrderLogRepository;
import com.fleamarket.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteScheduler {

    private final OrderRepository orderRepository;
    private final OrderLogRepository orderLogRepository;

    /** 每整点执行，自动完成收货超3天的订单 */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoCompleteReceivedOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);

        List<Order> receivedOrders = orderRepository
                .findByStatusAndOrderType("RECEIVED", OrderType.CASH);

        for (Order order : receivedOrders) {
            if (order.getUpdatedAt() != null && order.getUpdatedAt().isBefore(threshold)) {
                order.setStatus("COMPLETED");
                orderRepository.save(order);

                OrderLog log = OrderLog.builder()
                        .order(order)
                        .operator(null)
                        .actionType(ActionType.AUTO_COMPLETE)
                        .oldStatus("RECEIVED")
                        .newStatus("COMPLETED")
                        .detail("系统自动完成（收货超3天）")
                        .build();
                orderLogRepository.save(log);

                log.info("Auto-completed order: {}", order.getOrderNo());
            }
        }
    }
}
