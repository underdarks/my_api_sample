package com.example.my_api.lock;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Counter {

    private int count = 0; //Heap 메모리에 저장 -> 공유됨(임계영역)

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        int threadCount = 100000;
        Counter counter = new Counter();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(counter::increaseCount);
            thread.start(); //스레드 시작(태스크 완료 후 커널에 리소스 반납)
            threads.add(thread);
        }

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("  기대값: " + threadCount);
        System.out.println("  실제값: " + counter.getCount());
        System.out.println("  → 실제값이 기대값보다 작으면 Race Condition 발생!");
        log.info("counter = {}", counter.count);
    }

    public void increaseCount() {
        synchronized (this) {
            count++;
        }
    }
}
