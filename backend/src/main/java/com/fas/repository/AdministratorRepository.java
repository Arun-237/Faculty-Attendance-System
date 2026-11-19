package com.fas.repository;

import com.fas.entity.AdministratorAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdministratorRepository extends JpaRepository<AdministratorAccount, Long> {
    Optional<AdministratorAccount> findByUsername(String username);
}
