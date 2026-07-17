package com.ledgerservice.persistence.repository;

import com.ledgerservice.persistence.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByIdInOrderByIdAsc(Collection<Long> ids);
}
