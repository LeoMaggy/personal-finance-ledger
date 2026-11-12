package com.mahesh.expenses.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.mahesh.expenses.auth.AuthDtos.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements       // Day 5: no token in Swagger for this call (an old token would cause a 401)
    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(@Valid @RequestBody SignupRequest signupRequest){
        return authService.signUp(signupRequest);
    }

    @SecurityRequirements
    @PostMapping("/auth/login")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse login(@Valid @RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }

    @GetMapping("/me")
    public UserResponse getUser(){
        return authService.me();
    }
}
