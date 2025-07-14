package com.figmine.backend.service;

import com.figmine.backend.dto.UserLoginRequest;
import com.figmine.backend.dto.UserSignupRequest;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .avatarUrl(null)
                .onboardingComplete(false)
                .build();
        return userRepository.save(user);
    }

    public Optional<User> login(UserLoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }
}
