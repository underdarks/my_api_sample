package com.example.my_api.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequest(
        @NotBlank(message = "상태값은 필수입니다.")
        String status
) {
}
