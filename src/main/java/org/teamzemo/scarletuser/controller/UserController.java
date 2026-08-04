package org.teamzemo.scarletuser.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teamzemo.scarletuser.entity.User;
import org.teamzemo.scarletuser.service.UserService;

import java.util.UUID;

import org.teamzemo.scarletuser.dto.UserSyncRequest;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        log.info("Fetching profile for user ID: {}", userId);
        User user = userService.getUser(userId);
        if (user == null) {
            log.warn("Profile not found for user ID: {}", userId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/internal/sync")
    public ResponseEntity<User> syncUser(@RequestBody UserSyncRequest request) {
        log.info("Internal user sync request for email: {}", request.email());
        User user = userService.syncUser(
                request.id(),
                request.email(),
                request.firstName(),
                request.lastName()
        );
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody org.teamzemo.scarletuser.dto.UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request.firstName(), request.lastName());
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> uploadAvatar(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        User user = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(user);
    }
}
