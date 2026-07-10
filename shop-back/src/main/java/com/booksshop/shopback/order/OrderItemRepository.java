package com.booksshop.shopback.order;

import com.booksshop.shopback.order.dto.OrderItemResultDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    List<OrderItem> findByOrder_Id(String orderId);

    // 주문 결과 조회(성공 화면)용: order_items 스냅샷 + books 조인(표지/저자/출판사)
    @Query("SELECT new com.booksshop.shopback.order.dto.OrderItemResultDto(" +
           "b.id, oi.title, b.author, b.publisher, b.coverImage, oi.quantity, oi.amount) " +
           "FROM OrderItem oi JOIN oi.book b " +
           "WHERE oi.order.id = :orderId")
    List<OrderItemResultDto> findResultItemsByOrderId(@Param("orderId") String orderId);
}
