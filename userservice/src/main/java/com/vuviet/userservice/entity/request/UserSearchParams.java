package com.vuviet.userservice.entity.request;

import lombok.Data;

@Data
public class UserSearchParams {
    private String username;

    private String email;

    private String fullName;



    private String rolName;

    private Boolean isActive;

    public UserSearchParams(){}

    public UserSearchParams(String username, String email, String fullName, String rolName, Boolean isActive) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.rolName = rolName;
        this.isActive = isActive;
    }
}
