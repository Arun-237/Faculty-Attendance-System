package com.fas.repository;

import com.fas.entity.FacultyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<FacultyAccount, Long> {
    Optional<FacultyAccount> findByUsername(String username);
    Optional<FacultyAccount> findByEmail(String email);
}
