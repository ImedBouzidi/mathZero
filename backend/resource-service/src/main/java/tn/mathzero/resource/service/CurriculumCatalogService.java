package tn.mathzero.resource.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import tn.mathzero.resource.api.LearningDtos;

@Service
public class CurriculumCatalogService {
    private final Map<String, LearningDtos.CurriculumGradeView> grades = new LinkedHashMap<>();

    public CurriculumCatalogService() {
        seedPrimary();
        seedBasic();
        seedSecondary();
    }

    public LearningDtos.CurriculumGradeView grade(String gradeCode) {
        LearningDtos.CurriculumGradeView grade =
                grades.get(normalize(gradeCode));
        if (grade == null) {
            throw ResourceException.notFound();
        }
        return grade;
    }

    /** The three experience cycles with their grades and chapters in display order. */
    public List<LearningDtos.CurriculumCycleView> cycles() {
        Map<String, List<LearningDtos.CurriculumGradeView>> grouped = new LinkedHashMap<>();
        for (LearningDtos.CurriculumGradeView grade : grades.values()) {
            grouped.computeIfAbsent(grade.cycle(), key -> new ArrayList<>()).add(grade);
        }
        return List.of(
                new LearningDtos.CurriculumCycleView(
                        "PRIMARY", "Primaire", "الابتدائي",
                        grouped.getOrDefault("PRIMARY", List.of())),
                new LearningDtos.CurriculumCycleView(
                        "BASIC", "Enseignement de base", "التعليم الأساسي",
                        grouped.getOrDefault("BASIC", List.of())),
                new LearningDtos.CurriculumCycleView(
                        "SECONDARY", "Enseignement secondaire", "التعليم الثانوي",
                        grouped.getOrDefault("SECONDARY", List.of())));
    }

    public LearningDtos.CatalogAnswerResult answer(
            String gradeCode,
            LearningDtos.CatalogAnswerRequest request) {
        LearningDtos.CurriculumGradeView grade = grade(gradeCode);
        for (LearningDtos.CatalogChapterView chapter : grade.chapters()) {
            for (LearningDtos.CatalogExerciseView exercise : chapter.exercises()) {
                if (!exercise.id().equals(request.exerciseId())) {
                    continue;
                }
                Exercise internal = exercises().get(exercise.id());
                if (internal == null || request.optionIndex() < 0
                        || request.optionIndex() >= exercise.options().size()) {
                    throw ResourceException.invalid("Invalid catalog answer option");
                }
                boolean correct = request.optionIndex() == internal.correctIndex();
                return new LearningDtos.CatalogAnswerResult(
                        correct,
                        exercise.options().get(internal.correctIndex()),
                        correct ? "Bonne réponse. " + internal.explanation()
                                : "Revois la méthode puis réessaie. " + internal.explanation());
            }
        }
        throw ResourceException.notFound();
    }

    private void seedPrimary() {
        for (int number = 1; number <= 6; number++) {
            String code = "P" + number;
            String gradeName = ordinalPrimary(number);
            String arabicName = ordinalPrimaryArabic(number);
            List<LearningDtos.CatalogChapterView> chapters = List.of(
                    primaryChapter(code, "NUMBERS", "Nombres et calcul",
                            "الأعداد والحساب", number,
                            "Compter, lire, comparer et calculer des nombres.",
                            "قراءة ومقارنة وحساب الأعداد."),
                    primaryChapter(code, "GEOMETRY", "Repérage et géométrie",
                            "المكان والهندسة", number,
                            "Reconnaître les formes, mesurer et se repérer.",
                            "التعرف إلى الأشكال والقياس والتحديد في المكان."),
                    primaryChapter(code, "DATA", "Mesures et organisation",
                            "القياسات وتنظيم المعطيات", number,
                            "Lire des longueurs, des tableaux et des horaires simples.",
                            "قراءة الأطوال والجداول والأوقات البسيطة.")
            );
            grades.put(code, view(code, gradeName, arabicName, "PRIMARY", true,
                    "Chapitres primaires provisoires — attendre la revue des programmes officiels par degré.",
                    chapters));
        }
    }

    private void seedBasic() {
        grades.put("B7", basicGrade("B7", "Septième année de l’enseignement de base",
                "السنة السابعة من التعليم الأساسي",
                "Nombres entiers, divisibilité et fractions",
                "الأعداد الصحيحة الطبيعية والقواسم والكسور"));
        grades.put("B8", basicGrade("B8", "Huitième année de l’enseignement de base",
                "السنة الثامنة من التعليم الأساسي",
                "Rationnels, puissances et racines",
                "الأعداد النسبية والتمكين والجذور"));
        grades.put("B9", basicGrade("B9", "Neuvième année de l’enseignement de base",
                "السنة التاسعة من التعليم الأساسي",
                "Nombres réels, géométrie dans l’espace",
                "الأعداد الحقيقية والهندسة في الفضاء"));
    }

