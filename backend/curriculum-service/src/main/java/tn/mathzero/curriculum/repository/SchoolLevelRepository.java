package tn.mathzero.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.SchoolLevel;

public interface SchoolLevelRepository extends JpaRepository<SchoolLevel, UUID> {
    Page<SchoolLevel> findByVersion_IdAndVersion_StatusOrderByDisplayOrderAsc(
            UUID versionId,
            CurriculumVersion.Status status,
            Pageable pageable);

    Optional<SchoolLevel> findByVersion_IdAndCodeIgnoreCase(UUID versionId, String code);
}
