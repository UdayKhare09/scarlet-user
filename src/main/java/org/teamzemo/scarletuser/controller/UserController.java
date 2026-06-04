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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Name", required = false) String fullName) {

        String firstName = "";
        String lastName = "";
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        User user = userService.syncUser(userId, email, firstName, lastName);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/status")
    public ResponseEntity<User> getMyStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Name", required = false) String fullName) {
        User user = userService.getUser(userId);
        if (user == null) {
            String firstName = "";
            String lastName = "";
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            }
            user = userService.syncUser(userId, email, firstName, lastName);
        }
        return ResponseEntity.ok(user);
    }
}
