package tn.mathzero.curriculum.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

public final class CurriculumDtos {
    private CurriculumDtos() {}

    public record ApiResponse<T>(T data, Meta meta) {}
    public record Meta(String requestId) {}
    public record ApiError(String code, String message, String requestId) {}

    public record PageData<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
        public static <T> PageData<T> from(Page<T> source) {
            return new PageData<>(
                    source.getContent(),
                    source.getNumber(),
                    source.getSize(),
                    source.getTotalElements(),
                    source.getTotalPages());
        }
    }

    public record VersionView(
            UUID id,
            String code,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String sourceReference,
            String reviewer) {}

    public record CurriculumView(
            UUID id,
            String code,
            String nameFr,
            String nameAr,
            VersionView currentVersion) {}

    public record NamedNode(
            UUID id,
            String code,
            String nameFr,
            String nameAr,
            int displayOrder) {}

    public record ChapterView(
            UUID id,
            String code,
            String titleFr,
            String titleAr,
            int displayOrder) {}

    public record SkillView(
            UUID id,
            String code,
            String titleFr,
            String titleAr,
            String objectiveFr,
            String objectiveAr,
            List<UUID> prerequisites) {}

    public record CreateCurriculumRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr) {}

    public record CreateVersionRequest(
            @NotBlank @Size(max = 80) String code,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo) {}

    public record UpdateVersionDatesRequest(
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo) {}

    public record CreateNamedNodeRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @Min(0) int displayOrder) {}

    public record CreateChapterRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String titleFr,
            @NotBlank @Size(max = 180) String titleAr,
            @Min(0) int displayOrder) {}

    public record CreateSkillRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 240) String titleFr,
            @NotBlank @Size(max = 240) String titleAr,
            @NotBlank String objectiveFr,
            @NotBlank String objectiveAr) {}

    public record AuditView(
            UUID id,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Instant occurredAt) {}

    /**
     * Versioned bulk-import document. Every node is upserted by its natural key
     * (curriculum code, version code and node code), which makes re-running the
     * same file safe. A version that is already PUBLISHED is never mutated;
     * publish a new version code instead.
     */
    public record CurriculumImportRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @NotBlank @Size(max = 80) String versionCode,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @Size(max = 512) String sourceReference,
            @Size(max = 180) String reviewer,
            boolean publish,
            @Valid List<ImportLevel> levels,
            @Valid List<ImportSpecialization> specializations,
            @Valid List<ImportSubject> subjects,
            @Valid List<ImportSkill> skills) {}

    public record ImportLevel(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @Min(0) int displayOrder,
            @Valid List<ImportGrade> grades) {}

    public record ImportGrade(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @Min(0) int displayOrder,
            List<@NotBlank @Size(max = 80) String> specializationCodes,
            List<@NotBlank @Size(max = 80) String> subjectCodes) {}

    public record ImportSpecialization(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @Min(0) int displayOrder) {}

    public record ImportSubject(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String nameFr,
            @NotBlank @Size(max = 180) String nameAr,
            @Min(0) int displayOrder,
            @Valid List<ImportChapter> chapters) {}

    public record ImportChapter(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 180) String titleFr,
            @NotBlank @Size(max = 180) String titleAr,
            @Min(0) int displayOrder,
            List<@NotBlank @Size(max = 80) String> skillCodes) {}

    public record ImportSkill(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 240) String titleFr,
            @NotBlank @Size(max = 240) String titleAr,
            @NotBlank String objectiveFr,
            @NotBlank String objectiveAr,
            List<@NotBlank @Size(max = 80) String> prerequisiteCodes) {}

    public record ImportResult(
            boolean dryRun,
            boolean valid,
            boolean published,
            UUID curriculumId,
            UUID versionId,
            String curriculumCode,
            String versionCode,
            int levels,
            int grades,
            int specializations,
            int subjects,
            int chapters,
            int skills,
            List<String> errors,
            List<String> warnings) {}
}
