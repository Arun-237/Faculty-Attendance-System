package com.fas.repository;

import com.fas.entity.StudentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentAccount, Long> {
    Optional<StudentAccount> findByUsername(String username);
    Optional<StudentAccount> findByEmail(String email);
    java.util.List<StudentAccount> findByUsernameIn(Collection<String> usernames);
    java.util.List<StudentAccount> findBySectionOrderByUsernameAsc(String section);
}
