package com.intelligent.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.intelligent.ecommerce.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>
{

    @Query("select e from User e where e.email = :email")
    Optional<User> findByEmail(String email);
}
