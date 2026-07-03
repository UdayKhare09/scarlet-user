package org.teamzemo.scarletuser.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import org.teamzemo.scarletuser.client.AuthServiceClient;
import org.teamzemo.scarletuser.entity.User;
import org.teamzemo.scarletuser.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthServiceClient authServiceClient;
    private final S3Client s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.minio.public-url-prefix}")
    private String publicUrlPrefix;

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

    @Transactional
    public User updateProfile(UUID userId, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        User savedUser = userRepository.save(user);

        // Sync name updates back to scarlet-auth
        try {
            authServiceClient.syncProfile(userId, firstName, lastName);
            log.info("Successfully synced updated profile details back to auth service for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to sync profile update back to scarlet-auth for user {}", userId, e);
        }
        return savedUser;
    }

    @Transactional
    public User uploadAvatar(UUID userId, MultipartFile file) {
        // Enforce limits
        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds maximum limit of 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") || contentType.equals("image/jpeg") || contentType.equals("image/webp"))) {
            throw new RuntimeException("Only PNG, JPEG and WEBP image formats are allowed");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Extract file extension
        String originalFilename = file.getOriginalFilename();
        String extension = "png";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }

        // Generate unique object key
        String objectKey = String.format("%s-%d.%s", userId, System.currentTimeMillis(), extension);

        // Track the old avatar url to delete after successful upload
        String oldUrl = user.getProfilePictureUrl();

        try {
            // Upload to MinIO
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build(), 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            // Construct public read URL
            String publicUrl = publicUrlPrefix + objectKey;

            // Update user entity
            user.setProfilePictureUrl(publicUrl);
            User savedUser = userRepository.save(user);
            log.info("Uploaded avatar successfully for user: {}, URL: {}", userId, publicUrl);

            // Successfully uploaded new, delete old one if it existed
            if (oldUrl != null && oldUrl.startsWith(publicUrlPrefix)) {
                String oldKey = oldUrl.substring(publicUrlPrefix.length());
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(oldKey)
                            .build()
                    );
                    log.info("Deleted old avatar file from S3: {}", oldKey);
                } catch (Exception s3Ex) {
                    log.warn("Failed to delete old avatar file from S3: {}", oldKey, s3Ex);
                }
            }

            return savedUser;

        } catch (Exception e) {
            log.error("Failed to upload avatar to S3/MinIO for user {}", userId, e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        }
    }
}

