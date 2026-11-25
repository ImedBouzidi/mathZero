package tn.mathzero.identity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.identity.domain.TokenRecord;

public interface TokenRecordRepository extends JpaRepository<TokenRecord, UUID> {
}
