package com.example.my_api.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Counter2 {

    private int count = 0; //Heap 메모리에 저장 -> 공유됨(임계영역)

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        Counter2 counter2 = new Counter2();
        int threadCount = 9187;

        ExecutorService es = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            es.submit(counter2::increaseCount);
        }

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS); //최대 10초 기다림

        log.info("counter = {}", counter2.count);
    }

    public void increaseCount() {
        count++;
    }
}
