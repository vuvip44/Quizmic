package com.vuviet.userservice.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponse {
    private long id;

    private String username;

    private String email;

    private String fullName;

    private String role;

    private Boolean isActive;

    private LocalDateTime createdAt;
}
