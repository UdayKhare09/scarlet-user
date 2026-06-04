package org.teamzemo.scarletuser.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teamzemo.scarletuser.entity.User;
import org.teamzemo.scarletuser.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User syncUser(UUID keycloakId, String email, String firstName, String lastName) {
        return userRepository.findById(keycloakId)
                .map(existingUser -> {
                    boolean updated = false;
                    if (email != null && !email.equalsIgnoreCase(existingUser.getEmail())) {
                        existingUser.setEmail(email);
                        updated = true;
                    }
                    if (firstName != null && !firstName.equals(existingUser.getFirstName())) {
                        existingUser.setFirstName(firstName);
                        updated = true;
                    }
                    if (lastName != null && !lastName.equals(existingUser.getLastName())) {
                        existingUser.setLastName(lastName);
                        updated = true;
                    }
                    if (updated) {
                        log.info("Updating existing user info for ID: {}", keycloakId);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    return userRepository.findByEmail(email)
                            .map(existingUserByEmail -> {
                                log.info("Merging user with existing email: {} under new Keycloak UUID: {}", email, keycloakId);
                                userRepository.delete(existingUserByEmail);
                                userRepository.flush();
                                User mergedUser = User.builder()
                                        .id(keycloakId)
                                        .email(email)
                                        .firstName(firstName)
                                        .lastName(lastName)
                                        .build();
                                return userRepository.save(mergedUser);
                            })
                            .orElseGet(() -> {
                                log.info("Creating new user profile for Keycloak ID: {}", keycloakId);
                                User newUser = User.builder()
                                        .id(keycloakId)
                                        .email(email)
                                        .firstName(firstName)
                                        .lastName(lastName)
                                        .build();
                                return userRepository.save(newUser);
                            });
                });
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id).orElse(null);
    }
}

