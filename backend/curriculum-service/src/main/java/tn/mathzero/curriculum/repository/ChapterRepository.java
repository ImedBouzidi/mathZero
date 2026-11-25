package tn.mathzero.curriculum.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.curriculum.domain.Chapter;
import tn.mathzero.curriculum.domain.CurriculumVersion;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    Page<Chapter> findBySubject_IdAndSubject_CurriculumVersion_StatusOrderByDisplayOrderAsc(
            UUID subjectId,
            CurriculumVersion.Status status,
            Pageable pageable);

    Optional<Chapter> findBySubject_IdAndCodeIgnoreCase(UUID subjectId, String code);
}
