package com.twocall.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 100000) @Pattern(regexp = "[A-Za-z0-9+/=]*") String imageBase64) {}
