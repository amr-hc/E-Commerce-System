package com.intelligent.ecommerce.dto.auth;


import com.intelligent.ecommerce.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse
{
    private String token;
    private String username;
    private Role role;
}