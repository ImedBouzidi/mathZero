package tn.mathzero.resource.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Public learning views. Correct answers are never part of a lesson response. */
public final class LearningDtos {
    private LearningDtos() {}

    public record StepView(
            String kind,
            String title,
            String body,
            String formula) {}

    public record DemoFrameView(
            String label,
            String narration,
            String formula,
            String left,
            String right,
            int progress) {}

    /** Replayable worked demonstration. Contains no hidden quiz answer. */
    public record DemoView(
            String kind,
            String title,
            List<DemoFrameView> frames) {}

    public record ExerciseView(
            String id,
            String prompt,
            List<String> options,
            String hint) {}

    public record LessonView(
            UUID resourceId,
            String title,
            String summary,
            String language,
            String cycle,
            String sourceUrl,
            String licenceName,
            String attributionText,
            List<StepView> steps,
            DemoView demo,
            List<ExerciseView> exercises) {}

    public record AnswerRequest(
            @NotBlank String questionId,
            @NotNull @Min(0) @Max(5) Integer optionIndex) {}

    public record AnswerResult(
            boolean correct,
            String correctAnswer,
            String explanation,
            int position,
            int total,
            DemoView solution) {}

    public record CatalogExerciseView(
            String id,
            String prompt,
            List<String> options,
            String hint,
            String promptAr,
            List<String> optionsAr,
            String hintAr) {}

    public record CatalogChapterView(
            String id,
            String title,
            String description,
            String titleAr,
            String descriptionAr,
            List<StepView> steps,
            List<CatalogExerciseView> exercises) {}

    public record CurriculumGradeView(
            String gradeCode,
            String gradeName,
            String gradeNameAr,
            String cycle,
            boolean draft,
            String reviewNote,
            List<CatalogChapterView> chapters) {}

    public record CurriculumCycleView(
            String cycle,
            String nameFr,
            String nameAr,
            List<CurriculumGradeView> grades) {}

    public record CatalogAnswerRequest(
            @NotBlank String exerciseId,
            @NotNull @Min(0) @Max(5) Integer optionIndex) {}

    public record CatalogAnswerResult(
            boolean correct,
            String correctAnswer,
            String explanation) {}
}
