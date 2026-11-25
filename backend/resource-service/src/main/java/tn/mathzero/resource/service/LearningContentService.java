package tn.mathzero.resource.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.api.LearningDtos;
import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.domain.LearningContent;
import tn.mathzero.resource.repository.ExternalResourceRepository;
import tn.mathzero.resource.repository.LearningContentRepository;
import tn.mathzero.resource.service.LearningContentGenerator.Demo;
import tn.mathzero.resource.service.LearningContentGenerator.DemoFrame;
import tn.mathzero.resource.service.LearningContentGenerator.Generated;
import tn.mathzero.resource.service.LearningContentGenerator.Question;
import tn.mathzero.resource.service.LearningContentGenerator.Step;

/** Generates, stores and serves the animated lesson attached to each approved resource. */
@Service
public class LearningContentService {
    private static final int GENERATOR_VERSION = 2;

    private final LearningContentRepository contents;
    private final ExternalResourceRepository resources;
    private final LearningContentGenerator generator;
    private final ObjectMapper mapper;

    public LearningContentService(
            LearningContentRepository contents,
            ExternalResourceRepository resources,
            LearningContentGenerator generator,
            ObjectMapper mapper) {
        this.contents = contents;
        this.resources = resources;
        this.generator = generator;
        this.mapper = mapper;
    }

    /** Idempotent: approval calls this, and legacy resources generate on first read. */
    @Transactional
    public void ensureFor(UUID resourceId) {
        LearningContent content = contents.findByResourceId(resourceId).orElse(null);
        if (content != null && content.getGeneratorVersion() >= GENERATOR_VERSION) {
            return;
        }
        ExternalResource resource = resources.findById(resourceId).orElseThrow(ResourceException::notFound);
        if (resource.getReviewStatus() != ExternalResource.ReviewStatus.APPROVED) {
            return;
        }
        boolean ar = "ar".equalsIgnoreCase(resource.getLanguage());
        Generated generated = generator.generate(
                resource.getTitle(), resource.getDescription(), resource.getTopic(),
                resource.getLanguage(), resource.getCycle());

        List<LearningDtos.StepView> steps = new java.util.ArrayList<>();
        for (Step step : generated.steps()) {
            steps.add(new LearningDtos.StepView(
                    step.kind(),
                    ar ? step.titleAr() : step.titleFr(),
                    ar ? step.bodyAr() : step.bodyFr(),
                    step.formula()));
        }
        steps.add(new LearningDtos.StepView(
                "demo",
                ar ? "عرض متحرك" : "Démonstration animée",
                ar ? "شغّل العرض لتابع الحل خطوة بخطوة."
                   : "Lancez la démonstration pour suivre la résolution étape par étape.",
                null));
        String sourceTitle = ar ? "المصدر والحقوق" : "Source et droits";
        String sourceBody = (ar
                ? "يعتمد هذا الدرس على المورد المعتمد بموجب الترخيص "
                : "Cette leçon s'appuie sur la ressource approuvée sous licence ")
                + resource.getLicenceCode() + ". "
                + (ar ? "نسب المحتوى: " : "Attribution : ") + resource.getAttributionText();
        steps.add(new LearningDtos.StepView("source", sourceTitle, sourceBody, null));
        steps.add(new LearningDtos.StepView(
                "practice",
                ar ? "حان دورك" : "À vous de jouer",
                ar ? "أجب عن الأسئلة التالية لتأكيد فهمك."
                   : "Répondez aux questions suivantes pour vérifier votre compréhension.",
                null));

        if (content == null) {
            content = new LearningContent();
            content.setId(UUID.randomUUID());
            content.setCreatedAt(Instant.now());
        }
        content.setResourceId(resourceId);
        content.setCycle(resource.getCycle());
        content.setLanguage(resource.getLanguage());
        content.setTitle(resource.getTitle());
        content.setSummary(resource.getDescription());
        content.setStepsJson(write(steps));
        content.setExercisesJson(write(generated.questions()));
        content.setDemoJson(write(generated.demo()));
        content.setGeneratorVersion(GENERATOR_VERSION);
        contents.save(content);
    }

