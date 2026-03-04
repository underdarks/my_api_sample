package com.example.my_api.event;

import org.springframework.context.ApplicationEvent;

public class MemberSignUpEvent extends ApplicationEvent {

    private final String email;
    private final Long memberId;

    public MemberSignUpEvent(Object source, Long memberId, String email) {
        super(source);
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
