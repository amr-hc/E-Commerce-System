package com.intelligent.ecommerce.entity;

import java.time.LocalDateTime;

import com.intelligent.ecommerce.enums.Role;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    private Long id;

    private String email;

    private String password;

    private String username;

    private String phoneNumber;

    private LocalDateTime createdAt;

    private Boolean isVerified;

    @Enumerated(EnumType.STRING)
    private Role role;
}
