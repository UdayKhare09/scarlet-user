package org.teamzemo.scarletuser.dto;

import java.util.UUID;

public record UserSyncRequest(
    UUID id,
    String email,
    String firstName,
    String lastName
) {}
