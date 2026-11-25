package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.api.LearningDtos;
import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.domain.LearningContent;
import tn.mathzero.resource.repository.ExternalResourceRepository;
import tn.mathzero.resource.repository.LearningContentRepository;

@ExtendWith(MockitoExtension.class)
class LearningContentServiceTest {
    @Mock private LearningContentRepository contents;
    @Mock private ExternalResourceRepository resources;

    private final ObjectMapper mapper = new ObjectMapper();
    private LearningContentService service;

    @BeforeEach
    void setUp() {
        service = new LearningContentService(contents, resources, new LearningContentGenerator(), mapper);
    }

    private ExternalResource approvedResource(String language) {
        ExternalResource resource = new ExternalResource();
        resource.setId(UUID.randomUUID());
        resource.setTitle("Les fractions");
        resource.setDescription("Comprendre les fractions");
        resource.setTopic("fractions");
        resource.setLanguage(language);
        resource.setCycle(ExternalResource.Cycle.PRIMARY);
        resource.setReviewStatus(ExternalResource.ReviewStatus.APPROVED);
        resource.setLicenceCode("CC-BY-SA-4.0");
        resource.setLicenceName("Creative Commons Attribution-ShareAlike 4.0");
        resource.setAttributionText("Wikipedia contributors, Les fractions (CC BY-SA 4.0)");
        return resource;
    }

    @Test
    void lessonIsGeneratedOnceFromAnApprovedResourceWithoutExposingAnswers() {
        ExternalResource resource = approvedResource("fr");
        java.util.concurrent.atomic.AtomicReference<LearningContent> saved =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(resources.findById(resource.getId())).thenReturn(Optional.of(resource));
        when(contents.save(any(LearningContent.class))).thenAnswer(invocation -> {
            LearningContent value = invocation.getArgument(0);
            saved.set(value);
            return value;
        });
        when(contents.findByResourceId(resource.getId())).thenAnswer(
                invocation -> Optional.ofNullable(saved.get()));

        LearningDtos.LessonView lesson = service.lesson(resource.getId());

        assertEquals(6, lesson.steps().size());
        assertEquals("intro", lesson.steps().get(0).kind());
        assertTrue(lesson.steps().get(0).formula().contains("\\frac"));
        assertEquals("demo", lesson.steps().get(3).kind());
        assertEquals("source", lesson.steps().get(4).kind());
        assertEquals(3, lesson.exercises().size());
        assertEquals("q1", lesson.exercises().get(0).id());
        assertEquals("Choisissez la bonne réponse.", lesson.exercises().get(0).hint());

        assertEquals("FRACTIONS", lesson.demo().kind());
        assertEquals(4, lesson.demo().frames().size());
        assertEquals("Partager", lesson.demo().frames().get(0).label());
        assertEquals(100, lesson.demo().frames().get(3).progress());

        ArgumentCaptor<LearningContent> captor = ArgumentCaptor.forClass(LearningContent.class);
        verify(contents).save(captor.capture());
        assertTrue(captor.getValue().getExercisesJson().contains("correctIndex"));
        assertFalse(captor.getValue().getStepsJson().contains("correctIndex"));
        assertTrue(captor.getValue().getDemoJson().contains("FRACTIONS"));
        assertFalse(captor.getValue().getDemoJson().contains("correctIndex"));
    }

    @Test
    void generatedContentIsBilingual() {
        ExternalResource resource = approvedResource("ar");
        java.util.concurrent.atomic.AtomicReference<LearningContent> saved =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(resources.findById(resource.getId())).thenReturn(Optional.of(resource));
        when(contents.save(any(LearningContent.class))).thenAnswer(invocation -> {
            LearningContent value = invocation.getArgument(0);
            saved.set(value);
            return value;
        });
        when(contents.findByResourceId(resource.getId())).thenAnswer(
                invocation -> Optional.ofNullable(saved.get()));

        LearningDtos.LessonView lesson = service.lesson(resource.getId());

        assertEquals("الكسور", lesson.steps().get(0).title());
        assertEquals("كم ربعاً في الواحد الصحيح؟", lesson.exercises().get(0).prompt());
        assertEquals("بناء كسر متكافئ", lesson.demo().title());
        assertEquals("قسّم", lesson.demo().frames().get(0).label());
    }

