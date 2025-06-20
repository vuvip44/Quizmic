package com.vuviet.userservice.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;

    private String refreshToken;

    private String type="Bearer";

    private long id;

    private String username;

    private String email;

    private String role;
    // Constructor với 6 tham số (không có type)
    public JwtResponse(String accessToken, String refreshToken, Long id,
                       String username, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
