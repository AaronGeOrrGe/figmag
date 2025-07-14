package com.figmine.backend.service;

import com.figmine.backend.dto.UserProfileResponse;
import com.figmine.backend.exception.ResourceNotFoundException;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        log.debug("Fetching profile for user: {}", user.getEmail());
        return mapToProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public User createUser(User user) {
        log.info("Creating new user with email: {}", user.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + user.getEmail());
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default values
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setOnboardingComplete(false);

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(User user, String name, String avatarUrl) {
        log.debug("Updating profile for user: {}", user.getEmail());
        
        if (StringUtils.hasText(name)) {
            user.setName(name);
        }
        
        if (StringUtils.hasText(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(User user, String newPassword) {
        log.debug("Updating password for user: {}", user.getEmail());
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User completeOnboarding(User user) {
        log.info("Completing onboarding for user: {}", user.getEmail());
        user.setOnboardingComplete(true);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && userRepository.existsByEmail(email);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .onboardingComplete(user.isOnboardingComplete())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
