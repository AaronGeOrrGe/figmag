package com.figmine.backend.dto;

import lombok.Data;

@Data
public class UserSignupRequest {
    private String email;
    private String password;
    private String name;
}
