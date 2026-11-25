package tn.mathzero.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Specialization;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    Page<Specialization> findDistinctByGrades_IdAndCurriculumVersion_StatusOrderByDisplayOrderAsc(
            UUID gradeId,
            CurriculumVersion.Status status,
            Pageable pageable);

    Optional<Specialization> findByCurriculumVersion_IdAndCodeIgnoreCase(UUID versionId, String code);
}
