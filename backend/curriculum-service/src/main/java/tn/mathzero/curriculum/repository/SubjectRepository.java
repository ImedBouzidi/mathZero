package tn.mathzero.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Subject;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Page<Subject> findDistinctByGrades_IdAndCurriculumVersion_StatusOrderByDisplayOrderAsc(
            UUID gradeId,
            CurriculumVersion.Status status,
            Pageable pageable);

    Optional<Subject> findByCurriculumVersion_IdAndCodeIgnoreCase(UUID versionId, String code);
}
