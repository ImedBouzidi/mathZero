package tn.mathzero.curriculum.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.mathzero.curriculum.api.CurriculumDtos;
import tn.mathzero.curriculum.domain.Chapter;
import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumAuditEvent;
import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Grade;
import tn.mathzero.curriculum.domain.SchoolLevel;
import tn.mathzero.curriculum.domain.Skill;
import tn.mathzero.curriculum.domain.Specialization;
import tn.mathzero.curriculum.domain.Subject;
import tn.mathzero.curriculum.repository.ChapterRepository;
import tn.mathzero.curriculum.repository.CurriculumAuditEventRepository;
import tn.mathzero.curriculum.repository.CurriculumRepository;
import tn.mathzero.curriculum.repository.CurriculumVersionRepository;
import tn.mathzero.curriculum.repository.GradeRepository;
import tn.mathzero.curriculum.repository.SchoolLevelRepository;
import tn.mathzero.curriculum.repository.SkillRepository;
import tn.mathzero.curriculum.repository.SpecializationRepository;
import tn.mathzero.curriculum.repository.SubjectRepository;

@Service
public class CurriculumAdminService {
    private final CurriculumRepository curricula;
    private final CurriculumVersionRepository versions;
    private final SchoolLevelRepository levels;
    private final GradeRepository grades;
    private final SpecializationRepository specializations;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final SkillRepository skills;
    private final CurriculumAuditEventRepository auditEvents;