    @Test
    void answerChecksTheQuestionAgainstStoredContent() throws Exception {
        UUID resourceId = UUID.randomUUID();
        LearningContent content = new LearningContent();
        content.setId(UUID.randomUUID());
        content.setResourceId(resourceId);
        content.setLanguage("fr");
        content.setStepsJson("[]");
        LearningContentGenerator.Question question = new LearningContentGenerator.Question(
                "q1", "Combien de quarts ?", "كم ربعاً؟", List.of("4", "2"), List.of("4", "2"), 0,
                "4 quarts.", "4 أرباع.", null);
        content.setExercisesJson(mapper.writeValueAsString(List.of(question)));
        when(contents.findByResourceId(resourceId)).thenReturn(Optional.of(content));

        LearningDtos.AnswerResult right = service.answer(resourceId, new LearningDtos.AnswerRequest("q1", 0));
        assertTrue(right.correct());
        assertEquals("4", right.correctAnswer());
        assertEquals(1, right.position());
        assertEquals(1, right.total());
        assertEquals("SOLUTION", right.solution().kind());
        assertEquals(3, right.solution().frames().size());
        assertEquals("4", right.solution().frames().get(2).right());

        LearningDtos.AnswerResult wrong = service.answer(resourceId, new LearningDtos.AnswerRequest("q1", 1));
        assertFalse(wrong.correct());
        assertEquals("4 quarts.", wrong.explanation());

        assertThrows(ResourceException.class,
                () -> service.answer(resourceId, new LearningDtos.AnswerRequest("unknown", 0)));
    }

    @Test
    void legacyLessonsGetADemoBackfilledOnFirstRead() throws Exception {
        ExternalResource resource = approvedResource("fr");
        LearningContent content = new LearningContent();
        content.setId(UUID.randomUUID());
        content.setResourceId(resource.getId());
        content.setLanguage("fr");
        content.setCycle(resource.getCycle());
        content.setTitle(resource.getTitle());
        content.setSummary(resource.getDescription());
        content.setStepsJson(mapper.writeValueAsString(
                List.of(new LearningDtos.StepView("intro", "Ancien titre", "Ancien texte", null))));
        LearningContentGenerator.Question question = new LearningContentGenerator.Question(
                "q1", "Question ?", "سؤال؟", List.of("4"), List.of("4"), 0,
                "Réponse.", "إجابة.", null);
        content.setExercisesJson(mapper.writeValueAsString(List.of(question)));
        when(contents.findByResourceId(resource.getId())).thenReturn(Optional.of(content));
        when(resources.findById(resource.getId())).thenReturn(Optional.of(resource));
        when(contents.save(any(LearningContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningDtos.LessonView lesson = service.lesson(resource.getId());

        assertTrue(lesson.steps().stream().anyMatch(step -> "demo".equals(step.kind())));
        assertEquals("FRACTIONS", lesson.demo().kind());
        verify(contents).save(content);
    }

    @Test
    void upgradesStoredLessonsWhenTheGeneratorVersionChanges() {
        ExternalResource resource = approvedResource("fr");
        resource.setTitle("Multiplication manuelle");
        resource.setDescription("Apprendre la multiplication posée");
        resource.setTopic("multiplication");
        resource.setCycle(ExternalResource.Cycle.SECONDARY);
        resource.setProvider("MANUAL");

        LearningContent existing = new LearningContent();
        existing.setId(UUID.randomUUID());
        existing.setResourceId(resource.getId());
        existing.setCycle(resource.getCycle());
        existing.setLanguage("fr");
        existing.setTitle("Multiplication manuelle");
        existing.setStepsJson("[]");
        existing.setExercisesJson("[]");
        existing.setDemoJson("{\"kind\":\"GENERAL\",\"frames\":[]}");
        when(contents.findByResourceId(resource.getId())).thenReturn(Optional.of(existing));
        when(resources.findById(resource.getId())).thenReturn(Optional.of(resource));
        when(contents.save(any(LearningContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureFor(resource.getId());

        assertEquals(2, existing.getGeneratorVersion());
        assertTrue(existing.getStepsJson().contains("Multiplier avec plusieurs retenues"));
        assertTrue(existing.getStepsJson().contains("CC-BY-SA-4.0"));
        assertTrue(existing.getStepsJson().contains("Wikipedia contributors"));
        assertFalse(existing.getStepsJson().contains("adapted from a Wikimedia"));
        assertTrue(existing.getDemoJson().contains("\"kind\":\"MULTIPLICATION\""));
        verify(contents).save(existing);
    }

    @Test
    void draftsDoNotGenerateLearningContent() {
        ExternalResource resource = approvedResource("fr");
        resource.setReviewStatus(ExternalResource.ReviewStatus.DRAFT);
        when(resources.findById(resource.getId())).thenReturn(Optional.of(resource));

        assertThrows(ResourceException.class, () -> service.lesson(resource.getId()));
        verify(contents, org.mockito.Mockito.never()).save(any(LearningContent.class));
    }
}
