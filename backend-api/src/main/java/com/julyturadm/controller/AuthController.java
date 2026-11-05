package com.julyturadm.controller;

import com.julyturadm.dto.request.LoginRequest;
import com.julyturadm.dto.response.LoginResponse;
import com.julyturadm.service.AuthenticationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response =
                authenticationService.authenticate(request);

        return ResponseEntity.ok(response);
    }
}
