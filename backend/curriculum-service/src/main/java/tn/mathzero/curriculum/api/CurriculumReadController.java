package tn.mathzero.curriculum.api;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.curriculum.service.CurriculumReadService;

@RestController
@RequestMapping("/api/v1")
public class CurriculumReadController {
    private final CurriculumReadService curriculumReadService;

    public CurriculumReadController(CurriculumReadService curriculumReadService) {
        this.curriculumReadService = curriculumReadService;
    }

    @GetMapping("/curricula")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.CurriculumView>> curricula(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.curricula(search, asOf, page, size));
    }

    @GetMapping("/curricula/{code}/versions")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.VersionView>> versions(
            @PathVariable String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.versions(code, page, size));
    }

    @GetMapping("/versions/{versionId}/levels")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.NamedNode>> levels(
            @PathVariable UUID versionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.levels(versionId, page, size));
    }

    @GetMapping("/levels/{levelId}/grades")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.NamedNode>> grades(
            @PathVariable UUID levelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.grades(levelId, page, size));
    }

    @GetMapping("/grades/{gradeId}/specializations")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.NamedNode>> specializations(
            @PathVariable UUID gradeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.specializations(gradeId, page, size));
    }

    @GetMapping("/grades/{gradeId}/subjects")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.NamedNode>> subjects(
            @PathVariable UUID gradeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.subjects(gradeId, page, size));
    }

    @GetMapping("/subjects/{subjectId}/chapters")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.ChapterView>> chapters(
            @PathVariable UUID subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.chapters(subjectId, page, size));
    }

    @GetMapping("/chapters/{chapterId}/skills")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.SkillView>> skills(
            @PathVariable UUID chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.skills(chapterId, page, size));
    }

    @GetMapping("/skills/{skillId}/prerequisites")
    public CurriculumDtos.ApiResponse<CurriculumDtos.PageData<CurriculumDtos.SkillView>> prerequisites(
            @PathVariable UUID skillId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return response(curriculumReadService.prerequisites(skillId, page, size));
    }

    private static <T> CurriculumDtos.ApiResponse<T> response(T data) {
        return new CurriculumDtos.ApiResponse<>(data, new CurriculumDtos.Meta(UUID.randomUUID().toString()));
    }
}
