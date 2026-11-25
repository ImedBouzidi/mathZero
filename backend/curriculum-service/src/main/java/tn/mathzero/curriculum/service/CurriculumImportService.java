package tn.mathzero.curriculum.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * Idempotent bulk import for official curriculum documents.
 *
 * <p>All nodes are upserted by natural key. A DRAFT version can be re-imported
 * as many times as needed; a PUBLISHED version is immutable and must be
 * superseded by a new version code. Mappings (grade → subject,
 * chapter → skill, skill → prerequisite) are additive, so removing an entry
 * from the document never deletes reviewed data implicitly.</p>
 */
@Service
public class CurriculumImportService {
    private final CurriculumRepository curricula;
    private final CurriculumVersionRepository versions;
    private final SchoolLevelRepository levels;
    private final GradeRepository grades;
    private final SpecializationRepository specializations;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final SkillRepository skills;
    private final CurriculumAuditEventRepository auditEvents;
    private final CurriculumAdminService adminService;

    public CurriculumImportService(
            CurriculumRepository curricula,
            CurriculumVersionRepository versions,
            SchoolLevelRepository levels,
            GradeRepository grades,
            SpecializationRepository specializations,
            SubjectRepository subjects,
            ChapterRepository chapters,
            SkillRepository skills,
            CurriculumAuditEventRepository auditEvents,
            CurriculumAdminService adminService) {
        this.curricula = curricula;
        this.versions = versions;
        this.levels = levels;
        this.grades = grades;
        this.specializations = specializations;
        this.subjects = subjects;
        this.chapters = chapters;
        this.skills = skills;
        this.auditEvents = auditEvents;
        this.adminService = adminService;
    }

    @Transactional
    public CurriculumDtos.ImportResult importCurriculum(
            UUID actorId,
            CurriculumDtos.CurriculumImportRequest request,
            boolean dryRun) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        validate(request, errors, warnings);

        Curriculum curriculum = curricula.findByCodeIgnoreCase(request.code().trim()).orElse(null);
        CurriculumVersion version = curriculum == null
                ? null
                : versions.findByCurriculum_IdAndVersionCodeIgnoreCase(
                        curriculum.getId(), request.versionCode().trim()).orElse(null);

        if (version != null && version.getStatus() != CurriculumVersion.Status.DRAFT) {
            errors.add("Version " + request.versionCode() + " is already " + version.getStatus()
                    + "; create a new version code instead of overwriting published data");
        }

        if (!errors.isEmpty()) {
            if (dryRun) {
                return result(true, false, false, curriculum, version, request, errors, warnings);
            }
            throw CurriculumException.invalid(
                    "Import validation failed: " + String.join("; ", errors));
        }

        if (dryRun) {
            return result(true, true, false, curriculum, version, request, List.of(), warnings);
        }

        if (curriculum == null) {
            curriculum = new Curriculum();
            curriculum.setId(UUID.randomUUID());
            curriculum.setCode(request.code().trim());
        }
        curriculum.setNameFr(request.nameFr().trim());
        curriculum.setNameAr(request.nameAr().trim());
        curricula.save(curriculum);

        if (version == null) {
            version = new CurriculumVersion();
            version.setId(UUID.randomUUID());
            version.setCurriculum(curriculum);
            version.setVersionCode(request.versionCode().trim());
            version.setStatus(CurriculumVersion.Status.DRAFT);
        }
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        version.setSourceReference(trimToNull(request.sourceReference()));
        version.setReviewer(trimToNull(request.reviewer()));
        versions.save(version);

        Map<String, Specialization> specializationsByCode = upsertSpecializations(version, request);
        Map<String, Subject> subjectsByCode = upsertSubjects(version, request);
        Map<String, Chapter> chaptersByKey = upsertChapters(request, subjectsByCode);
        Map<String, Skill> skillsByCode = upsertSkills(version, request);
        upsertLevelsAndGrades(version, request, specializationsByCode, subjectsByCode);
        mapChapterSkills(request, chaptersByKey, skillsByCode);
        mapPrerequisites(request, skillsByCode);

        audit(actorId, "IMPORT", "curriculum_version", version.getId());
        if (request.publish()) {
            adminService.publishVersion(actorId, version.getId());
        }

