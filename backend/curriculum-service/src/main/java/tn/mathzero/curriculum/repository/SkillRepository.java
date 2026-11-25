package tn.mathzero.curriculum.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Skill;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Page<Skill> findDistinctByChapters_IdAndCurriculumVersion_StatusOrderByCodeAsc(
            UUID chapterId,
            CurriculumVersion.Status status,
            Pageable pageable);

    List<Skill> findByCurriculumVersion_Id(UUID versionId);

    Optional<Skill> findByCurriculumVersion_IdAndCodeIgnoreCase(UUID versionId, String code);
}
