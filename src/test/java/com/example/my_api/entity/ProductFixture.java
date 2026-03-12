package com.example.my_api.entity;

public class ProductFixture {

    public static Product.ProductBuilder defaultProduct() {
        // 요구 사항으로 인해 엔티티 추가되면 여기 코드만 변경하면됨
        return Product.builder()
            .stock(10)
            .name("상품A")
            .price(10000L)
            .description("상품A 입니다");
    }
}
