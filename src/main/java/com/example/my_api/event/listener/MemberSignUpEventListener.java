package com.example.my_api.event.listener;

import com.example.my_api.event.MemberSignUpEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class MemberSignUpEventListener {

    @Async(value = "cpuExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberSignUp(MemberSignUpEvent event) {
        try {
            Thread.sleep(1000); // 네트워크 i/o 시간
            log.info("메일 전송 완료! - {}", event.getEmail());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
