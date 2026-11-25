package tn.mathzero.curriculum.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import tn.mathzero.curriculum.domain.CurriculumVersion;

public interface CurriculumVersionRepository extends JpaRepository<CurriculumVersion, UUID> {
    Page<CurriculumVersion> findByCurriculum_CodeIgnoreCaseAndStatusOrderByEffectiveFromDesc(
            String code,
            CurriculumVersion.Status status,
            Pageable pageable);

    List<CurriculumVersion> findByCurriculum_IdAndStatus(UUID curriculumId, CurriculumVersion.Status status);
    Optional<CurriculumVersion> findByIdAndStatus(UUID id, CurriculumVersion.Status status);
    Optional<CurriculumVersion> findByCurriculum_IdAndVersionCodeIgnoreCase(UUID curriculumId, String versionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from CurriculumVersion v where v.id = :id")
    Optional<CurriculumVersion> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
