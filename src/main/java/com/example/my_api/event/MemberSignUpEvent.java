package com.example.my_api.event;

public class MemberSignUpEvent {

    private final String email;
    private final Long memberId;

    public MemberSignUpEvent(Long memberId, String email) {
        this.memberId = memberId;
        this.email = email;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }
}
