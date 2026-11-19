package com.fas.repository;

import com.fas.entity.StudentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentAccount, Long> {
    Optional<StudentAccount> findByUsername(String username);
}
