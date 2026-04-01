package com.example.my_api.service;

import com.example.my_api.dto.MemberResponse;
import com.example.my_api.dto.MemberSignUpRequest;
import com.example.my_api.entity.Member;
import com.example.my_api.event.MemberSignUpEvent;
import com.example.my_api.exception.ParameterNotValidate;
import com.example.my_api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MemberResponse signUp(MemberSignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ParameterNotValidate("이미 가입된 이메일입니다.");
        }

        Member member = Member.builder()
            .email(request.email())
            .password(request.password())
            .build();

        Member saved = memberRepository.save(member);
        eventPublisher.publishEvent(new MemberSignUpEvent(saved.getId(), saved.getEmail()));
        return new MemberResponse(saved.getId(), saved.getEmail());
    }

//    private void sendEmail(String email) {
//        try {
//            Thread.sleep(1); // 네트워크 i/o 시간
//            log.info("메일 전송 완료! - {}", email);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
