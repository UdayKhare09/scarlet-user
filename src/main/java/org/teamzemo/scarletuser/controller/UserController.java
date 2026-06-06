package org.teamzemo.scarletuser.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.teamzemo.scarletuser.entity.User;
import org.teamzemo.scarletuser.service.UserService;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.teamzemo.scarletuser.dto.UserSyncRequest;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/status")
    public ResponseEntity<User> getMyStatus(@RequestHeader("X-User-Id") UUID userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/internal/sync")
    public ResponseEntity<User> syncUser(@RequestBody UserSyncRequest request) {
        User user = userService.syncUser(
                request.id(),
                request.email(),
                request.firstName(),
                request.lastName()
        );
        return ResponseEntity.ok(user);
    }
}
