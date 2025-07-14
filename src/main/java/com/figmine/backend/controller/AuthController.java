package com.figmine.backend.controller;

import com.figmine.backend.dto.UserLoginRequest;
import com.figmine.backend.dto.UserSignupRequest;
import com.figmine.backend.model.User;
import com.figmine.backend.service.AuthService;
import com.figmine.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody UserSignupRequest request) {
        User user = authService.signup(request);
        String token = jwtService.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(user -> {
                    String token = jwtService.generateToken(user.getEmail());

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("email", user.getEmail());

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid credentials"))
                );
    }
}
