package tn.mathzero.curriculum.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumAuditEvent;

public interface CurriculumAuditEventRepository extends JpaRepository<CurriculumAuditEvent, UUID> {
}
