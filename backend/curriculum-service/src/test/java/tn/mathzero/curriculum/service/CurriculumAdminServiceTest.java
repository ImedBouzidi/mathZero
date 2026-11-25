package tn.mathzero.curriculum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.mathzero.curriculum.api.CurriculumDtos;
import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumAuditEvent;
import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Skill;
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
class CurriculumAdminServiceTest {
    @Mock private CurriculumRepository curricula;
    @Mock private CurriculumVersionRepository versions;
    @Mock private SchoolLevelRepository levels;
    @Mock private GradeRepository grades;
    @Mock private SpecializationRepository specializations;
    @Mock private SubjectRepository subjects;
    @Mock private ChapterRepository chapters;
    @Mock private SkillRepository skills;
    @Mock private CurriculumAuditEventRepository auditEvents;

    private CurriculumAdminService service;

    @BeforeEach
    void setUp() {
        service = new CurriculumAdminService(
                curricula, versions, levels, grades, specializations, subjects, chapters, skills, auditEvents);
    }

    @Test
    void rejectsPrerequisiteThatWouldCreateACycle() {
        UUID versionId = UUID.randomUUID();
        CurriculumVersion version = draftVersion(versionId);
        Skill skill = skill("SKILL_A", version);
        Skill prerequisite = skill("SKILL_B", version);
        prerequisite.getPrerequisites().add(skill);
        when(skills.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(skills.findById(prerequisite.getId())).thenReturn(Optional.of(prerequisite));
        when(versions.findByIdForUpdate(versionId)).thenReturn(Optional.of(version));

        CurriculumException exception = assertThrows(
                CurriculumException.class,
                () -> service.addPrerequisite(UUID.randomUUID(), skill.getId(), prerequisite.getId()));

        assertEquals("CONFLICT", exception.getCode());
        verify(skills, never()).save(any());
        verify(auditEvents, never()).save(any());
    }

    @Test
    void rejectsASkillAsItsOwnPrerequisite() {
        CurriculumVersion version = draftVersion(UUID.randomUUID());
        Skill skill = skill("SKILL_A", version);
        when(skills.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(versions.findByIdForUpdate(version.getId())).thenReturn(Optional.of(version));

        CurriculumException exception = assertThrows(
                CurriculumException.class,
                () -> service.addPrerequisite(UUID.randomUUID(), skill.getId(), skill.getId()));

        assertEquals("INVALID_REQUEST", exception.getCode());
        verify(skills, never()).save(any());
        verify(auditEvents, never()).save(any());
    }

    @Test
    void rejectsPrerequisitesAcrossCurriculumVersions() {
        CurriculumVersion versionA = draftVersion(UUID.randomUUID());
        CurriculumVersion versionB = draftVersion(UUID.randomUUID());
        Skill skill = skill("SKILL_A", versionA);
        Skill prerequisite = skill("SKILL_B", versionB);
        when(skills.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(skills.findById(prerequisite.getId())).thenReturn(Optional.of(prerequisite));
        when(versions.findByIdForUpdate(versionA.getId())).thenReturn(Optional.of(versionA));

        CurriculumException exception = assertThrows(
                CurriculumException.class,
                () -> service.addPrerequisite(UUID.randomUUID(), skill.getId(), prerequisite.getId()));

        assertEquals("INVALID_REQUEST", exception.getCode());
        verify(skills, never()).save(any());
        verify(auditEvents, never()).save(any());
    }

    @Test
    void recordsTheActorAndEntityForSuccessfulCurriculumCreation() {
        UUID actorId = UUID.randomUUID();
        UUID curriculumId = UUID.randomUUID();
        when(curricula.existsByCodeIgnoreCase("TN")).thenReturn(false);
        when(curricula.save(any(Curriculum.class))).thenAnswer(invocation -> {
            Curriculum saved = invocation.getArgument(0);
            saved.setId(curriculumId);
            return saved;
        });

        CurriculumDtos.CurriculumView result = service.createCurriculum(
                actorId, new CurriculumDtos.CreateCurriculumRequest(" TN ", "Programme", "المنهج"));

        assertEquals(curriculumId, result.id());
        ArgumentCaptor<CurriculumAuditEvent> eventCaptor = ArgumentCaptor.forClass(CurriculumAuditEvent.class);
        verify(auditEvents).save(eventCaptor.capture());
        CurriculumAuditEvent event = eventCaptor.getValue();
        assertEquals(actorId, event.getActorUserId());
        assertEquals("CREATE", event.getAction());
        assertEquals("curriculum", event.getEntityType());
        assertEquals(curriculumId, event.getEntityId());
    }

    private static CurriculumVersion draftVersion(UUID id) {
        CurriculumVersion version = new CurriculumVersion();
        version.setId(id);
        version.setVersionCode("draft");
        version.setEffectiveFrom(LocalDate.of(2026, 9, 1));
        version.setStatus(CurriculumVersion.Status.DRAFT);
        return version;
    }

    private static Skill skill(String code, CurriculumVersion version) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setCode(code);
        skill.setCurriculumVersion(version);
        return skill;
    }
}
