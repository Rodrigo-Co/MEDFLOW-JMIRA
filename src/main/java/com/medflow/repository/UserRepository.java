package com.medflow.repository;

import com.medflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailAndRole(String email, String role);
    Optional<User> findByUsernameAndRole(String username, String role);
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
}
