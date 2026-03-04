package com.example.my_api.dto;

import com.example.my_api.entity.OrderStatus;
import java.util.List;

public record OrderResponse(
    Long id,
    Long memberId,
    String memberEmail,
    Long totalPrice,
    OrderStatus status,
    List<OrderItemResponse> orderItems
) {

}
