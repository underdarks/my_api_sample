package com.example.my_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.example.my_api.dto.MemberSignUpRequest;
import com.example.my_api.entity.Member;
import com.example.my_api.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(Lifecycle.PER_CLASS)
class MemberServiceThreadPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceThreadPerformanceTest.class);
    private static final int TASK_COUNT = 1_000;
    private static final int IO_DELAY_MS = 20;
    private static final int PLATFORM_THREAD_COUNT = 200;
    private static final int TEST_REPEATS = 5;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberService memberService;

    private final AtomicLong memberIdGenerator = new AtomicLong(1);
    private final List<Long> platformDurations = new ArrayList<>();
    private final List<Long> virtualDurations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reset(memberRepository, eventPublisher);
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            sleepIoDelay();
            return Member.builder()
                .id(memberIdGenerator.getAndIncrement())
                .email(member.getEmail())
                .password(member.getPassword())
                .build();
        });
    }

    @RepeatedTest(value = TEST_REPEATS, name = "[{currentRepetition}/{totalRepetitions}] signUp thread performance")
    @DisplayName("signUp 반복 성능 테스트 - 가상 스레드 vs 플랫폼 스레드")
    void compareVirtualAndPlatformThreads(RepetitionInfo repetitionInfo) throws InterruptedException {
        long platformMs = runSignUpLoad(Executors.newFixedThreadPool(PLATFORM_THREAD_COUNT));
        long virtualMs = runSignUpLoad(Executors.newVirtualThreadPerTaskExecutor());

        platformDurations.add(platformMs);
        virtualDurations.add(virtualMs);

        double ratio = (double) virtualMs / platformMs;
        log.info(
            "반복 {}회차 결과 -> platform={}ms, virtual={}ms, virtual/platform={}",
            repetitionInfo.getCurrentRepetition(),
            platformMs,
            virtualMs,
            String.format("%.2f", ratio)
        );

        assertThat(platformMs).isGreaterThan(0);
        assertThat(virtualMs).isGreaterThan(0);
    }

    @AfterAll
    void printSummary() {
        long platformAvg = average(platformDurations);
        long virtualAvg = average(virtualDurations);
        double avgRatio = (double) virtualAvg / platformAvg;

        log.info("===== signUp 스레드 성능 요약 =====");
        log.info("플랫폼 스레드(ms): {}", platformDurations);
        log.info("가상 스레드(ms): {}", virtualDurations);
        log.info("평균 -> platform={}ms, virtual={}ms, virtual/platform={}",
            platformAvg,
            virtualAvg,
            String.format("%.2f", avgRatio));
    }

    private long runSignUpLoad(ExecutorService executorService) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(TASK_COUNT);
        AtomicInteger successCount = new AtomicInteger();

        IntStream.range(0, TASK_COUNT).forEach(i -> executorService.submit(() -> {
            try {
                startLatch.await();
                memberService.signUp(new MemberSignUpRequest("user" + i + "@example.com", "1234"));
                successCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        }));

        long startedAt = System.nanoTime();
        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        executorService.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(TASK_COUNT);
        return durationMs;
    }

    private long average(List<Long> values) {
        return Math.round(values.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private void sleepIoDelay() {
        try {
            Thread.sleep(IO_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
