package com.example.my_api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/health")
public class HealthCheck {

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GOOD GOOD1");
    }

}
