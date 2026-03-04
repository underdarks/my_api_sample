package com.example.my_api.dto;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    Long price
) {

}
