package tn.mathzero.curriculum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.mathzero.curriculum.api.CurriculumDtos;
import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.repository.ChapterRepository;
import tn.mathzero.curriculum.repository.CurriculumAuditEventRepository;
import tn.mathzero.curriculum.repository.CurriculumRepository;
import tn.mathzero.curriculum.repository.CurriculumVersionRepository;
import tn.mathzero.curriculum.repository.GradeRepository;
import tn.mathzero.curriculum.repository.SchoolLevelRepository;
import tn.mathzero.curriculum.repository.SkillRepository;
import tn.mathzero.curriculum.repository.SpecializationRepository;
import tn.mathzero.curriculum.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class CurriculumImportServiceTest {
    @Mock private CurriculumRepository curricula;
    @Mock private CurriculumVersionRepository versions;
    @Mock private SchoolLevelRepository levels;
    @Mock private GradeRepository grades;
    @Mock private SpecializationRepository specializations;
    @Mock private SubjectRepository subjects;
    @Mock private ChapterRepository chapters;
    @Mock private SkillRepository skills;
    @Mock private CurriculumAuditEventRepository auditEvents;
    @Mock private CurriculumAdminService adminService;

    private CurriculumImportService service;

    @BeforeEach
    void setUp() {
        service = new CurriculumImportService(
                curricula, versions, levels, grades, specializations,
                subjects, chapters, skills, auditEvents, adminService);
    }

    @Test
    void dryRunValidatesWithoutWriting() {
        when(curricula.findByCodeIgnoreCase("TN-MATH")).thenReturn(Optional.empty());

        CurriculumDtos.ImportResult result = service.importCurriculum(
                UUID.randomUUID(), validDocument(), true);

        assertTrue(result.valid());
        assertTrue(result.dryRun());
        assertEquals(1, result.levels());
        assertEquals(1, result.grades());
        assertEquals(1, result.subjects());
        assertEquals(1, result.chapters());
        assertEquals(2, result.skills());
        verify(curricula, never()).save(any());
        verify(versions, never()).save(any());
        verify(auditEvents, never()).save(any());
    }

    @Test
    void rejectsUnknownSkillReferences() {
        when(curricula.findByCodeIgnoreCase("TN-MATH")).thenReturn(Optional.empty());
        CurriculumDtos.CurriculumImportRequest document = documentWithChapterSkills(List.of("SK-MISSING"));

        CurriculumDtos.ImportResult result = service.importCurriculum(
                UUID.randomUUID(), document, true);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(message -> message.contains("SK-MISSING")));
        verify(curricula, never()).save(any());
    }

    @Test
    void rejectsDuplicateCodes() {
        when(curricula.findByCodeIgnoreCase("TN-MATH")).thenReturn(Optional.empty());
        CurriculumDtos.CurriculumImportRequest document = new CurriculumDtos.CurriculumImportRequest(
                "TN-MATH", "Programme", "البرنامج", "2026",
                LocalDate.of(2026, 9, 1), null, null, null, false,
                List.of(
                        new CurriculumDtos.ImportLevel("PRIMARY", "Primaire", "الابتدائي", 0, List.of()),
                        new CurriculumDtos.ImportLevel("primary", "Primaire", "الابتدائي", 1, List.of())),
                List.of(), List.of(), List.of());

        CurriculumDtos.ImportResult result = service.importCurriculum(
                UUID.randomUUID(), document, true);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(message -> message.contains("Duplicate level code")));
    }

    @Test
    void refusesToOverwriteAPublishedVersion() {
        Curriculum curriculum = new Curriculum();
        curriculum.setId(UUID.randomUUID());
        curriculum.setCode("TN-MATH");
        CurriculumVersion version = new CurriculumVersion();
        version.setId(UUID.randomUUID());
        version.setCurriculum(curriculum);
        version.setVersionCode("2026");
        version.setStatus(CurriculumVersion.Status.PUBLISHED);
        when(curricula.findByCodeIgnoreCase("TN-MATH")).thenReturn(Optional.of(curriculum));
        when(versions.findByCurriculum_IdAndVersionCodeIgnoreCase(curriculum.getId(), "2026"))
                .thenReturn(Optional.of(version));

        CurriculumDtos.ImportResult result = service.importCurriculum(
                UUID.randomUUID(), validDocument(), true);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(message -> message.contains("PUBLISHED")));
        verify(versions, never()).save(any());
    }

    @Test
    void rejectsSelfReferencingAndCyclicPrerequisites() {
        when(curricula.findByCodeIgnoreCase("TN-MATH")).thenReturn(Optional.empty());
        CurriculumDtos.CurriculumImportRequest document = new CurriculumDtos.CurriculumImportRequest(
                "TN-MATH", "Programme", "البرنامج", "2026",
                LocalDate.of(2026, 9, 1), null, null, null, false,
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new CurriculumDtos.ImportSkill("SK-A", "A", "أ", "Objectif", "هدف", List.of("SK-B")),
                        new CurriculumDtos.ImportSkill("SK-B", "B", "ب", "Objectif", "هدف", List.of("SK-A"))));

        CurriculumDtos.ImportResult result = service.importCurriculum(
                UUID.randomUUID(), document, true);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(message -> message.contains("cycle")));
    }

    private static CurriculumDtos.CurriculumImportRequest validDocument() {
        return documentWithChapterSkills(List.of("SK-A", "SK-B"));
    }

    private static CurriculumDtos.CurriculumImportRequest documentWithChapterSkills(List<String> skillCodes) {
        return new CurriculumDtos.CurriculumImportRequest(
                "TN-MATH",
                "Programme tunisien",
                "البرنامج التونسي",
                "2026",
                LocalDate.of(2026, 9, 1),
                null,
                "Ministère",
                "Reviewer",
                false,
                List.of(new CurriculumDtos.ImportLevel(
                        "PRIMARY", "Primaire", "الابتدائي", 0,
                        List.of(new CurriculumDtos.ImportGrade(
                                "P1", "Première année", "السنة الأولى", 0,
                                List.of(), List.of("MATH"))))),
                List.of(),
                List.of(new CurriculumDtos.ImportSubject(
                        "MATH", "Mathématiques", "الرياضيات", 0,
                        List.of(new CurriculumDtos.ImportChapter(
                                "NUM-01", "Nombres", "الأعداد", 0, skillCodes)))),
                List.of(
                        new CurriculumDtos.ImportSkill(
                                "SK-A", "Lire", "قراءة", "Objectif", "هدف", List.of()),
                        new CurriculumDtos.ImportSkill(
                                "SK-B", "Calculer", "حساب", "Objectif", "هدف", List.of("SK-A"))));
    }
}
