package com.figmine.backend.controller;

import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import com.figmine.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/complete")
    public ResponseEntity<?> completeOnboarding(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        userService.completeOnboarding(user);
        return ResponseEntity.ok().build();
    }
}
