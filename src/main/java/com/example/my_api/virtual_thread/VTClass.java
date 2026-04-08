package com.example.my_api.virtual_thread;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class VTClass {

    static final int TASK_COUNT = 100;
    static final Duration IO_DURATION = Duration.ofSeconds(1); // 100ms I/O

    public static void main(String[] args) throws InterruptedException {
        int TASK_COUNT = 20; // 1,000개의 I/O 작업 가정

//        System.out.println("--- 플랫폼 스레드 (고정 풀 200개) 시작 ---");
//        runTest(Executors.newFixedThreadPool(200), TASK_COUNT);
//
//        System.out.println("\n--- 요청 마다 가상 스레드 시작 ---");
//        runTest(Executors.newVirtualThreadPerTaskExecutor(), TASK_COUNT);

//        System.out.println("--- Pinning 문제- 플랫폼 스레드 (고정 풀 200개) 시작 ---");
//        runPinningTest2(Executors.newFixedThreadPool(200));

        System.out.println("\n---  Pinning 문제 - 요청 마다 가상 스레드 시작 ---");
        runPinningTest(Executors.newVirtualThreadPerTaskExecutor());

//        System.out.println("--- Pinning 테스트 - 플랫폼 스레드 (고정 풀 200개) 시작 ---");
//        runPinningTest2(Executors.newFixedThreadPool(200));

        System.out.println("\n---  Pinning 해결 - 요청 마다 가상 스레드 시작 ---");
        runPinningTest2(Executors.newVirtualThreadPerTaskExecutor());
    }

    private static void runTest(ExecutorService executor) {
        Instant start = Instant.now();

        try (executor) {
            IntStream.range(0, TASK_COUNT).forEach(i -> {
                executor.submit(() -> {
                    try {
                        // 1초가 걸리는 외부 API 호출(I/O Blocking)을 시뮬레이션
                        Thread.sleep(IO_DURATION);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            });
        } // executor.close()가 호출되면서 모든 작업이 끝날 때까지 대기함

        Instant end = Instant.now();
        System.out.printf("작업 완료 시간: %d ms%n", Duration.between(start, end).toMillis());
    }

    private static void runPinningTest(ExecutorService executor) {
        Instant start = Instant.now();

        try (executor) {
            IntStream.range(0, TASK_COUNT).forEach(i -> {
                executor.submit(() -> {
                    // 자바의 모든 객체는 내부에 고유한 모니터 락(Monitor Lock)을 가지고 있는데, 이를 통해 한 번에 하나의 스레드만 해당 블록에 들어오도록 제어하는 것
                    synchronized (executor) { //해당 코드를 실행하려면 객체의 잠금(Lock)을 획득해야 실행 가능
                        try {
                            // 1초가 걸리는 외부 API 호출(I/O Blocking)을 시뮬레이션
                            Thread.sleep(IO_DURATION);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            });
        } // executor.close()가 호출되면서 모든 작업이 끝날 때까지 대기함

        Instant end = Instant.now();
        System.out.printf("작업 완료 시간: %d ms%n", Duration.between(start, end).toMillis());
    }

    public static void runPinningTest2(ExecutorService executor) {
        Instant start = Instant.now();
        try (executor) {
            IntStream.range(0, TASK_COUNT).forEach(i ->
                executor.submit(() -> {
                    ReentrantLock lock = new ReentrantLock(); // 각자 독립 락
                    lock.lock();
                    try {
                        Thread.sleep(IO_DURATION);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lock.unlock();
                    }

                })
            );
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("  실제 소요: %,d ms%n", elapsed);
    }


}