    @Transactional
    public LearningDtos.LessonView lesson(UUID resourceId) {
        ensureFor(resourceId);
        LearningContent content = contents.findByResourceId(resourceId)
                .orElseThrow(ResourceException::notFound);
        ExternalResource resource = resources.findById(resourceId)
                .orElseThrow(ResourceException::notFound);
        boolean ar = "ar".equalsIgnoreCase(content.getLanguage());
        List<LearningDtos.StepView> steps = new java.util.ArrayList<>(read(content.getStepsJson(),
                new TypeReference<List<LearningDtos.StepView>>() {}));
        if (steps.stream().noneMatch(step -> "demo".equals(step.kind()))) {
            int insertAt = 0;
            for (int i = 0; i < steps.size(); i++) {
                if ("source".equals(steps.get(i).kind()) || "practice".equals(steps.get(i).kind())) {
                    insertAt = i;
                    break;
                }
                insertAt = i + 1;
            }
            steps.add(insertAt, demoStep(ar));
        }

        Demo demo;
        if (content.getDemoJson() == null || content.getDemoJson().isBlank()) {
            demo = generator.backfillDemo(
                    resource.getTitle(), resource.getDescription(), resource.getTopic(), resource.getCycle());
            content.setDemoJson(write(demo));
            contents.save(content);
        } else {
            demo = read(content.getDemoJson(), new TypeReference<Demo>() {});
        }

        List<Question> questions = read(content.getExercisesJson(), new TypeReference<List<Question>>() {});
        List<LearningDtos.ExerciseView> exercises = questions.stream()
                .map(question -> new LearningDtos.ExerciseView(
                        question.id(),
                        ar ? question.promptAr() : question.promptFr(),
                        ar ? question.optionsAr() : question.optionsFr(),
                        ar ? "اختر الإجابة الصحيحة." : "Choisissez la bonne réponse."))
                .toList();
        return new LearningDtos.LessonView(
                resourceId,
                content.getTitle(),
                content.getSummary(),
                content.getLanguage(),
                content.getCycle() == null ? null : content.getCycle().name(),
                resource.getUrl(),
                resource.getLicenceName(),
                resource.getAttributionText(),
                steps,
                toDemoView(demo, ar),
                exercises);
    }

    @Transactional(readOnly = true)
    public LearningDtos.AnswerResult answer(UUID resourceId, LearningDtos.AnswerRequest request) {
        LearningContent content = contents.findByResourceId(resourceId)
                .orElseThrow(ResourceException::notFound);
        List<Question> questions = read(content.getExercisesJson(), new TypeReference<List<Question>>() {});
        Question question = questions.stream()
                .filter(item -> item.id().equals(request.questionId()))
                .findFirst()
                .orElseThrow(ResourceException::notFound);
        boolean ar = "ar".equalsIgnoreCase(content.getLanguage());
        List<String> options = ar ? question.optionsAr() : question.optionsFr();
        if (request.optionIndex() < 0 || request.optionIndex() >= options.size()) {
            throw ResourceException.invalid("Invalid answer option");
        }
        boolean correct = request.optionIndex() == question.correctIndex();
        String explanation = ar ? question.explanationAr() : question.explanationFr();
        String correctAnswer = options.get(question.correctIndex());
        Demo solution = question.solution() != null
                ? question.solution()
                : fallbackSolution(question, options, correctAnswer, explanation, ar);
        return new LearningDtos.AnswerResult(
                correct,
                correctAnswer,
                explanation,
                questions.indexOf(question) + 1,
                questions.size(),
                toDemoView(solution, ar));
    }

    private LearningDtos.StepView demoStep(boolean ar) {
        return new LearningDtos.StepView(
                "demo",
                ar ? "عرض متحرك" : "Démonstration animée",
                ar ? "شغّل العرض لتابع الحل خطوة بخطوة."
                   : "Lancez la démonstration pour suivre la résolution étape par étape.",
                null);
    }

    private LearningDtos.DemoView toDemoView(Demo demo, boolean ar) {
        List<LearningDtos.DemoFrameView> frames = demo.frames().stream()
                .map(frame -> new LearningDtos.DemoFrameView(
                        ar ? frame.labelAr() : frame.labelFr(),
                        ar ? frame.narrationAr() : frame.narrationFr(),
                        frame.formula(),
                        frame.left(),
                        frame.right(),
                        frame.progress()))
                .toList();
        return new LearningDtos.DemoView(demo.kind(), ar ? demo.titleAr() : demo.titleFr(), frames);
    }

    private Demo fallbackSolution(Question question, List<String> options,
            String correctAnswer, String explanation, boolean ar) {
        String prompt = ar ? question.promptAr() : question.promptFr();
        return new Demo("SOLUTION",
                ar ? "خطوات الحل" : "Méthode pas à pas",
                ar ? "خطوات الحل" : "Méthode pas à pas",
                List.of(
                        new DemoFrame(ar ? "حدّد" : "Repérer", ar ? "حدّد" : "Repérer",
                                prompt, prompt, null, prompt, prompt, 15),
                        new DemoFrame(ar ? "فكّر" : "Raisonner", ar ? "فكّر" : "Raisonner",
                                explanation, explanation, null, explanation, explanation, 65),
                        new DemoFrame(ar ? "أجب" : "Répondre", ar ? "أجب" : "Répondre",
                                correctAnswer, correctAnswer, null, options.get(0), correctAnswer, 100)));
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store generated lesson content", exception);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read generated lesson content", exception);
        }
    }
}
