package com.madeeasy.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class GatewayController {

    @RequestMapping(path = "/user-service")
    public Mono<String> userServiceFallBack() {
        return Mono.just("The User Service is currently unavailable. Please try again later. For urgent issues, contact support at support@madeeasy.com.");
    }

    @RequestMapping(path = "/course-service")
    public Mono<String> courseServiceFallBack() {
        return Mono.just("The Course Service is currently unavailable. Please try again later. If the problem persists, please contact our support team at support@madeeasy.com.");
    }

    @RequestMapping(path = "/auth-service")
    public Mono<String> authServiceFallBack() {
        return Mono.just("The Authentication Service is currently unavailable. Please try again later. For further assistance, reach out to support@madeeasy.com.");
    }

    @RequestMapping(path = "/instance-service")
    public Mono<String> instanceServiceFallBack() {
        return Mono.just("The Instance Service is currently unavailable. Please try again later. If you need immediate help, please contact support at support@madeeasy.com.");
    }
}