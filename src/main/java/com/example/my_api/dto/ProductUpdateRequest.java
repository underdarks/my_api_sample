package com.example.my_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Size(min = 1, message = "상품명은 비어 있을 수 없습니다.")
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다.")
        BigDecimal price,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock
) {
}
