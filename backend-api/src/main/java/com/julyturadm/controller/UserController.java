package com.julyturadm.controller;

import java.util.List;

import com.julyturadm.dto.request.UserCreateRequest;
import com.julyturadm.dto.response.UserResponse;
import com.julyturadm.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.findUserById(id)
        );
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(
            userService.findUserByEmail(email)
        );
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAllActiveUsers() {

        return ResponseEntity.ok(
            userService.findAllActiveUsers()
        );
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.disableUser(id)
        );
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.enableUser(id)
        );
    }
}
