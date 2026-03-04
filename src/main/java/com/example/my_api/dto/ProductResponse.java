package com.example.my_api.dto;

public record ProductResponse(
    Long id,
    String name,
    String description,
    Long price,
    Integer stock
) {

}
