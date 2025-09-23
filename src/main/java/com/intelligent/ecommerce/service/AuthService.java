package com.intelligent.ecommerce.service;


import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.intelligent.ecommerce.dto.auth.AuthResponse;
import com.intelligent.ecommerce.dto.auth.LoginRequest;
import com.intelligent.ecommerce.dto.auth.SignupRequest;
import com.intelligent.ecommerce.entity.User;
import com.intelligent.ecommerce.enums.Role;
import com.intelligent.ecommerce.repository.UserRepository;
import com.intelligent.ecommerce.security.JwtUtil;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
@Data

@AllArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;


    public AuthResponse login(LoginRequest request)
    {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword()))
        {
            throw new BadCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), user.getRole());
    }


    public AuthResponse register(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPn());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.User);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsVerified(true);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), user.getRole());

    }

}

