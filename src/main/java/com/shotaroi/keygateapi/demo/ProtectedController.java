package com.shotaroi.keygateapi.demo;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedController {

    @GetMapping("/hello")
    public String hello(Authentication auth) {
        return "Hello! You are: " + auth.getName();
    }
}
