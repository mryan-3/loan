package com.ryanm.loan.service;

import com.ryanm.loan.dto.AuthResponse;
import com.ryanm.loan.dto.ChangePasswordRequest;
import com.ryanm.loan.dto.UserLoginRequest;
import com.ryanm.loan.dto.UserRegistrationRequest;
import com.ryanm.loan.dto.UserResponse;
import com.ryanm.loan.exception.AuthenticationException;
import com.ryanm.loan.exception.ResourceNotFoundException;
import com.ryanm.loan.exception.ValidationException;
import com.ryanm.loan.model.Role;
import com.ryanm.loan.model.User;
import com.ryanm.loan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already in use");
        }
        try {
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone())
                    .income(request.getIncome())
                    .role(request.getRole() != null ? Role.valueOf(request.getRole().toUpperCase()) : Role.CUSTOMER)
                    .build();
            userRepository.save(user);
            log.info("User registered successfully: {}", user.getEmail());
            String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId());
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(mapToDto(user))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role provided: {}", request.getRole());
            throw new ValidationException("Invalid role: " + request.getRole());
        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage(), e);
            throw new ValidationException("Registration failed");
        }
    }

    public AuthResponse login(UserLoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }
        log.info("User logged in successfully: {}", user.getEmail());
        String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToDto(user))
                .build();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
        return mapToDto(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return mapToDto(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("User password change attempt: {}", email);
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Invalid current password for user: {}", email);
            throw new ValidationException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User password changed successfully: {}", email);
    }

    public UserResponse updateProfileImage(String email, MultipartFile file) {
        log.info("User profile image upload attempt: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Validate file
        if (file.isEmpty()) {
            throw new ValidationException("No file uploaded");
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new ValidationException("Only image files are allowed");
        }
        if (file.getSize() > 2 * 1024 * 1024) { // 2MB limit
            throw new ValidationException("File size exceeds 2MB limit");
        }

        // Save file
        String uploadDir = "uploads/profile-images";
        String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());
            // Optionally, delete old image file
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                File oldFile = new File(user.getImage());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            user.setImage(filePath.toString());
            userRepository.save(user);
            log.info("User profile image updated: {}", email);
            return mapToDto(user);
        } catch (IOException e) {
            log.error("Error saving profile image for user {}: {}", email, e.getMessage(), e);
            throw new ValidationException("Failed to save image");
        }
    }

    public void deleteAccount(String email) {
        log.info("User account delete attempt: {}", email);
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        // Delete profile image file if exists
        if (user.getImage() != null && !user.getImage().isEmpty()) {
            File oldFile = new File(user.getImage());
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }
        user.setDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        log.info("User account soft deleted: {}", email);
    }

    // For Spring Security
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    // Helper method to map User entity to UserResponse DTO
    private UserResponse mapToDto(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .image(user.getImage())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(FORMATTER) : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().format(FORMATTER) : null)
                .build();
    }
}
