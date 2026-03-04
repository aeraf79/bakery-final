package com.example.myproject.service;

import com.example.myproject.dto.UserDTO;
import com.example.myproject.entity.UserEntity;
import com.example.myproject.exception.ResourceNotFoundException;
import com.example.myproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Map entity → DTO (never expose password)
    private UserDTO toDTO(UserEntity user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        return dto;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toDTO(user);
    }

    // Used by GET /api/users/profile (Checkout.jsx, etc.)
    public UserDTO getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return toDTO(user);
    }

    public UserDTO updateUser(Long id, UserDTO dto) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getRole() != null) user.setRole(dto.getRole().toUpperCase());

        return toDTO(userRepository.save(user));
    }

    // Used by PATCH /api/users/patch/{id} — handles profile fields + optional password change
    public UserDTO patchUser(Long id, Map<String, Object> updates) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update basic profile fields
        if (updates.containsKey("fullName")) {
            user.setFullName((String) updates.get("fullName"));
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber((String) updates.get("phoneNumber"));
        }
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            // Only update email if it actually changed (avoid unique constraint issues)
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new RuntimeException("Email is already in use by another account.");
                }
                user.setEmail(newEmail);
            }
        }
        if (updates.containsKey("role")) {
            user.setRole(((String) updates.get("role")).toUpperCase());
        }

        // Password change: requires currentPassword + newPassword
        String newPassword = (String) updates.get("newPassword");
        if (newPassword != null && !newPassword.isEmpty()) {
            String currentPassword = (String) updates.get("currentPassword");
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new RuntimeException("Current password is required to set a new password.");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("Current password is incorrect.");
            }
            if (newPassword.length() < 6) {
                throw new RuntimeException("New password must be at least 6 characters.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        return toDTO(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}