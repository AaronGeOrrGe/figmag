package com.figmine.backend.security;

import com.figmine.backend.exception.RateLimitExceededException;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import com.figmine.backend.service.JwtService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Metrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String RATE_LIMIT_HEADER = "X-Rate-Limit-Remaining";
    private static final String TOKEN_BLACKLIST_HEADER = "X-Token-Blacklisted";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final Bucket rateLimitBucket;
    private final HandlerExceptionResolver resolver;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(
            JwtService jwtService,
            UserRepository userRepository,
            @Qualifier("rateLimitBucket") Bucket rateLimitBucket,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.rateLimitBucket = rateLimitBucket;
        this.resolver = resolver;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Apply rate limiting
        if (!applyRateLimit(request, response)) {
            Metrics.counter("rate_limit_exceeded", "path", request.getRequestURI()).increment();
            resolver.resolveException(request, response, null, new RateLimitExceededException("Rate limit exceeded"));
            return;
        }

        try {
            String token = extractToken(request);

            if (token != null) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    response.setHeader(TOKEN_BLACKLIST_HEADER, "true");
                    throw new InsufficientAuthenticationException("Token has been invalidated");
                }

                authenticateUserFromToken(token, request);
            }

            filterChain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            log.warn("Authentication failed: {}", ex.getMessage());
            resolver.resolveException(request, response, null, ex);
        } catch (Exception ex) {
            log.error("Security filter error: {}", ex.getMessage(), ex);
            resolver.resolveException(request, response, null,
                    new BadCredentialsException("Authentication failed"));
        }
    }

    private boolean applyRateLimit(HttpServletRequest request, HttpServletResponse response) {
        if (shouldNotFilter(request)) {
            return true;
        }

        ConsumptionProbe probe = rateLimitBucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader(RATE_LIMIT_HEADER,
                    String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader(RATE_LIMIT_HEADER, "0");
        response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private void authenticateUserFromToken(String token, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromJwt(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(username)
                        .orElseThrow(() -> new BadCredentialsException("User not found"));

                if (!jwtService.validateJwtToken(token)) {
                    throw new BadCredentialsException("Invalid or expired token");
                }

                List<GrantedAuthority> authorities = user.getAuthorities().stream()
                        .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                        .collect(Collectors.toList());

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(authorities)
                        .accountExpired(!user.isAccountNonExpired())
                        .accountLocked(!user.isAccountNonLocked())
                        .credentialsExpired(!user.isCredentialsNonExpired())
                        .disabled(!user.isEnabled())
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                Metrics.counter("auth_success", "user", username).increment();
            }
        } catch (Exception ex) {
            log.warn("Failed to authenticate user from token: {}", ex.getMessage());
            throw new BadCredentialsException("Authentication failed", ex);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && (
                path.startsWith("/api/auth/") ||
                        path.startsWith("/actuator/") ||
                        path.startsWith("/api-docs") ||
                        path.startsWith("/swagger-ui") ||
                        path.equals("/api/figma/callback") // ✅ Skip this path
        );
    }
}
