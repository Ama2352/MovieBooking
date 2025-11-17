package com.api.moviebooking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test controller for Thymeleaf payment flow testing
 * Provides simple UI for end-to-end payment testing
 */
@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
@Hidden // Hide from Swagger docs
public class TestPaymentController {

    @GetMapping("/payment-status")
    public String paymentStatus() {
        return "payment-status";
    }
}
