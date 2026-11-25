package tn.mathzero.curriculum.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.curriculum.service.CurriculumAdminService;
import tn.mathzero.curriculum.service.CurriculumImportService;

@RestController
@RequestMapping("/api/v1/admin/curricula")
public class CurriculumAdminController {
    private final CurriculumAdminService curriculumAdminService;
    private final CurriculumImportService curriculumImportService;

    public CurriculumAdminController(
            CurriculumAdminService curriculumAdminService,
            CurriculumImportService curriculumImportService) {
        this.curriculumAdminService = curriculumAdminService;
        this.curriculumImportService = curriculumImportService;
    }

    @PostMapping("/import")
    public CurriculumDtos.ApiResponse<CurriculumDtos.ImportResult> importCurriculum(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @Valid @RequestBody CurriculumDtos.CurriculumImportRequest request) {
        return response(curriculumImportService.importCurriculum(actorId(jwt), request, dryRun));
    }

    @PostMapping
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.CurriculumView>> createCurriculum(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CurriculumDtos.CreateCurriculumRequest request) {
        return created(curriculumAdminService.createCurriculum(actorId(jwt), request));
    }

    @PostMapping("/{curriculumId}/versions")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.VersionView>> createVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID curriculumId,
            @Valid @RequestBody CurriculumDtos.CreateVersionRequest request) {
        return created(curriculumAdminService.createVersion(actorId(jwt), curriculumId, request));
    }

    @PutMapping("/versions/{versionId}/dates")
    public CurriculumDtos.ApiResponse<CurriculumDtos.VersionView> updateVersionDates(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId,
            @Valid @RequestBody CurriculumDtos.UpdateVersionDatesRequest request) {
        return response(curriculumAdminService.updateVersionDates(actorId(jwt), versionId, request));
    }

    @PostMapping("/versions/{versionId}/levels")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.NamedNode>> createLevel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId,
            @Valid @RequestBody CurriculumDtos.CreateNamedNodeRequest request) {
        return created(curriculumAdminService.createLevel(actorId(jwt), versionId, request));
    }

    @PostMapping("/levels/{levelId}/grades")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.NamedNode>> createGrade(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID levelId,
            @Valid @RequestBody CurriculumDtos.CreateNamedNodeRequest request) {
        return created(curriculumAdminService.createGrade(actorId(jwt), levelId, request));
    }

    @PostMapping("/versions/{versionId}/specializations")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.NamedNode>> createSpecialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId,
            @Valid @RequestBody CurriculumDtos.CreateNamedNodeRequest request) {
        return created(curriculumAdminService.createSpecialization(actorId(jwt), versionId, request));
    }

    @PutMapping("/grades/{gradeId}/specializations/{specializationId}")
    public ResponseEntity<Void> addSpecialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID gradeId,
            @PathVariable UUID specializationId) {
        curriculumAdminService.addSpecialization(actorId(jwt), gradeId, specializationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/versions/{versionId}/subjects")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.NamedNode>> createSubject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId,
            @Valid @RequestBody CurriculumDtos.CreateNamedNodeRequest request) {
        return created(curriculumAdminService.createSubject(actorId(jwt), versionId, request));
    }

    @PutMapping("/grades/{gradeId}/subjects/{subjectId}")
    public ResponseEntity<Void> addSubject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID gradeId,
            @PathVariable UUID subjectId) {
        curriculumAdminService.addSubject(actorId(jwt), gradeId, subjectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subjects/{subjectId}/chapters")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.ChapterView>> createChapter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID subjectId,
            @Valid @RequestBody CurriculumDtos.CreateChapterRequest request) {
        return created(curriculumAdminService.createChapter(actorId(jwt), subjectId, request));
    }

    @PostMapping("/versions/{versionId}/skills")
    public ResponseEntity<CurriculumDtos.ApiResponse<CurriculumDtos.SkillView>> createSkill(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId,
            @Valid @RequestBody CurriculumDtos.CreateSkillRequest request) {
        return created(curriculumAdminService.createSkill(actorId(jwt), versionId, request));
    }

    @PutMapping("/chapters/{chapterId}/skills/{skillId}")
    public ResponseEntity<Void> addSkillToChapter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID chapterId,
            @PathVariable UUID skillId) {
        curriculumAdminService.addSkillToChapter(actorId(jwt), chapterId, skillId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/skills/{skillId}/prerequisites/{prerequisiteId}")
    public ResponseEntity<Void> addPrerequisite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID skillId,
            @PathVariable UUID prerequisiteId) {
        curriculumAdminService.addPrerequisite(actorId(jwt), skillId, prerequisiteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/versions/{versionId}/publish")
    public CurriculumDtos.ApiResponse<CurriculumDtos.VersionView> publishVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID versionId) {
        return response(curriculumAdminService.publishVersion(actorId(jwt), versionId));
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static <T> ResponseEntity<CurriculumDtos.ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(data));
    }

    private static <T> CurriculumDtos.ApiResponse<T> response(T data) {
        return new CurriculumDtos.ApiResponse<>(data, new CurriculumDtos.Meta(UUID.randomUUID().toString()));
    }
}
