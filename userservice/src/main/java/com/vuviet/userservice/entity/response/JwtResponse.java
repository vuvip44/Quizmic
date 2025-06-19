package com.vuviet.userservice.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;

    private String refreshToken;

    private long id;

    private String username;

    private String email;

    private String role;
}
