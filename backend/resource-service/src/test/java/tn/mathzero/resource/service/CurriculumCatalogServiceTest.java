package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tn.mathzero.resource.api.LearningDtos;

class CurriculumCatalogServiceTest {
    private final CurriculumCatalogService service = new CurriculumCatalogService();

    @Test
    void exposesEveryConfiguredGradeWithThreeChaptersAndExercises() {
        for (String gradeCode : new String[] {
                "P1", "P2", "P3", "P4", "P5", "P6",
                "B7", "B8", "B9", "S1", "S2", "S3", "S4"
        }) {
            LearningDtos.CurriculumGradeView grade = service.grade(gradeCode);
            assertEquals(3, grade.chapters().size());
            assertTrue(grade.chapters().stream()
                    .allMatch(chapter -> chapter.exercises().size() == 2));
        }
    }

    @Test
    void marksPrimaryGradesAsDraftAndSecondaryGradesAsReviewedCatalog() {
        assertTrue(service.grade("P1").draft());
        assertFalse(service.grade("B7").draft());
        assertFalse(service.grade("S1").draft());
    }

    @Test
    void groupsEveryGradeUnderItsCycleWithBilingualNames() {
        var cycles = service.cycles();

        assertEquals(3, cycles.size());
        assertEquals("PRIMARY", cycles.get(0).cycle());
        assertEquals("Primaire", cycles.get(0).nameFr());
        assertEquals(6, cycles.get(0).grades().size());
        assertEquals("Première année primaire", cycles.get(0).grades().get(0).gradeName());
        assertEquals("السنة الأولى ابتدائي", cycles.get(0).grades().get(0).gradeNameAr());

        assertEquals("BASIC", cycles.get(1).cycle());
        assertEquals(3, cycles.get(1).grades().size());

        assertEquals("SECONDARY", cycles.get(2).cycle());
        assertEquals(4, cycles.get(2).grades().size());
        assertEquals("S4", cycles.get(2).grades().get(3).gradeCode());
    }

    @Test
    void acceptsTheFirstExerciseOptionAndRejectsAnotherOption() {
        LearningDtos.CurriculumGradeView grade = service.grade("B7");
        String exerciseId = grade.chapters().get(0).exercises().get(0).id();

        LearningDtos.CatalogAnswerResult correct = service.answer(
                "B7", new LearningDtos.CatalogAnswerRequest(exerciseId, 0));
        LearningDtos.CatalogAnswerResult wrong = service.answer(
                "B7", new LearningDtos.CatalogAnswerRequest(exerciseId, 1));

        assertTrue(correct.correct());
        assertFalse(wrong.correct());
    }
}