    private LearningDtos.CurriculumGradeView basicGrade(
            String code, String frName, String arName, String numbersFr, String numbersAr) {
        List<LearningDtos.CatalogChapterView> chapters = List.of(
                chapter(code, "NUMBERS", numbersFr, numbersAr,
                        "Calculer avec précision et expliquer chaque étape.",
                        "الحساب بدقة وشرح كل مرحلة.",
                        "Nombres", "الأعداد"),
                chapter(code, "GEOMETRY", "Géométrie plane",
                        "الهندسة المستوية",
                        "Construire une figure, identifier ses propriétés et raisonner.",
                        "إنشاء الشكل وتحديد خواصه والتعليل.",
                        "Géométrie", "الهندسة"),
                chapter(code, "DATA", "Organisation des données",
                        "تنظيم المعطيات",
                        "Lire, représenter et interpréter des données.",
                        "قراءة وتمثيل وتفسير المعطيات.",
                        "Données", "معطيات")
        );
        return view(code, frName, arName, "BASIC", false,
                "Chapitres issus du projet officiel 2011 — révision pédagogique recommandée.",
                chapters);
    }

    private void seedSecondary() {
        grades.put("S1", secondaryGrade("S1", "Première année secondaire",
                "السنة الأولى ثانوي", "Fonctions linéaires et affines"));
        grades.put("S2", secondaryGrade("S2", "Deuxième année secondaire",
                "السنة الثانية ثانوي", "Fonctions réelles"));
        grades.put("S3", secondaryGrade("S3", "Troisième année secondaire",
                "السنة الثالثة ثانوي", "Suites, fonctions et intégration"));
        grades.put("S4", secondaryGrade("S4", "Quatrième année secondaire",
                "السنة الرابعة ثانوي", "Analyse, complexes et géométrie"));
    }

    private LearningDtos.CurriculumGradeView secondaryGrade(
            String code, String frName, String arName, String analysisFr) {
        List<LearningDtos.CatalogChapterView> chapters = List.of(
                chapter(code, "ANALYSIS", analysisFr, "التحليل والدوال",
                        "Modéliser une situation, calculer et justifier.",
                        "نمذجة وضعية والحساب والتعليل.",
                        "Analyse", "تحليل"),
                chapter(code, "GEOMETRY", "Géométrie analytique",
                        "الهندسة التحليلية",
                        "Utiliser vecteurs, coordonnées, cercles et espaces.",
                        "استعمال المتجهات والإحداثيات والدوائر والفضاء.",
                        "Géométrie", "هندسة"),
                chapter(code, "DATA", "Probabilités et statistiques",
                        "الاحتمالات والإحصاء",
                        "Analyser des données et calculer une probabilité.",
                        "تحليل المعطيات وحساب الاحتمالات.",
                        "Données", "معطيات")
        );
        return view(code, frName, arName, "SECONDARY", false,
                "Chapitres issus du programme secondaire archivé 2008/2011 — révision spécialisée requise.",
                chapters);
    }

    private LearningDtos.CatalogChapterView primaryChapter(
            String gradeCode, String domain, String frTitle, String arTitle, int grade,
            String frDescription, String arDescription) {
        return chapter(gradeCode, domain, frTitle, arTitle, frDescription, arDescription,
                "Niveau " + grade, "المستوى " + grade);
    }

