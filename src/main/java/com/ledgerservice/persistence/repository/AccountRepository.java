package com.ledgerservice.persistence.repository;

import com.ledgerservice.persistence.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

}