    public CurriculumAdminService(
            CurriculumRepository curricula,
            CurriculumVersionRepository versions,
            SchoolLevelRepository levels,
            GradeRepository grades,
            SpecializationRepository specializations,
            SubjectRepository subjects,
            ChapterRepository chapters,
            SkillRepository skills,
            CurriculumAuditEventRepository auditEvents) {
        this.curricula = curricula;
        this.versions = versions;
        this.levels = levels;
        this.grades = grades;
        this.specializations = specializations;
        this.subjects = subjects;
        this.chapters = chapters;
        this.skills = skills;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public CurriculumDtos.CurriculumView createCurriculum(
            UUID actorId, CurriculumDtos.CreateCurriculumRequest request) {
        String code = request.code().trim();
        if (curricula.existsByCodeIgnoreCase(code)) {
            throw CurriculumException.conflict("A curriculum with this code already exists");
        }
        Curriculum curriculum = new Curriculum();
        curriculum.setId(UUID.randomUUID());
        curriculum.setCode(code);
        curriculum.setNameFr(request.nameFr().trim());
        curriculum.setNameAr(request.nameAr().trim());
        curricula.save(curriculum);
        audit(actorId, "CREATE", "curriculum", curriculum.getId());
        return new CurriculumDtos.CurriculumView(
                curriculum.getId(), curriculum.getCode(), curriculum.getNameFr(), curriculum.getNameAr(), null);
    }

    @Transactional
    public CurriculumDtos.VersionView createVersion(
            UUID actorId, UUID curriculumId, CurriculumDtos.CreateVersionRequest request) {
        Curriculum curriculum = requireCurriculum(curriculumId);
        validateDates(request.effectiveFrom(), request.effectiveTo());
        CurriculumVersion version = new CurriculumVersion();
        version.setId(UUID.randomUUID());
        version.setCurriculum(curriculum);
        version.setVersionCode(request.code().trim());
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        version.setStatus(CurriculumVersion.Status.DRAFT);
        versions.save(version);
        audit(actorId, "CREATE_VERSION", "curriculum_version", version.getId());
        return toView(version);
    }

    @Transactional
    public CurriculumDtos.VersionView updateVersionDates(
            UUID actorId, UUID versionId, CurriculumDtos.UpdateVersionDatesRequest request) {
        CurriculumVersion version = requireDraftVersion(versionId);
        validateDates(request.effectiveFrom(), request.effectiveTo());
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        versions.save(version);
        audit(actorId, "UPDATE_VERSION_DATES", "curriculum_version", version.getId());
        return toView(version);
    }

    @Transactional
    public CurriculumDtos.NamedNode createLevel(
            UUID actorId, UUID versionId, CurriculumDtos.CreateNamedNodeRequest request) {
        CurriculumVersion version = requireDraftVersion(versionId);
        SchoolLevel level = new SchoolLevel();
        level.setId(UUID.randomUUID());
        level.setVersion(version);
        setNamed(level, request);
        levels.save(level);
        audit(actorId, "CREATE", "school_level", level.getId());
        return toView(level);
    }

    @Transactional
    public CurriculumDtos.NamedNode createGrade(
            UUID actorId, UUID levelId, CurriculumDtos.CreateNamedNodeRequest request) {
        SchoolLevel level = levels.findById(levelId)
                .orElseThrow(() -> CurriculumException.notFound("School level"));
        requireDraftVersion(level.getVersion().getId());
        Grade grade = new Grade();
        grade.setId(UUID.randomUUID());
        grade.setSchoolLevel(level);
        setNamed(grade, request);
        grades.save(grade);
        audit(actorId, "CREATE", "grade", grade.getId());
        return toView(grade);
    }

    @Transactional
    public CurriculumDtos.NamedNode createSpecialization(
            UUID actorId, UUID versionId, CurriculumDtos.CreateNamedNodeRequest request) {
        CurriculumVersion version = requireDraftVersion(versionId);
        Specialization specialization = new Specialization();
        specialization.setId(UUID.randomUUID());
        specialization.setCurriculumVersion(version);
        setNamed(specialization, request);
        specializations.save(specialization);
        audit(actorId, "CREATE", "specialization", specialization.getId());
        return toView(specialization);
    }

    @Transactional
    public void addSpecialization(UUID actorId, UUID gradeId, UUID specializationId) {
        Grade grade = requireGrade(gradeId);
        Specialization specialization = specializations.findById(specializationId)
                .orElseThrow(() -> CurriculumException.notFound("Specialization"));
        CurriculumVersion gradeVersion = requireDraftVersion(grade.getSchoolLevel().getVersion().getId());
        if (!gradeVersion.getId().equals(specialization.getCurriculumVersion().getId())) {
            throw CurriculumException.invalid("Grade and specialization must belong to the same curriculum version");
        }
        if (grade.getSpecializations().add(specialization)) {
            grades.save(grade);
            audit(actorId, "MAP", "grade_specialization", grade.getId());
        }
    }

    @Transactional
    public CurriculumDtos.NamedNode createSubject(
            UUID actorId, UUID versionId, CurriculumDtos.CreateNamedNodeRequest request) {
        CurriculumVersion version = requireDraftVersion(versionId);
        Subject subject = new Subject();
        subject.setId(UUID.randomUUID());
        subject.setCurriculumVersion(version);
        setNamed(subject, request);
        subjects.save(subject);
        audit(actorId, "CREATE", "subject", subject.getId());
        return toView(subject);
    }

    @Transactional
    public void addSubject(UUID actorId, UUID gradeId, UUID subjectId) {
        Grade grade = requireGrade(gradeId);
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> CurriculumException.notFound("Subject"));
        CurriculumVersion gradeVersion = requireDraftVersion(grade.getSchoolLevel().getVersion().getId());
        if (!gradeVersion.getId().equals(subject.getCurriculumVersion().getId())) {
            throw CurriculumException.invalid("Grade and subject must belong to the same curriculum version");
        }
        if (grade.getSubjects().add(subject)) {
            grades.save(grade);
            audit(actorId, "MAP", "grade_subject", grade.getId());
        }
    }

