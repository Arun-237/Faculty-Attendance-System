package com.fas.repository;

import com.fas.entity.HodAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HodRepository extends JpaRepository<HodAccount, Long> {
    Optional<HodAccount> findByUsername(String username);
}
