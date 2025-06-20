package com.vuviet.userservice.entity.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDto {
    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 character")
    private String password;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
}
