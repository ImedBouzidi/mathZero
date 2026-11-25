package tn.mathzero.curriculum.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.mathzero.curriculum.api.CurriculumDtos;
import tn.mathzero.curriculum.domain.Chapter;
import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumVersion;
import tn.mathzero.curriculum.domain.Grade;
import tn.mathzero.curriculum.domain.SchoolLevel;
import tn.mathzero.curriculum.domain.Skill;
import tn.mathzero.curriculum.domain.Subject;
import tn.mathzero.curriculum.repository.ChapterRepository;
import tn.mathzero.curriculum.repository.CurriculumRepository;
import tn.mathzero.curriculum.repository.CurriculumVersionRepository;
import tn.mathzero.curriculum.repository.GradeRepository;
import tn.mathzero.curriculum.repository.SchoolLevelRepository;
import tn.mathzero.curriculum.repository.SkillRepository;
import tn.mathzero.curriculum.repository.SpecializationRepository;
import tn.mathzero.curriculum.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class CurriculumReadService {
    private static final int MAX_PAGE_SIZE = 100;

    private final CurriculumRepository curricula;
    private final CurriculumVersionRepository versions;
    private final SchoolLevelRepository levels;
    private final GradeRepository grades;
    private final SpecializationRepository specializations;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;
    private final SkillRepository skills;

    public CurriculumReadService(
            CurriculumRepository curricula,
            CurriculumVersionRepository versions,
            SchoolLevelRepository levels,
            GradeRepository grades,
            SpecializationRepository specializations,
            SubjectRepository subjects,
            ChapterRepository chapters,
            SkillRepository skills) {
        this.curricula = curricula;
        this.versions = versions;
        this.levels = levels;
        this.grades = grades;
        this.specializations = specializations;
        this.subjects = subjects;
        this.chapters = chapters;
        this.skills = skills;
    }

    public CurriculumDtos.PageData<CurriculumDtos.CurriculumView> curricula(
            String search, LocalDate asOf, int page, int size) {
        LocalDate effectiveDate = asOf == null ? LocalDate.now(ZoneOffset.UTC) : asOf;
        Page<Curriculum> result = curricula.findPublishedOn(
                CurriculumVersion.Status.PUBLISHED,
                effectiveDate,
                normalizeSearch(search),
                pageRequest(page, size, Sort.by("code").ascending()));
        return CurriculumDtos.PageData.from(result.map(curriculum -> toView(curriculum, effectiveDate)));
    }

    public CurriculumDtos.PageData<CurriculumDtos.VersionView> versions(
            String curriculumCode, int page, int size) {
        if (curricula.findByCodeIgnoreCase(curriculumCode).isEmpty()) {
            throw CurriculumException.notFound("Curriculum");
        }
        Page<CurriculumVersion> result = versions
                .findByCurriculum_CodeIgnoreCaseAndStatusOrderByEffectiveFromDesc(
                        curriculumCode,
                        CurriculumVersion.Status.PUBLISHED,
                        pageRequest(page, size, Sort.unsorted()));
        return CurriculumDtos.PageData.from(result.map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.NamedNode> levels(UUID versionId, int page, int size) {
        requirePublishedVersion(versionId);
        return CurriculumDtos.PageData.from(levels
                .findByVersion_IdAndVersion_StatusOrderByDisplayOrderAsc(
                        versionId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.NamedNode> grades(UUID levelId, int page, int size) {
        SchoolLevel level = levels.findById(levelId).orElseThrow(() -> CurriculumException.notFound("School level"));
        requirePublishedVersion(level.getVersion().getId());
        return CurriculumDtos.PageData.from(grades
                .findBySchoolLevel_IdAndSchoolLevel_Version_StatusOrderByDisplayOrderAsc(
                        levelId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.NamedNode> specializations(
            UUID gradeId, int page, int size) {
        requirePublishedGrade(gradeId);
        return CurriculumDtos.PageData.from(specializations
                .findDistinctByGrades_IdAndCurriculumVersion_StatusOrderByDisplayOrderAsc(
                        gradeId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.NamedNode> subjects(UUID gradeId, int page, int size) {
        requirePublishedGrade(gradeId);
        return CurriculumDtos.PageData.from(subjects
                .findDistinctByGrades_IdAndCurriculumVersion_StatusOrderByDisplayOrderAsc(
                        gradeId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.ChapterView> chapters(
            UUID subjectId, int page, int size) {
        Subject subject = subjects.findById(subjectId).orElseThrow(() -> CurriculumException.notFound("Subject"));
        requirePublishedVersion(subject.getCurriculumVersion().getId());
        return CurriculumDtos.PageData.from(chapters
                .findBySubject_IdAndSubject_CurriculumVersion_StatusOrderByDisplayOrderAsc(
                        subjectId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.SkillView> skills(UUID chapterId, int page, int size) {
        Chapter chapter = chapters.findById(chapterId).orElseThrow(() -> CurriculumException.notFound("Chapter"));
        requirePublishedVersion(chapter.getSubject().getCurriculumVersion().getId());
        return CurriculumDtos.PageData.from(skills
                .findDistinctByChapters_IdAndCurriculumVersion_StatusOrderByCodeAsc(
                        chapterId, CurriculumVersion.Status.PUBLISHED, pageRequest(page, size, Sort.unsorted()))
                .map(this::toView));
    }

    public CurriculumDtos.PageData<CurriculumDtos.SkillView> prerequisites(
            UUID skillId, int page, int size) {
        Skill skill = skills.findById(skillId).orElseThrow(() -> CurriculumException.notFound("Skill"));
        requirePublishedVersion(skill.getCurriculumVersion().getId());
        List<Skill> matching = skill.getPrerequisites().stream()
                .sorted(Comparator.comparing(Skill::getCode))
                .toList();
        Pageable pageable = pageRequest(page, size, Sort.unsorted());
        int start = Math.min(Math.toIntExact(pageable.getOffset()), matching.size());
        int end = Math.min(start + pageable.getPageSize(), matching.size());
        List<CurriculumDtos.SkillView> items = matching.subList(start, end).stream()
                .map(this::toView)
                .toList();
        return new CurriculumDtos.PageData<>(
                items,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                matching.size(),
                (int) Math.ceil((double) matching.size() / pageable.getPageSize()));
    }

    private void requirePublishedGrade(UUID gradeId) {
        Grade grade = grades.findById(gradeId).orElseThrow(() -> CurriculumException.notFound("Grade"));
        requirePublishedVersion(grade.getSchoolLevel().getVersion().getId());
    }

    private void requirePublishedVersion(UUID versionId) {
        versions.findByIdAndStatus(versionId, CurriculumVersion.Status.PUBLISHED)
                .orElseThrow(() -> CurriculumException.notFound("Published curriculum version"));
    }

    private CurriculumDtos.CurriculumView toView(Curriculum curriculum, LocalDate asOf) {
        CurriculumVersion current = versions.findByCurriculum_IdAndStatus(
                        curriculum.getId(), CurriculumVersion.Status.PUBLISHED).stream()
                .filter(version -> !version.getEffectiveFrom().isAfter(asOf))
                .filter(version -> version.getEffectiveTo() == null || !version.getEffectiveTo().isBefore(asOf))
                .max(Comparator.comparing(CurriculumVersion::getEffectiveFrom))
                .orElseThrow(() -> CurriculumException.notFound("Effective curriculum version"));
        return new CurriculumDtos.CurriculumView(
                curriculum.getId(), curriculum.getCode(), curriculum.getNameFr(), curriculum.getNameAr(),
                toView(current));
    }

    private CurriculumDtos.VersionView toView(CurriculumVersion version) {
        return new CurriculumDtos.VersionView(
                version.getId(), version.getVersionCode(), version.getEffectiveFrom(), version.getEffectiveTo(),
                version.getSourceReference(), version.getReviewer());
    }

    private CurriculumDtos.NamedNode toView(SchoolLevel value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.NamedNode toView(Grade value) {
        return new CurriculumDtos.NamedNode(
                value.getId(), value.getCode(), value.getNameFr(), value.getNameAr(), value.getDisplayOrder());
    }

    private CurriculumDtos.NamedNode toView(tn.mathzero.curriculum.domain.Specialization value) {
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

    private Pageable pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw CurriculumException.invalid("page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, sort);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }
}
