package com.capa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@AuthenticationPrincipal String email) {
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of("authenticated", true, "email", email));
    }
}