    private LearningDtos.CatalogChapterView chapter(
            String gradeCode, String domain, String frTitle, String arTitle,
            String frDescription, String arDescription, String stepFr, String stepAr) {
        String chapterId = gradeCode + "-" + domain;
        String frStepTitle = stepFr + " : " + frTitle;
        String arStepTitle = stepAr + " : " + arTitle;
        List<LearningDtos.StepView> steps = List.of(
                new LearningDtos.StepView("intro", frStepTitle, frDescription, null),
                new LearningDtos.StepView("concept", "Méthode",
                        "On identifie les données, on choisit une opération ou une propriété, puis on vérifie le résultat.",
                        null),
                new LearningDtos.StepView("practice", "À vous de jouer",
                        "Résolvez les exercices avant de passer au chapitre suivant.", null)
        );
        List<LearningDtos.CatalogExerciseView> exerciseViews = new ArrayList<>();
        for (int number = 1; number <= 2; number++) {
            String exerciseId = chapterId + "-Q" + number;
            Exercise exercise = exerciseFor(gradeCode, domain, number);
            exercises().put(exerciseId, exercise);
            exerciseViews.add(new LearningDtos.CatalogExerciseView(
                    exerciseId,
                    exercise.promptFr(),
                    List.of(exercise.option1(), exercise.option2(), exercise.option3(), exercise.option4()),
                    "Choisissez une réponse puis vérifiez votre raisonnement.",
                    exercise.promptAr(),
                    List.of(exercise.option1(), exercise.option2(), exercise.option3(), exercise.option4()),
                    "اختر إجابة ثم تحقق من تعليلك."));
        }
        return new LearningDtos.CatalogChapterView(
                chapterId,
                frTitle,
                frDescription,
                arTitle,
                arDescription,
                steps,
                exerciseViews);
    }

    private Exercise exerciseFor(String gradeCode, String domain, int number) {
        int base = gradeNumber(gradeCode) + number;
        if ("NUMBERS".equals(domain) || "ANALYSIS".equals(domain)) {
            int a = base + 3;
            int b = base + 1;
            int result = a + b;
            return new Exercise(
                    "Calculer " + a + " + " + b,
                    "احسب " + a + " + " + b,
                    String.valueOf(result),
                    String.valueOf(result - 1),
                    String.valueOf(result + 2),
                    String.valueOf(result + 5),
                    0,
                    "Additionner les deux termes donne " + result + ".");
        }
        if ("GEOMETRY".equals(domain)) {
            int side = base + 2;
            int perimeter = side * 4;
            return new Exercise(
                    "Un carré de côté " + side + " cm a quel périmètre ?",
                    "ما محيط مربع طول ضلعه " + side + " سم؟",
                    perimeter + " cm",
                    String.valueOf(perimeter - side) + " cm",
                    String.valueOf(perimeter + side) + " cm",
                    String.valueOf(side) + " cm",
                    0,
                    "Le périmètre du carré est 4 × côté.");
        }
        int total = base + 7;
        int selected = base + 2;
        return new Exercise(
                "Dans " + total + " élèves, " + selected + " préfèrent les sciences. Quelle proportion ?",
                "من " + total + " تلاميذ، يفضل " + selected + " العلوم. ما النسبة؟",
                selected + "/" + total,
                "1/" + total,
                (selected + 1) + "/" + total,
                "1/2",
                0,
                "La proportion est le nombre favorable sur le nombre total.");
    }

    private LearningDtos.CurriculumGradeView view(
            String code, String frName, String arName, String cycle, boolean draft,
            String reviewNote, List<LearningDtos.CatalogChapterView> chapters) {
        return new LearningDtos.CurriculumGradeView(
                code, frName, arName, cycle, draft, reviewNote, chapters);
    }

    private int gradeNumber(String gradeCode) {
        if (gradeCode.startsWith("P")) return Integer.parseInt(gradeCode.substring(1));
        if (gradeCode.startsWith("B")) return 6 + Integer.parseInt(gradeCode.substring(1));
        if (gradeCode.startsWith("S")) return 9 + Integer.parseInt(gradeCode.substring(1));
        return 1;
    }

    private String normalize(String gradeCode) {
        return gradeCode == null ? "" : gradeCode.trim().toUpperCase(Locale.ROOT);
    }

    private String ordinalPrimary(int number) {
        return switch (number) {
            case 1 -> "Première année primaire";
            case 2 -> "Deuxième année primaire";
            case 3 -> "Troisième année primaire";
            case 4 -> "Quatrième année primaire";
            case 5 -> "Cinquième année primaire";
            default -> "Sixième année primaire";
        };
    }

    private String ordinalPrimaryArabic(int number) {
        return switch (number) {
            case 1 -> "السنة الأولى ابتدائي";
            case 2 -> "السنة الثانية ابتدائي";
            case 3 -> "السنة الثالثة ابتدائي";
            case 4 -> "السنة الرابعة ابتدائي";
            case 5 -> "السنة الخامسة ابتدائي";
            default -> "السنة السادسة ابتدائي";
        };
    }

    private Map<String, Exercise> exercises() {
        if (internalExercises == null) {
            internalExercises = new LinkedHashMap<>();
        }
        return internalExercises;
    }

    private Map<String, Exercise> internalExercises;

    private record Exercise(
            String promptFr, String promptAr, String option1, String option2,
            String option3, String option4, int correctIndex, String explanation) {}
}
