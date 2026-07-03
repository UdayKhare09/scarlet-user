package org.teamzemo.scarletuser.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "scarlet-auth", url = "${app.auth-service-url:http://scarlet-auth}")
public interface AuthServiceClient {

    @PostMapping("/api/auth/internal/sync-profile")
    void syncProfile(
            @RequestParam("userId") UUID userId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName
    );
}
