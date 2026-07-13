package com.booksshop.shopback.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);

    Page<Order> findByUserIdAndStatusAndCreateAtGreaterThanEqual(
            String userId, OrderStatus status, LocalDateTime createAtFrom, Pageable pageable);
}
