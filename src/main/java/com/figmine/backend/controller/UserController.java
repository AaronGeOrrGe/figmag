package com.figmine.backend.controller;

import com.figmine.backend.dto.UserProfileResponse;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import com.figmine.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam(required = false) String avatarUrl) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.updateProfile(user, name, avatarUrl);
        return ResponseEntity.ok().build();
    }
}
