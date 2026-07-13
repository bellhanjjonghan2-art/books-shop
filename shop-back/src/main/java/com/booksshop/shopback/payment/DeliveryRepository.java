package com.booksshop.shopback.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    Optional<Delivery> findByOrder_Id(String orderId);

    // 주문/배송 목록 조회용: 여러 주문의 배송을 order_id IN (...)으로 한 번에 조회(N+1 방지)
    List<Delivery> findByOrder_IdIn(List<String> orderIds);
}
