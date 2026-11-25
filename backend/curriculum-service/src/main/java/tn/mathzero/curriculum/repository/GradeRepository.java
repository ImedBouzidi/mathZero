package tn.mathzero.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Grade;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    Page<Grade> findBySchoolLevel_IdAndSchoolLevel_Version_StatusOrderByDisplayOrderAsc(
            UUID levelId,
            CurriculumVersion.Status status,
            Pageable pageable);

    Optional<Grade> findBySchoolLevel_IdAndCodeIgnoreCase(UUID levelId, String code);
}
