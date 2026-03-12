package com.example.my_api.entity;

public class MemberFixture {

    public static Member.MemberBuilder defaultMember() {
        // 요구 사항으로 인해 엔티티 추가되면 여기 코드만 변경하면됨
        return Member.builder()
            .email("test1@gmail.com")
            .password("1234");

    }
}
