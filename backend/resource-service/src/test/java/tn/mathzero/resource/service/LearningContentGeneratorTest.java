package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.service.LearningContentGenerator.Generated;

class LearningContentGeneratorTest {
    private final LearningContentGenerator generator = new LearningContentGenerator();

    @Test
    void createsManualMultiplicationLessonsForAllEducationCycles() {
        assertMultiplicationLesson(
                ExternalResource.Cycle.PRIMARY,
                "Une multiplication à un chiffre",
                "24 × 3",
                "72");
        assertMultiplicationLesson(
                ExternalResource.Cycle.BASIC,
                "Deux chiffres par deux chiffres",
                "238 + 680",
                "918");
        assertMultiplicationLesson(
                ExternalResource.Cycle.SECONDARY,
                "Multiplication à plusieurs retenues",
                "1 404 + 7 020",
                "8 424");
    }

    @Test
    void generatesArabicMultiplicationExplanationsAndCycleSpecificBackfills() {
        Generated lesson = generator.generate(
                "الضرب اليدوي", "شرح جدول الضرب", "جدول الضرب",
                "ar", ExternalResource.Cycle.SECONDARY);

        assertEquals("MULTIPLICATION", lesson.demo().kind());
        assertEquals("الضرب المطوّل مع عدة عمليات حمل", lesson.demo().titleAr());
        assertFalse(lesson.demo().frames().get(0).narrationAr().isBlank());
        assertTrue(lesson.steps().stream().allMatch(step -> !step.titleAr().isBlank()));

        LearningContentGenerator.Demo backfilled = generator.backfillDemo(
                "Multiplication", "Manual multiplication", "multiplication",
                ExternalResource.Cycle.BASIC);
        assertEquals("Deux chiffres par deux chiffres", backfilled.titleFr());
    }

    private void assertMultiplicationLesson(
            ExternalResource.Cycle cycle,
            String expectedDemoTitle,
            String expectedFinalOperation,
            String expectedResult) {
        Generated lesson = generator.generate(
                "Multiplication manuelle", "Apprendre la multiplication", "multiplication",
                "fr", cycle);

        assertEquals("MULTIPLICATION", lesson.demo().kind());
        assertEquals(expectedDemoTitle, lesson.demo().titleFr());
        assertEquals(expectedFinalOperation, lesson.demo().frames().get(lesson.demo().frames().size() - 1).left());
        assertEquals(expectedResult, lesson.demo().frames().get(lesson.demo().frames().size() - 1).right());
        assertTrue(lesson.demo().frames().stream()
                .anyMatch(frame -> frame.left().matches("\\d+ × \\d+")));
        assertEquals(3, lesson.questions().size());
        assertTrue(lesson.steps().stream().anyMatch(step -> step.kind().equals("example")));
    }
}