    @Transactional
    public CurriculumDtos.ChapterView createChapter(
            UUID actorId, UUID subjectId, CurriculumDtos.CreateChapterRequest request) {
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> CurriculumException.notFound("Subject"));
        requireDraftVersion(subject.getCurriculumVersion().getId());
        Chapter chapter = new Chapter();
        chapter.setId(UUID.randomUUID());
        chapter.setSubject(subject);
        chapter.setCode(request.code().trim());
        chapter.setTitleFr(request.titleFr().trim());
        chapter.setTitleAr(request.titleAr().trim());
        chapter.setDisplayOrder(request.displayOrder());
        chapters.save(chapter);
        audit(actorId, "CREATE", "chapter", chapter.getId());
        return toView(chapter);
    }

    @Transactional
    public CurriculumDtos.SkillView createSkill(
            UUID actorId, UUID versionId, CurriculumDtos.CreateSkillRequest request) {
        CurriculumVersion version = requireDraftVersion(versionId);
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setCurriculumVersion(version);
        skill.setCode(request.code().trim());
        skill.setTitleFr(request.titleFr().trim());
        skill.setTitleAr(request.titleAr().trim());
        skill.setObjectiveFr(request.objectiveFr().trim());
        skill.setObjectiveAr(request.objectiveAr().trim());
        skills.save(skill);
        audit(actorId, "CREATE", "skill", skill.getId());
        return toView(skill);
    }

    @Transactional
    public void addSkillToChapter(UUID actorId, UUID chapterId, UUID skillId) {
        Chapter chapter = chapters.findById(chapterId)
                .orElseThrow(() -> CurriculumException.notFound("Chapter"));
        Skill skill = skills.findById(skillId)
                .orElseThrow(() -> CurriculumException.notFound("Skill"));
        CurriculumVersion chapterVersion = requireDraftVersion(
                chapter.getSubject().getCurriculumVersion().getId());
        if (!chapterVersion.getId().equals(skill.getCurriculumVersion().getId())) {
            throw CurriculumException.invalid("Chapter and skill must belong to the same curriculum version");
        }
        if (!chapter.getSkills().contains(skill)) {
            chapter.getSkills().add(skill);
            chapters.save(chapter);
            audit(actorId, "MAP", "chapter_skill", chapter.getId());
        }
    }

    @Transactional
    public void addPrerequisite(UUID actorId, UUID skillId, UUID prerequisiteId) {
        Skill skill = skills.findById(skillId).orElseThrow(() -> CurriculumException.notFound("Skill"));
        Skill prerequisite = skills.findById(prerequisiteId)
                .orElseThrow(() -> CurriculumException.notFound("Prerequisite skill"));
        CurriculumVersion version = requireDraftVersion(skill.getCurriculumVersion().getId());
        if (!version.getId().equals(prerequisite.getCurriculumVersion().getId())) {
            throw CurriculumException.invalid("Prerequisite skills must belong to the same curriculum version");
        }
        if (skillId.equals(prerequisiteId)) {
            throw CurriculumException.invalid("A skill cannot be a prerequisite of itself");
        }
        if (skill.getPrerequisites().contains(prerequisite)) {
            return;
        }
        if (hasPath(prerequisite, skillId)) {
            throw CurriculumException.conflict("The prerequisite would create a cycle");
        }
        skill.getPrerequisites().add(prerequisite);
        skills.save(skill);
        audit(actorId, "MAP", "skill_prerequisite", skill.getId());
    }

    @Transactional
    public CurriculumDtos.VersionView publishVersion(UUID actorId, UUID versionId) {
        CurriculumVersion version = versions.findByIdForUpdate(versionId)
                .orElseThrow(() -> CurriculumException.notFound("Curriculum version"));
        if (version.getStatus() != CurriculumVersion.Status.DRAFT) {
            throw CurriculumException.conflict("Only draft versions can be published");
        }
        validateDates(version.getEffectiveFrom(), version.getEffectiveTo());
        curricula.findByIdForUpdate(version.getCurriculum().getId())
                .orElseThrow(() -> CurriculumException.notFound("Curriculum"));

        for (CurriculumVersion existing : versions.findByCurriculum_IdAndStatus(
                version.getCurriculum().getId(), CurriculumVersion.Status.PUBLISHED)) {
            if (!overlaps(version, existing)) {
                continue;
            }
            if (!existing.getEffectiveFrom().isBefore(version.getEffectiveFrom())) {
                throw CurriculumException.conflict(
                        "A published version already covers part of this effective date range");
            }
            existing.setEffectiveTo(version.getEffectiveFrom().minusDays(1));
            versions.save(existing);
            audit(actorId, "CLOSE_VERSION", "curriculum_version", existing.getId());
        }
        version.setStatus(CurriculumVersion.Status.PUBLISHED);
        versions.save(version);
        audit(actorId, "PUBLISH", "curriculum_version", version.getId());
        return toView(version);
    }

    private boolean hasPath(Skill start, UUID targetId) {
        ArrayDeque<Skill> pending = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            Skill current = pending.removeFirst();
            if (targetId.equals(current.getId())) {
                return true;
            }
            if (visited.add(current.getId())) {
                pending.addAll(current.getPrerequisites());
            }
        }
        return false;
    }

    private boolean overlaps(CurriculumVersion left, CurriculumVersion right) {
        boolean leftStartsBeforeRightEnds = right.getEffectiveTo() == null
                || !left.getEffectiveFrom().isAfter(right.getEffectiveTo());
        boolean rightStartsBeforeLeftEnds = left.getEffectiveTo() == null
                || !right.getEffectiveFrom().isAfter(left.getEffectiveTo());
        return leftStartsBeforeRightEnds && rightStartsBeforeLeftEnds;
    }

    private Curriculum requireCurriculum(UUID id) {
        return curricula.findById(id).orElseThrow(() -> CurriculumException.notFound("Curriculum"));
    }

    private Grade requireGrade(UUID id) {
        return grades.findById(id).orElseThrow(() -> CurriculumException.notFound("Grade"));
    }

    private CurriculumVersion requireDraftVersion(UUID id) {
        CurriculumVersion version = versions.findByIdForUpdate(id)
                .orElseThrow(() -> CurriculumException.notFound("Curriculum version"));
        if (version.getStatus() != CurriculumVersion.Status.DRAFT) {
            throw CurriculumException.conflict("Only draft versions can be changed");
        }
        return version;
    }

    private void validateDates(java.time.LocalDate from, java.time.LocalDate to) {
        if (to != null && to.isBefore(from)) {
            throw CurriculumException.invalid("effectiveTo must not be before effectiveFrom");
        }
    }

    private void audit(UUID actorId, String action, String entityType, UUID entityId) {
        CurriculumAuditEvent event = new CurriculumAuditEvent();
        event.setId(UUID.randomUUID());
        event.setActorUserId(actorId);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setOccurredAt(Instant.now());
        auditEvents.save(event);
    }

    private void setNamed(SchoolLevel value, CurriculumDtos.CreateNamedNodeRequest request) {
        value.setCode(request.code().trim());
        value.setNameFr(request.nameFr().trim());
        value.setNameAr(request.nameAr().trim());
        value.setDisplayOrder(request.displayOrder());
    }

    private void setNamed(Grade value, CurriculumDtos.CreateNamedNodeRequest request) {
        value.setCode(request.code().trim());
        value.setNameFr(request.nameFr().trim());
        value.setNameAr(request.nameAr().trim());
        value.setDisplayOrder(request.displayOrder());
    }

    private void setNamed(Specialization value, CurriculumDtos.CreateNamedNodeRequest request) {
        value.setCode(request.code().trim());
        value.setNameFr(request.nameFr().trim());
        value.setNameAr(request.nameAr().trim());
        value.setDisplayOrder(request.displayOrder());
    }

    private void setNamed(Subject value, CurriculumDtos.CreateNamedNodeRequest request) {
        value.setCode(request.code().trim());
        value.setNameFr(request.nameFr().trim());
        value.setNameAr(request.nameAr().trim());
        value.setDisplayOrder(request.displayOrder());
    }

    private CurriculumDtos.VersionView toView(CurriculumVersion value) {
        return new CurriculumDtos.VersionView(
                value.getId(), value.getVersionCode(), value.getEffectiveFrom(), value.getEffectiveTo(),
                value.getSourceReference(), value.getReviewer());
    }

    private CurriculumDtos.NamedNode toView(SchoolLevel value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.NamedNode toView(Grade value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.NamedNode toView(Specialization value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.NamedNode toView(Subject value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.ChapterView toView(Chapter value) {
        return new CurriculumDtos.ChapterView(
                value.getId(), value.getCode(), value.getTitleFr(), value.getTitleAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.SkillView toView(Skill value) {
        List<UUID> prerequisiteIds = value.getPrerequisites().stream()
                .map(Skill::getId)
                .sorted()
                .toList();
        return new CurriculumDtos.SkillView(
                value.getId(), value.getCode(), value.getTitleFr(), value.getTitleAr(),
                value.getObjectiveFr(), value.getObjectiveAr(), prerequisiteIds);
    }
}
