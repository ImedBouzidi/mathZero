package tn.mathzero.resource.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.resource.service.LearningContentService;
import tn.mathzero.resource.service.CurriculumCatalogService;

/** Public animated lesson and quiz API for approved resources. */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {
    private final LearningContentService learningContent;
    private final CurriculumCatalogService curriculumCatalog;

    public LearningController(
            LearningContentService learningContent,
            CurriculumCatalogService curriculumCatalog) {
        this.learningContent = learningContent;
        this.curriculumCatalog = curriculumCatalog;
    }

    @GetMapping("/lessons/{resourceId}")
    public ResourceDtos.ApiResponse<LearningDtos.LessonView> lesson(@PathVariable UUID resourceId) {
        return response(learningContent.lesson(resourceId));
    }

    @PostMapping("/lessons/{resourceId}/answers")
    public ResourceDtos.ApiResponse<LearningDtos.AnswerResult> answer(
            @PathVariable UUID resourceId,
            @Valid @RequestBody LearningDtos.AnswerRequest request) {
        return response(learningContent.answer(resourceId, request));
    }

    @GetMapping("/curriculum/cycles")
    public ResourceDtos.ApiResponse<List<LearningDtos.CurriculumCycleView>> curriculumCycles() {
        return response(curriculumCatalog.cycles());
    }

    @GetMapping("/curriculum/grades/{gradeCode}")
    public ResourceDtos.ApiResponse<LearningDtos.CurriculumGradeView> curriculumGrade(
            @PathVariable String gradeCode) {
        return response(curriculumCatalog.grade(gradeCode));
    }

    @PostMapping("/curriculum/grades/{gradeCode}/answers")
    public ResourceDtos.ApiResponse<LearningDtos.CatalogAnswerResult> answerCatalogExercise(
            @PathVariable String gradeCode,
            @Valid @RequestBody LearningDtos.CatalogAnswerRequest request) {
        return response(curriculumCatalog.answer(gradeCode, request));
    }

    private static <T> ResourceDtos.ApiResponse<T> response(T data) {
        return new ResourceDtos.ApiResponse<>(data, new ResourceDtos.Meta(UUID.randomUUID().toString()));
    }
}