        return result(false, true, request.publish(), curriculum, version, request, List.of(), warnings);
    }

    private Map<String, Specialization> upsertSpecializations(
            CurriculumVersion version, CurriculumDtos.CurriculumImportRequest request) {
        Map<String, Specialization> result = new LinkedHashMap<>();
        for (CurriculumDtos.ImportSpecialization node : safe(request.specializations())) {
            String code = node.code().trim();
            Specialization entity = specializations
                    .findByCurriculumVersion_IdAndCodeIgnoreCase(version.getId(), code)
                    .orElseGet(() -> {
                        Specialization created = new Specialization();
                        created.setId(UUID.randomUUID());
                        created.setCurriculumVersion(version);
                        created.setCode(code);
                        return created;
                    });
            entity.setNameFr(node.nameFr().trim());
            entity.setNameAr(node.nameAr().trim());
            entity.setDisplayOrder(node.displayOrder());
            specializations.save(entity);
            result.put(normalize(code), entity);
        }
        return result;
    }

    private Map<String, Subject> upsertSubjects(
            CurriculumVersion version, CurriculumDtos.CurriculumImportRequest request) {
        Map<String, Subject> result = new LinkedHashMap<>();
        for (CurriculumDtos.ImportSubject node : safe(request.subjects())) {
            String code = node.code().trim();
            Subject entity = subjects
                    .findByCurriculumVersion_IdAndCodeIgnoreCase(version.getId(), code)
                    .orElseGet(() -> {
                        Subject created = new Subject();
                        created.setId(UUID.randomUUID());
                        created.setCurriculumVersion(version);
                        created.setCode(code);
                        return created;
                    });
            entity.setNameFr(node.nameFr().trim());
            entity.setNameAr(node.nameAr().trim());
            entity.setDisplayOrder(node.displayOrder());
            subjects.save(entity);
            result.put(normalize(code), entity);
        }
        return result;
    }

    private Map<String, Chapter> upsertChapters(
            CurriculumDtos.CurriculumImportRequest request,
            Map<String, Subject> subjectsByCode) {
        Map<String, Chapter> result = new LinkedHashMap<>();
        for (CurriculumDtos.ImportSubject subjectNode : safe(request.subjects())) {
            Subject subject = subjectsByCode.get(normalize(subjectNode.code()));
            if (subject == null) {
                continue;
            }
            for (CurriculumDtos.ImportChapter node : safe(subjectNode.chapters())) {
                String code = node.code().trim();
                Chapter entity = chapters.findBySubject_IdAndCodeIgnoreCase(subject.getId(), code)
                        .orElseGet(() -> {
                            Chapter created = new Chapter();
                            created.setId(UUID.randomUUID());
                            created.setSubject(subject);
                            created.setCode(code);
                            return created;
                        });
                entity.setTitleFr(node.titleFr().trim());
                entity.setTitleAr(node.titleAr().trim());
                entity.setDisplayOrder(node.displayOrder());
                chapters.save(entity);
                result.put(chapterKey(subjectNode.code(), code), entity);
            }
        }
        return result;
    }

    private Map<String, Skill> upsertSkills(
            CurriculumVersion version, CurriculumDtos.CurriculumImportRequest request) {
        Map<String, Skill> result = new LinkedHashMap<>();
        for (CurriculumDtos.ImportSkill node : safe(request.skills())) {
            String code = node.code().trim();
            Skill entity = skills
                    .findByCurriculumVersion_IdAndCodeIgnoreCase(version.getId(), code)
                    .orElseGet(() -> {
                        Skill created = new Skill();
                        created.setId(UUID.randomUUID());
                        created.setCurriculumVersion(version);
                        created.setCode(code);
                        return created;
                    });
            entity.setTitleFr(node.titleFr().trim());
            entity.setTitleAr(node.titleAr().trim());
            entity.setObjectiveFr(node.objectiveFr().trim());
            entity.setObjectiveAr(node.objectiveAr().trim());
            skills.save(entity);
            result.put(normalize(code), entity);
        }
        return result;
    }

    private void upsertLevelsAndGrades(
            CurriculumVersion version,
            CurriculumDtos.CurriculumImportRequest request,
            Map<String, Specialization> specializationsByCode,
            Map<String, Subject> subjectsByCode) {
        for (CurriculumDtos.ImportLevel levelNode : safe(request.levels())) {
            String levelCode = levelNode.code().trim();
            SchoolLevel level = levels
                    .findByVersion_IdAndCodeIgnoreCase(version.getId(), levelCode)
                    .orElseGet(() -> {
                        SchoolLevel created = new SchoolLevel();
                        created.setId(UUID.randomUUID());
                        created.setVersion(version);
                        created.setCode(levelCode);
                        return created;
                    });
            level.setNameFr(levelNode.nameFr().trim());
            level.setNameAr(levelNode.nameAr().trim());
            level.setDisplayOrder(levelNode.displayOrder());
            levels.save(level);

            for (CurriculumDtos.ImportGrade gradeNode : safe(levelNode.grades())) {
                String gradeCode = gradeNode.code().trim();
                Grade grade = grades.findBySchoolLevel_IdAndCodeIgnoreCase(level.getId(), gradeCode)
                        .orElseGet(() -> {
                            Grade created = new Grade();
                            created.setId(UUID.randomUUID());
                            created.setSchoolLevel(level);
                            created.setCode(gradeCode);
                            return created;
                        });
                grade.setNameFr(gradeNode.nameFr().trim());
                grade.setNameAr(gradeNode.nameAr().trim());
                grade.setDisplayOrder(gradeNode.displayOrder());

                for (String specializationCode : safe(gradeNode.specializationCodes())) {
                    Specialization specialization = specializationsByCode.get(normalize(specializationCode));
                    if (specialization != null) {
                        grade.getSpecializations().add(specialization);
                    }
                }
                for (String subjectCode : safe(gradeNode.subjectCodes())) {
                    Subject subject = subjectsByCode.get(normalize(subjectCode));
                    if (subject != null) {
                        grade.getSubjects().add(subject);
                    }
                }
                grades.save(grade);
            }
        }
    }

    private void mapChapterSkills(
            CurriculumDtos.CurriculumImportRequest request,
            Map<String, Chapter> chaptersByKey,
            Map<String, Skill> skillsByCode) {
        for (CurriculumDtos.ImportSubject subjectNode : safe(request.subjects())) {
            for (CurriculumDtos.ImportChapter chapterNode : safe(subjectNode.chapters())) {
                Chapter chapter = chaptersByKey.get(chapterKey(subjectNode.code(), chapterNode.code()));
                if (chapter == null) {
                    continue;
                }
                for (String skillCode : safe(chapterNode.skillCodes())) {
                    Skill skill = skillsByCode.get(normalize(skillCode));
                    if (skill != null && !chapter.getSkills().contains(skill)) {
                        chapter.getSkills().add(skill);
                    }
                }
                chapters.save(chapter);
            }
        }
    }

    private void mapPrerequisites(
            CurriculumDtos.CurriculumImportRequest request,
            Map<String, Skill> skillsByCode) {
        for (CurriculumDtos.ImportSkill skillNode : safe(request.skills())) {
            Skill skill = skillsByCode.get(normalize(skillNode.code()));
            if (skill == null) {
                continue;
            }
            for (String prerequisiteCode : safe(skillNode.prerequisiteCodes())) {
                Skill prerequisite = skillsByCode.get(normalize(prerequisiteCode));
                if (prerequisite != null && !skill.getPrerequisites().contains(prerequisite)) {
                    skill.getPrerequisites().add(prerequisite);
                }
            }
            skills.save(skill);
        }
    }

    private void validate(
            CurriculumDtos.CurriculumImportRequest request,
            List<String> errors,
            List<String> warnings) {
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            errors.add("effectiveTo must not be before effectiveFrom");
        }

        Set<String> specializationCodes = new HashSet<>();
        for (CurriculumDtos.ImportSpecialization node : safe(request.specializations())) {
            if (!specializationCodes.add(normalize(node.code()))) {
                errors.add("Duplicate specialization code: " + node.code());
            }
        }

        Set<String> subjectCodes = new HashSet<>();
        for (CurriculumDtos.ImportSubject node : safe(request.subjects())) {
            if (!subjectCodes.add(normalize(node.code()))) {
                errors.add("Duplicate subject code: " + node.code());
            }
            Set<String> chapterCodes = new HashSet<>();
            for (CurriculumDtos.ImportChapter chapter : safe(node.chapters())) {
                if (!chapterCodes.add(normalize(chapter.code()))) {
                    errors.add("Duplicate chapter code " + chapter.code()
                            + " in subject " + node.code());
                }
            }
        }

        Set<String> skillCodes = new HashSet<>();
        for (CurriculumDtos.ImportSkill node : safe(request.skills())) {
            if (!skillCodes.add(normalize(node.code()))) {
                errors.add("Duplicate skill code: " + node.code());
            }
        }

        Set<String> levelCodes = new HashSet<>();
        for (CurriculumDtos.ImportLevel level : safe(request.levels())) {
            if (!levelCodes.add(normalize(level.code()))) {
                errors.add("Duplicate level code: " + level.code());
            }
            Set<String> gradeCodes = new HashSet<>();
            for (CurriculumDtos.ImportGrade grade : safe(level.grades())) {
                if (!gradeCodes.add(normalize(grade.code()))) {
                    errors.add("Duplicate grade code " + grade.code()
                            + " in level " + level.code());
                }
                for (String specializationCode : safe(grade.specializationCodes())) {
                    if (!specializationCodes.contains(normalize(specializationCode))) {
                        errors.add("Grade " + grade.code()
                                + " references unknown specialization " + specializationCode);
                    }
                }
                for (String subjectCode : safe(grade.subjectCodes())) {
                    if (!subjectCodes.contains(normalize(subjectCode))) {
                        errors.add("Grade " + grade.code()
                                + " references unknown subject " + subjectCode);
                    }
                }
            }
        }

        for (CurriculumDtos.ImportSubject subject : safe(request.subjects())) {
            for (CurriculumDtos.ImportChapter chapter : safe(subject.chapters())) {
                for (String skillCode : safe(chapter.skillCodes())) {
                    if (!skillCodes.contains(normalize(skillCode))) {
                        errors.add("Chapter " + chapter.code()
                                + " references unknown skill " + skillCode);
                    }
                }
            }
        }

        Map<String, List<String>> prerequisiteGraph = new HashMap<>();
        for (CurriculumDtos.ImportSkill skill : safe(request.skills())) {
            List<String> prerequisites = new ArrayList<>();
            for (String prerequisiteCode : safe(skill.prerequisiteCodes())) {
                if (normalize(prerequisiteCode).equals(normalize(skill.code()))) {
                    errors.add("Skill " + skill.code() + " cannot be its own prerequisite");
                } else if (!skillCodes.contains(normalize(prerequisiteCode))) {
                    errors.add("Skill " + skill.code()
                            + " references unknown prerequisite " + prerequisiteCode);
                } else {
                    prerequisites.add(normalize(prerequisiteCode));
                }
            }
            prerequisiteGraph.put(normalize(skill.code()), prerequisites);
        }
        if (hasCycle(prerequisiteGraph)) {
            errors.add("Skill prerequisites contain a cycle");
        }

        if (safe(request.levels()).isEmpty()
                && safe(request.subjects()).isEmpty()
                && safe(request.skills()).isEmpty()) {
            warnings.add("The import document contains no levels, subjects or skills");
        }
    }

    private boolean hasCycle(Map<String, List<String>> graph) {
        Map<String, Integer> state = new HashMap<>();
        for (String node : graph.keySet()) {
            if (visit(node, graph, state)) {
                return true;
            }
        }
        return false;
    }

    private boolean visit(String node, Map<String, List<String>> graph, Map<String, Integer> state) {
        Integer current = state.get(node);
        if (current != null) {
            return current == 1;
        }
        state.put(node, 1);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (visit(next, graph, state)) {
                return true;
            }
        }
        state.put(node, 2);
        return false;
    }

    private CurriculumDtos.ImportResult result(
            boolean dryRun,
            boolean valid,
            boolean published,
            Curriculum curriculum,
            CurriculumVersion version,
            CurriculumDtos.CurriculumImportRequest request,
            List<String> errors,
            List<String> warnings) {
        int gradeCount = 0;
        for (CurriculumDtos.ImportLevel level : safe(request.levels())) {
            gradeCount += safe(level.grades()).size();
        }
        int chapterCount = 0;
        for (CurriculumDtos.ImportSubject subject : safe(request.subjects())) {
            chapterCount += safe(subject.chapters()).size();
        }
        return new CurriculumDtos.ImportResult(
                dryRun,
                valid,
                published,
                curriculum == null ? null : curriculum.getId(),
                version == null ? null : version.getId(),
                request.code(),
                request.versionCode(),
                safe(request.levels()).size(),
                gradeCount,
                safe(request.specializations()).size(),
                safe(request.subjects()).size(),
                chapterCount,
                safe(request.skills()).size(),
                List.copyOf(errors),
                List.copyOf(warnings));
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

    private static String chapterKey(String subjectCode, String chapterCode) {
        return normalize(subjectCode) + "|" + normalize(chapterCode);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
