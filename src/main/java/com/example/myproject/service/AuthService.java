package com.example.myproject.service;

import com.example.myproject.dto.AuthResponse;
import com.example.myproject.dto.LoginRequest;
import com.example.myproject.dto.RegisterRequest;
import com.example.myproject.entity.UserEntity;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private com.example.myproject.security.CustomUserDetailsService userDetailsService;

    @Autowired
    private EmailService emailService;

    // REGISTER
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole() != null ? request.getRole().toUpperCase() : "USER");

        userRepository.save(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(userDetails, claims);

        // FIX: pass user.getUserId() so localStorage has userId
        return new AuthResponse(token, user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(userDetails, claims);

        // FIX: pass user.getUserId() so localStorage has userId
        return new AuthResponse(token, user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}