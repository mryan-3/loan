package com.ryanm.loan.controller;

import com.ryanm.loan.dto.AuthResponse;
import com.ryanm.loan.dto.ChangePasswordRequest;
import com.ryanm.loan.dto.UserLoginRequest;
import com.ryanm.loan.dto.UserRegistrationRequest;
import com.ryanm.loan.dto.UserResponse;
import com.ryanm.loan.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    // 1. Register (Sign Up)
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("API: Register user");
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    // 2. Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("API: User login");
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    // 3. Get Profile (current user)
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("API: Get profile for user: {}", userDetails.getUsername());
        UserResponse response = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    // 4. Logout (stateless, just a placeholder for API spec)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // In stateless JWT, logout is handled on the client by deleting the token.
        log.info("API: User logout (stateless, no server action)");
        return ResponseEntity.ok().build();
    }

    // 5. Change Password
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        log.info("API: Change password for user: {}", userDetails.getUsername());
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    // 6. Change Profile Image
    @PatchMapping("/image")
    public ResponseEntity<UserResponse> updateProfileImage(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam("file") MultipartFile file) {
        log.info("API: Update profile image for user: {}", userDetails.getUsername());
        UserResponse response = userService.updateProfileImage(userDetails.getUsername(), file);
        return ResponseEntity.ok(response);
    }
} 