package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.LoginRequest;
import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.dto.response.AuthResponse;
import com.universityofengineers.sms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
