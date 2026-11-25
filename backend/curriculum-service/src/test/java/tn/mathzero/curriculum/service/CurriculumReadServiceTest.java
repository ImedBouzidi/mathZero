package tn.mathzero.curriculum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.repository.ChapterRepository;
import tn.mathzero.curriculum.repository.CurriculumRepository;
import tn.mathzero.curriculum.repository.CurriculumVersionRepository;
import tn.mathzero.curriculum.repository.GradeRepository;
import tn.mathzero.curriculum.repository.SchoolLevelRepository;
import tn.mathzero.curriculum.repository.SkillRepository;
import tn.mathzero.curriculum.repository.SpecializationRepository;
import tn.mathzero.curriculum.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class CurriculumReadServiceTest {
    @Mock private CurriculumRepository curricula;
    @Mock private CurriculumVersionRepository versions;
    @Mock private SchoolLevelRepository levels;
    @Mock private GradeRepository grades;
    @Mock private SpecializationRepository specializations;
    @Mock private SubjectRepository subjects;
    @Mock private ChapterRepository chapters;
    @Mock private SkillRepository skills;

    private CurriculumReadService service;

    @BeforeEach
    void setUp() {
        service = new CurriculumReadService(
                curricula, versions, levels, grades, specializations, subjects, chapters, skills);
    }

    @Test
    void selectsThePublishedVersionEffectiveOnTheRequestedDate() {
        LocalDate asOf = LocalDate.of(2026, 10, 1);
        Curriculum curriculum = new Curriculum();
        curriculum.setId(UUID.randomUUID());
        curriculum.setCode("TN");
        curriculum.setNameFr("Programme tunisien");
        curriculum.setNameAr("المنهج التونسي");

        CurriculumVersion expired = version(curriculum, "2024", "2024-09-01", "2025-08-31");
        CurriculumVersion effective = version(curriculum, "2026", "2026-09-01", null);
        when(curricula.findPublishedOn(
                eq(CurriculumVersion.Status.PUBLISHED), eq(asOf), eq("Tunisie"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(curriculum), PageRequest.of(0, 20), 1));
        when(versions.findByCurriculum_IdAndStatus(
                curriculum.getId(), CurriculumVersion.Status.PUBLISHED))
                .thenReturn(List.of(expired, effective));

        var result = service.curricula(" Tunisie ", asOf, 0, 20);

        assertEquals(1, result.totalElements());
        assertEquals("2026", result.items().get(0).currentVersion().code());
        assertEquals("المنهج التونسي", result.items().get(0).nameAr());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(curricula).findPublishedOn(
                eq(CurriculumVersion.Status.PUBLISHED), eq(asOf), eq("Tunisie"), pageableCaptor.capture());
        assertEquals(Sort.by("code").ascending(), pageableCaptor.getValue().getSort());
    }

    @Test
    void enforcesTheMaximumPageSize() {
        CurriculumException exception = assertThrows(
                CurriculumException.class,
                () -> service.curricula(null, LocalDate.now(), 0, 101));

        assertEquals("INVALID_REQUEST", exception.getCode());
    }

    private static CurriculumVersion version(
            Curriculum curriculum, String code, String from, String to) {
        CurriculumVersion version = new CurriculumVersion();
        version.setId(UUID.randomUUID());
        version.setCurriculum(curriculum);
        version.setVersionCode(code);
        version.setEffectiveFrom(LocalDate.parse(from));
        version.setEffectiveTo(to == null ? null : LocalDate.parse(to));
        version.setStatus(CurriculumVersion.Status.PUBLISHED);
        return version;
    }
}
