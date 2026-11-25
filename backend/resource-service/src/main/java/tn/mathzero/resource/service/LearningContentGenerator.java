package tn.mathzero.resource.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import tn.mathzero.resource.domain.ExternalResource;

/**
 * Deterministic, offline lesson builder. It uses a small set of bilingual topic
 * templates plus the approved resource metadata; no external AI service and no
 * administrator input are involved. The generated content is intentionally
 * introductory and always credits the source resource.
 */
@Component
public class LearningContentGenerator {
    public record Step(String kind, String titleFr, String titleAr, String bodyFr, String bodyAr, String formula) {}

    /** One animated frame of a worked demonstration. */
    public record DemoFrame(
            String labelFr,
            String labelAr,
            String narrationFr,
            String narrationAr,
            String formula,
            String left,
            String right,
            int progress) {}

    public record Demo(
            String kind,
            String titleFr,
            String titleAr,
            List<DemoFrame> frames) {}

    public record Question(
            String id,
            String promptFr,
            String promptAr,
            List<String> optionsFr,
            List<String> optionsAr,
            int correctIndex,
            String explanationFr,
            String explanationAr,
            Demo solution) {}

    public record Generated(List<Step> steps, List<Question> questions, Demo demo) {
        public Generated(List<Step> steps, List<Question> questions) {
            this(steps, questions, new Demo("GENERAL", "Démonstration", "عرض توضيحي", List.of()));
        }
    }

    private enum Topic {
        FRACTIONS, EQUATION, GEOMETRY, FUNCTION, TRIGONOMETRY, MULTIPLICATION, ARITHMETIC, GENERAL
    }

    public Generated generate(
            String title, String description, String topic, String language, ExternalResource.Cycle cycle) {
        boolean ar = "ar".equalsIgnoreCase(language);
        Topic detected = detect(title, topic, description);
        Generated generated = switch (detected) {
            case FRACTIONS -> fractions();
            case EQUATION -> equation();
            case GEOMETRY -> geometry();
            case FUNCTION -> function();
            case TRIGONOMETRY -> trigonometry();
            case MULTIPLICATION -> multiplication(cycle);
            case ARITHMETIC -> arithmetic();
            case GENERAL -> general(title, description, ar, cycle);
        };
        return new Generated(generated.steps(), generated.questions(), demoFor(detected, title, description, cycle));
    }

    /** Used to backfill demonstrations for lessons generated before this feature existed. */
    public Demo backfillDemo(
            String title, String description, String topic, ExternalResource.Cycle cycle) {
        return demoFor(detect(title, topic, description), title, description, cycle);
    }

    private static Topic detect(String title, String topic, String description) {
        String text = ((title == null ? "" : title) + " " + (topic == null ? "" : topic) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        if (contains(text, "fraction", "كسور", "كسر")) return Topic.FRACTIONS;
        if (contains(text, "multiplication", "multiply", "multiplying", "times table",
                "table de multiplication", "جدول الضرب", "الضرب اليدوي", "الضرب")) {
            return Topic.MULTIPLICATION;
        }
        if (contains(text, "équation", "equation", "معادلة")) return Topic.EQUATION;
        if (contains(text, "trigonométrie", "trigonometrie", "trigonometry", "مثلثات")) return Topic.TRIGONOMETRY;
        if (contains(text, "géométrie", "geometrie", "geometry", "هندسة")) return Topic.GEOMETRY;
        if (contains(text, "fonction", "function", "دالة")) return Topic.FUNCTION;
        if (contains(text, "arithmétique", "arithmetique", "arithmetic", "الحساب", "حساب")) return Topic.ARITHMETIC;
        return Topic.GENERAL;
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private static Step step(String kind, String titleFr, String titleAr, String bodyFr, String bodyAr, String formula) {
        return new Step(kind, titleFr, titleAr, bodyFr, bodyAr, formula);
    }

    private static Question question(int number, String promptFr, String promptAr,
            List<String> optionsFr, List<String> optionsAr, int correct,
            String explanationFr, String explanationAr) {
        String answerFr = optionsFr.get(correct);
        String answerAr = optionsAr.get(correct);
        Demo solution = demo("SOLUTION",
                "Méthode pas à pas", "خطوات الحل",
                frame("Repérer", "حدّد",
                        "On relit l’énoncé : " + promptFr, "نعيد قراءة السؤال: " + promptAr,
                        null, promptFr, promptAr, 15),
                frame("Raisonner", "فكّر",
                        explanationFr, explanationAr, null, explanationFr, explanationAr, 65),
                frame("Répondre", "أجب",
                        "La bonne réponse est « " + answerFr + " ».", "الإجابة الصحيحة هي « " + answerAr + " ».",
                        null, answerFr, answerAr, 100));
        return new Question("q" + number, promptFr, promptAr, optionsFr, optionsAr, correct,
                explanationFr, explanationAr, solution);
    }

    private static DemoFrame frame(String labelFr, String labelAr, String narrationFr, String narrationAr,
            String formula, String left, String right, int progress) {
        return new DemoFrame(labelFr, labelAr, narrationFr, narrationAr, formula, left, right, progress);
    }

    private static Demo demo(String kind, String titleFr, String titleAr, DemoFrame... frames) {
        return new Demo(kind, titleFr, titleAr, List.of(frames));
    }

    private Demo demoFor(
            Topic topic, String title, String description, ExternalResource.Cycle cycle) {
        return switch (topic) {
            case FRACTIONS -> fractionsDemo();
            case EQUATION -> equationDemo();
            case GEOMETRY -> geometryDemo();
            case FUNCTION -> functionDemo();
            case TRIGONOMETRY -> trigonometryDemo();
            case MULTIPLICATION -> multiplicationDemo(cycle);
            case ARITHMETIC -> arithmeticDemo();
            case GENERAL -> generalDemo(title, description);
        };
    }

    private Generated multiplication(ExternalResource.Cycle cycle) {
        if (cycle == null || cycle == ExternalResource.Cycle.PRIMARY) {
            return primaryMultiplication();
        }
        if (cycle == ExternalResource.Cycle.BASIC) {
            return basicMultiplication();
        }
        return secondaryMultiplication();
    }

    private Generated primaryMultiplication() {
        return new Generated(
                List.of(
                        step("intro", "Les tables de multiplication", "جداول الضرب",
                                "La table de 3 donne 3 × 4 = 12. Multiplier, c’est additionner plusieurs groupes identiques.",
                                "جدول الضرب في 3 يعطينا 3 × 4 = 12. الضرب هو جمع مجموعات متساوية.",
                                "3\\times4=12"),
                        step("concept", "Multiplier en colonnes", "الضرب عمودياً",
                                "Pour 24 × 3, commence par les unités : 3 × 4 = 12. Écris 2 aux unités et retiens 1 dizaine.",
                                "لحساب 24 × 3 نبدأ بالآحاد: 3 × 4 = 12. نكتب 2 في خانة الآحاد ونحتفظ بعشرة واحدة.",
                                "3\\times4=12"),
                        step("example", "Terminer 24 × 3", "إكمال 24 × 3",
                                "Multiplie les dizaines : 3 × 2 dizaines = 6 dizaines. Ajoute la dizaine retenue : 7 dizaines. Le résultat est 72.",
                                "نضرب العشرات: 3 × عشرتين = 6 عشرات. نضيف العشرة المحتفظ بها فنحصل على 7 عشرات. النتيجة 72.",
                                "3\\times20+10+2=72")),
                List.of(
                        question(1, "Dans la table de 3, combien vaut 3 × 4 ?", "في جدول الضرب في 3، كم يساوي 3 × 4؟",
                                List.of("12", "7", "24", "10"), List.of("12", "7", "24", "10"), 0,
                                "3 groupes de 4 font 12.", "ثلاث مجموعات من 4 تساوي 12."),
                        question(2, "Combien vaut 24 × 3 ?", "كم يساوي 24 × 3؟",
                                List.of("72", "62", "27", "84"), List.of("72", "62", "27", "84"), 0,
                                "3 × 4 = 12 : on écrit 2 et on retient 1. Puis 3 × 2 + 1 = 7 dizaines.",
                                "3 × 4 = 12: نكتب 2 ونحتفظ بواحد. ثم 3 × 2 + 1 = 7 عشرات."),
                        question(3, "Dans 3 × 4 = 12, quel chiffre écrit-on aux unités ?",
                                "في 3 × 4 = 12، ما الرقم الذي نكتبه في الآحاد؟",
                                List.of("2", "1", "12", "4"), List.of("2", "1", "12", "4"), 0,
                                "On écrit 2 aux unités et on retient la dizaine.", "نكتب 2 في الآحاد ونحتفظ بالعشرة.")));
    }

    private Generated basicMultiplication() {
        return new Generated(
                List.of(
                        step("intro", "Multiplier deux nombres à deux chiffres", "ضرب عددين من رقمين",
                                "Utilise la table de multiplication pour calculer 34 × 27. Commence par les unités du second nombre.",
                                "نستخدم جدول الضرب لحساب 34 × 27. نبدأ بآحاد العدد الثاني.",
                                "34\\times27"),
                        step("concept", "Calculer la première ligne", "حساب السطر الأول",
                                "7 × 4 = 28 : écris 8 et retiens 2. Puis 7 × 3 dizaines + 2 = 23 dizaines. La première ligne vaut 238.",
                                "7 × 4 = 28: نكتب 8 ونحتفظ باثنين. ثم 7 × 3 عشرات + 2 = 23 عشرة. السطر الأول يساوي 238.",
                                "34\\times7=238"),
                        step("example", "Décaler puis additionner", "إزاحة ثم جمع",
                                "Le 2 de 27 représente 2 dizaines : 34 × 20 = 680. Décale cette ligne d’un rang, puis additionne 238 + 680 = 918.",
                                "الرقم 2 في 27 يمثل عشرتين: 34 × 20 = 680. نزيح السطر خانة واحدة ثم نجمع 238 + 680 = 918.",
                                "238+680=918")),
                List.of(
                        question(1, "Combien vaut 7 × 4 ?", "كم يساوي 7 × 4؟",
                                List.of("28", "24", "32", "11"), List.of("28", "24", "32", "11"), 0,
                                "La table de 7 donne 7 × 4 = 28.", "يعطينا جدول الضرب في 7: 7 × 4 = 28."),
                        question(2, "Quelle est la première ligne de 34 × 27 ?", "ما السطر الأول في 34 × 27؟",
                                List.of("238", "68", "918", "248"), List.of("238", "68", "918", "248"), 0,
                                "34 × 7 = 238, retenues comprises.", "34 × 7 = 238 مع إضافة الأرقام المحتفظ بها."),
                        question(3, "Quel est le résultat de 34 × 27 ?", "ما ناتج 34 × 27؟",
                                List.of("918", "812", "238", "680"), List.of("918", "812", "238", "680"), 0,
                                "On additionne les produits partiels : 238 + 680 = 918.",
                                "نجمع النواتج الجزئية: 238 + 680 = 918.")));
    }

    private Generated secondaryMultiplication() {
        return new Generated(
                List.of(
                        step("intro", "Multiplier avec plusieurs retenues", "الضرب مع عدة أرقام محتفظ بها",
                                "La table de multiplication aide aussi pour les grands nombres. Posons 234 × 36 en alignant unités, dizaines et centaines.",
                                "يساعدنا جدول الضرب أيضاً في الأعداد الكبيرة. نرتب 234 × 36 بحيث تتقابل الآحاد والعشرات والمئات.",
                                "234\\times36"),
                        step("concept", "Multiplier par les unités", "الضرب في الآحاد",
                                "Calcule 234 × 6 de droite à gauche : 6 × 4 = 24, puis 6 × 3 + 2 = 20, puis 6 × 2 + 2 = 14. La ligne vaut 1 404.",
                                "نحسب 234 × 6 من اليمين إلى اليسار: 6 × 4 = 24، ثم 6 × 3 + 2 = 20، ثم 6 × 2 + 2 = 14. السطر يساوي 1 404.",
                                "234\\times6=1404"),
                        step("example", "Multiplier par les dizaines", "الضرب في العشرات",
                                "Le 3 de 36 vaut 30 : 234 × 30 = 7 020. Décale le produit d’un rang et additionne 1 404 + 7 020 = 8 424.",
                                "الرقم 3 في 36 يمثل 30: 234 × 30 = 7 020. نزيح الناتج خانة واحدة ثم نجمع 1 404 + 7 020 = 8 424.",
                                "1404+7020=8424")),
                List.of(
                        question(1, "Combien vaut 6 × 4 ?", "كم يساوي 6 × 4؟",
                                List.of("24", "10", "20", "64"), List.of("24", "10", "20", "64"), 0,
                                "La table de 6 donne 6 × 4 = 24.", "يعطينا جدول الضرب في 6: 6 × 4 = 24."),
                        question(2, "Quelle est la première ligne de 234 × 36 ?", "ما السطر الأول في 234 × 36؟",
                                List.of("1 404", "702", "7 020", "8 424"), List.of("1 404", "702", "7 020", "8 424"), 0,
                                "234 × 6 = 1 404, en reportant chaque retenue.", "234 × 6 = 1 404 بعد إضافة كل رقم محتفظ به."),
                        question(3, "Quel est le résultat de 234 × 36 ?", "ما ناتج 234 × 36؟",
                                List.of("8 424", "8 204", "1 404", "7 020"), List.of("8 424", "8 204", "1 404", "7 020"), 0,
                                "1 404 + 7 020 = 8 424.", "1 404 + 7 020 = 8 424.")));
    }

    private Demo multiplicationDemo(ExternalResource.Cycle cycle) {
        if (cycle == ExternalResource.Cycle.BASIC) {
            return basicMultiplicationDemo();
        }
        if (cycle == ExternalResource.Cycle.SECONDARY) {
            return secondaryMultiplicationDemo();
        }
        return primaryMultiplicationDemo();
    }

    private Demo primaryMultiplicationDemo() {
        return demo("MULTIPLICATION", "Une multiplication à un chiffre", "الضرب في رقم واحد",
                frame("Choisir dans la table", "الاختيار من الجدول",
                        "Repère 3 × 4 dans la table : le produit est 12.",
                        "نبحث عن 3 × 4 في الجدول: الناتج هو 12.",
                        "3\\times4=12", "3 × 4", "12", 20),
                frame("Multiplier les unités", "ضرب الآحاد",
                        "Dans 24 × 3, 3 × 4 = 12. Écris 2 aux unités et retiens 1 dizaine.",
                        "في 24 × 3، نحسب 3 × 4 = 12. نكتب 2 في الآحاد ونحتفظ بعشرة.",
                        "3\\times4=12", "4 × 3", "12", 50),
                frame("Multiplier les dizaines", "ضرب العشرات",
                        "3 × 2 dizaines = 6 dizaines. Ajoute la dizaine retenue : 7 dizaines.",
                        "3 × عشرتين = 6 عشرات. نضيف العشرة المحتفظ بها فنحصل على 7 عشرات.",
                        "3\\times2+1=7", "2 × 3", "7 dizaines", 75),
                frame("Lire le résultat", "قراءة الناتج",
                        "Les 7 dizaines et les 2 unités donnent 72. Donc 24 × 3 = 72.",
                        "7 عشرات و2 آحاد تساوي 72. إذن 24 × 3 = 72.",
                        "24\\times3=72", "24 × 3", "72", 100));
    }

    private Demo basicMultiplicationDemo() {
        return demo("MULTIPLICATION", "Deux chiffres par deux chiffres", "ضرب عددين من رقمين",
                frame("Choisir dans la table", "الاختيار من الجدول",
                        "Dans la table de 7, 7 × 4 = 28.",
                        "في جدول الضرب في 7، 7 × 4 = 28.",
                        "7\\times4=28", "7 × 4", "28", 15),
                frame("Multiplier les unités", "ضرب الآحاد",
                        "Écris 8 aux unités et retiens 2 dizaines.",
                        "نكتب 8 في الآحاد ونحتفظ بعشرتين.",
                        "7\\times4=28", "7 × 4", "8, retenue 2", 35),
                frame("Multiplier les dizaines", "ضرب العشرات",
                        "7 × 3 dizaines + 2 dizaines retenues = 23 dizaines. La première ligne est 238.",
                        "7 × 3 عشرات + عشرتان محتفظ بهما = 23 عشرة. السطر الأول هو 238.",
                        "7\\times30+20=230", "7 × 3 dizaines + 2", "238", 55),
                frame("Décaler le second produit", "إزاحة الناتج الثاني",
                        "Le 2 de 27 signifie 20. Ainsi 34 × 20 = 680 : on décale d’une colonne.",
                        "الرقم 2 في 27 يعني 20. لذلك 34 × 20 = 680 ونزيح الناتج خانة واحدة.",
                        "34\\times20=680", "34 × 20", "680", 75),
                frame("Additionner les lignes", "جمع السطرين",
                        "Additionne les produits partiels : 238 + 680 = 918.",
                        "نجمع النواتج الجزئية: 238 + 680 = 918.",
                        "238+680=918", "238 + 680", "918", 100));
    }

    private Demo secondaryMultiplicationDemo() {
        return demo("MULTIPLICATION", "Multiplication à plusieurs retenues", "الضرب المطوّل مع عدة عمليات حمل",
                frame("Choisir dans la table", "الاختيار من الجدول",
                        "La table de 6 donne 6 × 4 = 24 : on écrit 4 et on retient 2.",
                        "يعطينا جدول الضرب في 6: 6 × 4 = 24. نكتب 4 ونحتفظ باثنين.",
                        "6\\times4=24", "6 × 4", "24", 15),
                frame("Calculer les dizaines", "حساب العشرات",
                        "6 × 3 = 18, puis ajoute les 2 retenues : 20. Écris 0 et retiens 2.",
                        "6 × 3 = 18، ثم نضيف الاثنين المحتفظ بهما فنحصل على 20. نكتب 0 ونحتفظ باثنين.",
                        "6\\times3+2=20", "6 × 3", "20", 35),
                frame("Calculer les centaines", "حساب المئات",
                        "6 × 2 = 12, puis ajoute 2 : 14. La première ligne complète vaut 1 404.",
                        "6 × 2 = 12، ثم نضيف 2 فنحصل على 14. السطر الأول كاملاً يساوي 1 404.",
                        "6\\times2+2=14", "6 × 2", "1 404", 55),
                frame("Décaler le produit des dizaines", "إزاحة ناتج العشرات",
                        "Le 3 de 36 vaut 30 : 234 × 30 = 7 020. Décale d’une colonne.",
                        "الرقم 3 في 36 يعني 30: 234 × 30 = 7 020. نزيح الناتج خانة واحدة.",
                        "234\\times30=7020", "234 × 30", "7 020", 75),
                frame("Additionner les lignes", "جمع السطرين",
                        "Additionne 1 404 et 7 020 : le résultat final est 8 424.",
                        "نجمع 1 404 و7 020: الناتج النهائي هو 8 424.",
                        "1404+7020=8424", "1 404 + 7 020", "8 424", 100));
    }

    private static Demo fractionsDemo() {
        return demo("FRACTIONS", "Construire une fraction équivalente", "بناء كسر متكافئ",
                frame("Partager", "قسّم",
                        "On partage le tout en 2 parties égales et on en prend 1.",
                        "نقسّم الواحد إلى جزأين متساويين ونأخذ جزءاً واحداً.",
                        "\\frac{1}{2}", "1 part sur 2", "1 / 2", 50),
                frame("Multiplier", "اضرب",
                        "On multiplie le numérateur et le dénominateur par 2.",
                        "نضرب البسط والمقام في 2.",
                        "\\frac{1\\times 2}{2\\times 2}", "1 × 2", "2 × 2", 70),
                frame("Comparer", "قارن",
                        "La portion est la même : 2 parts sur 4. La quantité ne change pas.",
                        "الجزء نفسه: 2 من 4. لا تتغير الكمية.",
                        "\\frac{2}{4}", "1 / 2", "2 / 4", 50),
                frame("Vérifier", "تحقّق",
                        "Les deux écritures désignent la même valeur.",
                        "الكتابتان تدلّان على القيمة نفسها.",
                        "\\frac{1}{2}=\\frac{2}{4}", "1 / 2", "= 2 / 4", 100));
    }

    private static Demo equationDemo() {
        return demo("EQUATION", "Garder la balance", "الحفاظ على التوازن",
                frame("Écrire", "اكتب",
                        "On place l’équation sur une balance : x + 3 à gauche, 7 à droite.",
                        "نضع المعادلة على ميزان: x + 3 على اليسار و 7 على اليمين.",
                        "x+3=7", "x + 3", "7", 40),
                frame("Retirer", "اطرح",
                        "On retire 3 des deux plateaux pour garder l’équilibre.",
                        "نطرح 3 من الكفتين للحفاظ على التوازن.",
                        "x+3-3=7-3", "x", "7 − 3", 70),
                frame("Simplifier", "بسّط",
                        "Il reste x = 4 : la balance est toujours horizontale.",
                        "يبقى x = 4: الميزان ما زال أفقياً.",
                        "x=4", "x", "4", 100),
                frame("Vérifier", "تحقّق",
                        "On remplace x par 4 : 4 + 3 = 7, c’est vrai.",
                        "نعوّض x بـ 4: 4 + 3 = 7، وهذا صحيح.",
                        "4+3=7", "4 + 3", "7", 100));
    }

    private static Demo geometryDemo() {
        return demo("GEOMETRY", "Calculer l’aire d’un rectangle", "حساب مساحة مستطيل",
                frame("Mesurer", "قِس",
                        "Le rectangle mesure 4 cm de long et 3 cm de large.",
                        "طول المستطيل 4 سم وعرضه 3 سم.",
                        "L=4,\\ l=3", "4 cm", "3 cm", 25),
                frame("Choisir", "اختر",
                        "L’aire se calcule par longueur × largeur.",
                        "المساحة = الطول × العرض.",
                        "A=L\\times l", "L × l", "A", 55),
                frame("Calculer", "احسب",
                        "On remplace : 4 × 3 = 12 cm².",
                        "نعوّض: 4 × 3 = 12 سم².",
                        "A=12\\ \\mathrm{cm}^2", "4 × 3", "12 cm²", 100),
                frame("Vérifier", "تحقّق",
                        "Le périmètre, lui, vaut 2 × (4 + 3) = 14 cm.",
                        "أما المحيط فهو 2 × (4 + 3) = 14 سم.",
                        "P=14\\ \\mathrm{cm}", "2 × (4 + 3)", "14 cm", 100));
    }

    private static Demo functionDemo() {
        return demo("FUNCTION", "Une machine à calculer", "آلة حسابية",
                frame("Entrée", "المدخل",
                        "On choisit x = 3 comme entrée de la fonction f(x) = 2x + 1.",
                        "نختار x = 3 مدخلاً للدالة f(x) = 2x + 1.",
                        "x=3", "x = 3", "f", 20),
                frame("Multiplier", "اضرب",
                        "La machine calcule d’abord 2 × 3 = 6.",
                        "تحسب الآلة أولاً 2 × 3 = 6.",
                        "2\\times 3=6", "2 × 3", "6", 55),
                frame("Ajouter", "أضف",
                        "Puis elle ajoute 1 : 6 + 1 = 7.",
                        "ثم تضيف 1: 6 + 1 = 7.",
                        "6+1=7", "6 + 1", "7", 85),
                frame("Sortie", "المخرج",
                        "La sortie est f(3) = 7.",
                        "المخرج هو f(3) = 7.",
                        "f(3)=7", "f(3)", "7", 100));
    }

    private static Demo trigonometryDemo() {
        return demo("TRIGONOMETRY", "Résoudre un triangle rectangle", "حل مثلث قائم",
                frame("Observer", "لاحظ",
                        "Les deux côtés de l’angle droit mesurent 3 et 4.",
                        "ضلعا الزاوية القائمة طولهما 3 و 4.",
                        "a=3,\\ b=4", "3", "4", 25),
                frame("Pythagore", "فيثاغورس",
                        "Dans un triangle rectangle : a² + b² = c².",
                        "في المثلث القائم: a² + b² = c².",
                        "a^2+b^2=c^2", "a² + b²", "c²", 60),
                frame("Calculer", "احسب",
                        "3² + 4² = 9 + 16 = 25.",
                        "3² + 4² = 9 + 16 = 25.",
                        "3^2+4^2=25", "9 + 16", "25", 85),
                frame("Conclure", "استنتج",
                        "L’hypoténuse mesure donc c = 5.",
                        "إذن طول الوتر c = 5.",
                        "c=5", "√25", "5", 100));
    }

    private static Demo arithmeticDemo() {
        return demo("ARITHMETIC", "Décomposer pour calculer", "فكّك لتحسب",
                frame("Décomposer", "فكّك",
                        "On décompose 37 en 35 + 2 pour compléter une dizaine.",
                        "نفكّك 37 إلى 35 + 2 لإكمال عشرة.",
                        "37=35+2", "37", "35 + 2", 30),
                frame("Regrouper", "اجمع",
                        "On additionne d’abord 25 + 35 = 60.",
                        "نجمع أولاً 25 + 35 = 60.",
                        "25+35=60", "25 + 35", "60", 65),
                frame("Ajouter", "أضف",
                        "Puis on rajoute le reste : 60 + 2 = 62.",
                        "ثم نضيف الباقي: 60 + 2 = 62.",
                        "60+2=62", "60 + 2", "62", 100),
                frame("Vérifier", "تحقّق",
                        "Donc 25 + 37 = 62.",
                        "إذن 25 + 37 = 62.",
                        "25+37=62", "25 + 37", "62", 100));
    }

    private static Demo generalDemo(String title, String description) {
        String safeTitle = title == null ? "" : title;
        String snippet = description == null || description.isBlank()
                ? "Cette ressource est étudiée étape par étape."
                : (description.length() > 160 ? description.substring(0, 160) + "…" : description);
        return demo("GENERAL", "Lire, repérer, appliquer", "اقرأ، حدّد، طبّق",
                frame("Lire", "اقرأ",
                        "On commence par le titre : " + safeTitle,
                        "نبدأ بالعنوان: " + safeTitle,
                        null, safeTitle, snippet, 20),
                frame("Repérer", "حدّد",
                        snippet, snippet, null, "Idée clé", "فكرة أساسية", 55),
                frame("Appliquer", "طبّق",
                        "On refait un exemple de la ressource étape par étape.",
                        "نعيد مثالاً من المورد خطوة بخطوة.",
                        null, "Exemple", "مثال", 80),
                frame("Vérifier", "تحقّق",
                        "On répond aux exercices pour confirmer la compréhension.",
                        "نجيب عن التمارين للتأكد من الفهم.",
                        null, "Exercices", "تمارين", 100));
    }

    private Generated fractions() {
        return new Generated(
                List.of(
                        step("intro", "Les fractions", "الكسور",
                                "Une fraction représente une ou plusieurs parties égales d’un tout. On l’écrit a/b : a est le numérateur et b le dénominateur.",
                                "الكسور تمثل جزءاً أو أجزاء متساوية من كلٍّ. نكتبها a/b: a هو البسط و b هو المقام.",
                                "\\frac{a}{b}"),
                        step("concept", "Comprendre le dénominateur", "فهم المقام",
                                "Le dénominateur indique en combien de parties égales le tout est partagé. Le numérateur indique combien de parties on prend.",
                                "المقام يدل على عدد الأجزاء المتساوية، والبسط يدل على عدد الأجزاء المأخوذة.",
                                "\\frac{3}{4}"),
                        step("example", "Fractions équivalentes", "كسور متكافئة",
                                "On obtient une fraction équivalente en multipliant le numérateur et le dénominateur par le même nombre non nul.",
                                "نحصل على كسر متكافئ بضرب البسط والمقام في العدد غير الصفري نفسه.",
                                "\\frac{1}{2}=\\frac{2}{4}")),
                List.of(
                        question(1, "Combien de quarts y a-t-il dans un entier ?", "كم ربعاً في الواحد الصحيح؟",
                                List.of("4", "2", "3", "8"), List.of("4", "2", "3", "8"), 0,
                                "Un entier coupé en 4 parts égales contient 4 quarts.",
                                "الواحد مقسوماً إلى 4 أجزاء متساوية يعطي 4 أرباع."),
                        question(2, "Quelle fraction est équivalente à 1/2 ?", "أي كسر يساوي 1/2؟",
                                List.of("2/4", "1/3", "3/4", "2/3"), List.of("2/4", "1/3", "3/4", "2/3"), 0,
                                "On multiplie le numérateur et le dénominateur par 2.",
                                "نضرب البسط والمقام في 2."),
                        question(3, "Dans 3/4, que représente le nombre 4 ?", "في 3/4 ماذا يمثل الرقم 4؟",
                                List.of("Le dénominateur", "Le numérateur", "Le résultat", "La somme"),
                                List.of("المقام", "البسط", "الناتج", "المجموع"), 0,
                                "Le 4 est le dénominateur : il indique 4 parties égales.",
                                "الرقم 4 هو المقام: يدل على 4 أجزاء متساوية.")));
    }

    private Generated equation() {
        return new Generated(
                List.of(
                        step("intro", "Qu’est-ce qu’une équation ?", "ما هي المعادلة؟",
                                "Une équation est une égalité avec une inconnue, souvent notée x. Résoudre, c’est trouver la valeur de x qui rend l’égalité vraie.",
                                "المعادلة مساواة تحتوي على مجهول غالباً يُرمز له بـ x. حلها يعني إيجاد قيمة x التي تحقق المساواة.",
                                "x+3=7"),
                        step("concept", "Garder la balance", "الحفاظ على التوازن",
                                "On peut ajouter ou soustraire le même nombre des deux côtés sans changer l’égalité.",
                                "يمكن إضافة أو طرح العدد نفسه من الطرفين دون تغيير المساواة.",
                                "x+3-3=7-3"),
                        step("example", "Un exemple pas à pas", "مثال خطوة بخطوة",
                                "x + 3 = 7 donne x = 4. Vérification : 4 + 3 = 7.",
                                "x + 3 = 7 يعطي x = 4. التحقق: 4 + 3 = 7.",
                                "x=4")),
                List.of(
                        question(1, "Si x + 5 = 12, combien vaut x ?", "إذا كان x + 5 = 12 فكم يساوي x؟",
                                List.of("7", "17", "6", "5"), List.of("7", "17", "6", "5"), 0,
                                "12 − 5 = 7, et 7 + 5 = 12.", "12 − 5 = 7، و 7 + 5 = 12."),
                        question(2, "Si 3x = 18, combien vaut x ?", "إذا كان 3x = 18 فكم يساوي x؟",
                                List.of("6", "15", "21", "3"), List.of("6", "15", "21", "3"), 0,
                                "On divise les deux membres par 3 : x = 6.", "نقسم الطرفين على 3: x = 6."),
                        question(3, "Que faut-il faire pour résoudre x − 4 = 9 ?", "ماذا نفعل لحل x − 4 = 9؟",
                                List.of("Ajouter 4 des deux côtés", "Soustraire 4 des deux côtés", "Multiplier par 4", "Diviser par 4"),
                                List.of("نضيف 4 للطرفين", "نطرح 4 من الطرفين", "نضرب في 4", "نقسم على 4"), 0,
                                "L’inverse de −4 est +4 : x = 13.", "عكس −4 هو +4: x = 13.")));
    }

    private Generated geometry() {
        return new Generated(
                List.of(
                        step("intro", "Formes et figures", "الأشكال والهندسة",
                                "La géométrie étudie les formes, leurs angles, leurs longueurs et leurs aires.",
                                "تدرس الهندسة الأشكال وزواياها وأطوالها ومساحاتها.",
                                null),
                        step("concept", "Le triangle", "المثلث",
                                "Un triangle a 3 côtés et 3 sommets. La somme de ses angles intérieurs vaut 180°.",
                                "للمثلث 3 أضلاع و3 رؤوس. مجموع زواياه الداخلية 180°.",
                                "180^{\\circ}"),
                        step("example", "Aire et périmètre", "المساحة والمحيط",
                                "L’aire d’un rectangle est longueur × largeur ; le périmètre est la somme de tous les côtés.",
                                "مساحة المستطيل = الطول × العرض، والمحيط هو مجموع كل الأضلاع.",
                                "A=L\\times l")),
                List.of(
                        question(1, "Quelle est la somme des angles d’un triangle ?", "كم مجموع زوايا المثلث؟",
                                List.of("180°", "90°", "360°", "270°"), List.of("180°", "90°", "360°", "270°"), 0,
                                "Les angles intérieurs d’un triangle totalisent toujours 180°.",
                                "مجموع الزوايا الداخلية للمثلث دائماً 180°."),
                        question(2, "Aire d’un rectangle de 4 cm sur 3 cm ?", "مساحة مستطيل طوله 4 سم وعرضه 3 سم؟",
                                List.of("12 cm²", "7 cm²", "14 cm²", "24 cm²"), List.of("12 سم²", "7 سم²", "14 سم²", "24 سم²"), 0,
                                "4 × 3 = 12 cm².", "4 × 3 = 12 سم²."),
                        question(3, "Périmètre d’un carré de côté 5 cm ?", "محيط مربع ضلعه 5 سم؟",
                                List.of("20 cm", "10 cm", "25 cm", "15 cm"), List.of("20 سم", "10 سم", "25 سم", "15 سم"), 0,
                                "4 × 5 = 20 cm.", "4 × 5 = 20 سم.")));
    }

    private Generated function() {
        return new Generated(
                List.of(
                        step("intro", "Une fonction, c’est quoi ?", "ما هي الدالة؟",
                                "Une fonction associe à chaque nombre x un seul résultat noté f(x).",
                                "الدالة تربط كل عدد x بنتيجة واحدة فقط تُكتب f(x).",
                                "f(x)=2x+1"),
                        step("concept", "Entrée et sortie", "المدخل والمخرج",
                                "x est l’entrée et f(x) la sortie. On remplace x par la valeur choisie.",
                                "x هو المدخل و f(x) هو المخرج. نعوّض x بالقيمة المختارة.",
                                null),
                        step("example", "Calculer une image", "حساب الصورة",
                                "Avec f(x) = 2x + 1, on calcule f(3) = 2×3 + 1 = 7.",
                                "مع f(x) = 2x + 1 نحسب f(3) = 2×3 + 1 = 7.",
                                "f(3)=7")),
                List.of(
                        question(1, "Avec f(x)=2x+1, combien vaut f(3) ?", "مع f(x)=2x+1 كم تساوي f(3)؟",
                                List.of("7", "6", "5", "9"), List.of("7", "6", "5", "9"), 0,
                                "2×3 + 1 = 7.", "2×3 + 1 = 7."),
                        question(2, "Avec f(x)=2x+1, combien vaut f(0) ?", "مع f(x)=2x+1 كم تساوي f(0)؟",
                                List.of("1", "0", "2", "-1"), List.of("1", "0", "2", "-1"), 0,
                                "2×0 + 1 = 1.", "2×0 + 1 = 1."),
                        question(3, "Avec f(x)=2x+1, quel x donne f(x)=9 ?", "مع f(x)=2x+1 أي x يعطي f(x)=9؟",
                                List.of("4", "5", "3", "9"), List.of("4", "5", "3", "9"), 0,
                                "2x + 1 = 9 donc 2x = 8 et x = 4.", "2x + 1 = 9 إذن 2x = 8 و x = 4.")));
    }

    private Generated trigonometry() {
        return new Generated(
                List.of(
                        step("intro", "Dans le triangle rectangle", "في المثلث القائم",
                                "La trigonométrie relie les angles et les longueurs d’un triangle rectangle.",
                                "تربط حساب المثلثات بين الزوايا والأطوال في المثلث القائم.",
                                "\\sin(\\theta)=\\frac{oppose}{hypotenuse}"),
                        step("concept", "Sin, cos, tan", "الجيب والجتان والظل",
                                "sin = opposé/hypoténuse, cos = adjacent/hypoténuse, tan = opposé/adjacent.",
                                "الجيب = المقابل/الوتري، الجتان = المجاور/الوتري، الظل = المقابل/المجاور.",
                                null),
                        step("example", "Valeurs utiles", "قيم مفيدة",
                                "sin 30° = 1/2 et cos 60° = 1/2. Dans un triangle rectangle : a² + b² = c².",
                                "جيب 30° = 1/2 وجتان 60° = 1/2. في المثلث القائم: a² + b² = c².",
                                "a^2+b^2=c^2")),
                List.of(
                        question(1, "Combien vaut sin 30° ?", "كم يساوي جيب 30°؟",
                                List.of("1/2", "1", "0", "√2/2"), List.of("1/2", "1", "0", "√2/2"), 0,
                                "sin 30° = 1/2.", "جيب 30° = 1/2."),
                        question(2, "Dans un triangle 3-4-5, combien mesure l’hypoténuse ?", "في مثلث 3-4-5 كم طول الوتر؟",
                                List.of("5", "4", "3", "7"), List.of("5", "4", "3", "7"), 0,
                                "3² + 4² = 25 = 5².", "3² + 4² = 25 = 5²."),
                        question(3, "Quelle formule donne cos θ ?", "ما الصيغة التي تعطي جتان θ؟",
                                List.of("adjacent/hypoténuse", "opposé/hypoténuse", "opposé/adjacent", "hypoténuse/adjacent"),
                                List.of("المجاور/الوتري", "المقابل/الوتري", "المقابل/المجاور", "الوتري/المجاور"), 0,
                                "cos θ = adjacent / hypoténuse.", "جتان θ = المجاور / الوتري.")));
    }

    private Generated arithmetic() {
        return new Generated(
                List.of(
                        step("intro", "Les quatre opérations", "العمليات الأربع",
                                "Addition, soustraction, multiplication et division sont les bases du calcul.",
                                "الجمع والطرح والضرب والقسمة هي أساس الحساب.",
                                null),
                        step("concept", "Priorité des opérations", "أولوية العمليات",
                                "On effectue d’abord les parenthèses, puis multiplications et divisions, puis additions et soustractions.",
                                "نبدأ بالأقواس، ثم الضرب والقسمة، ثم الجمع والطرح.",
                                null),
                        step("example", "Calculer vite", "حساب سريع",
                                "7 × 8 = 56 et 144 ÷ 12 = 12. Décomposez les nombres pour calculer plus vite.",
                                "7 × 8 = 56 و 144 ÷ 12 = 12. حلّل الأعداد لتحسب أسرع.",
                                "7\\times 8=56")),
                List.of(
                        question(1, "7 × 8 = ?", "7 × 8 =؟",
                                List.of("56", "54", "64", "48"), List.of("56", "54", "64", "48"), 0,
                                "7 × 8 = 56.", "7 × 8 = 56."),
                        question(2, "144 ÷ 12 = ?", "144 ÷ 12 =؟",
                                List.of("12", "11", "14", "24"), List.of("12", "11", "14", "24"), 0,
                                "12 × 12 = 144.", "12 × 12 = 144."),
                        question(3, "25 + 37 = ?", "25 + 37 =؟",
                                List.of("62", "52", "72", "63"), List.of("62", "52", "72", "63"), 0,
                                "25 + 35 + 2 = 62.", "25 + 35 + 2 = 62.")));
    }

    private Generated general(String title, String description, boolean ar, ExternalResource.Cycle cycle) {
        String safeTitle = title == null ? "" : title;
        String snippet = description == null || description.isBlank()
                ? (ar ? "هذا المورد مختار لمساعدتك على التعلم خطوة بخطوة."
                      : "Cette ressource est proposée pour vous aider à apprendre étape par étape.")
                : (description.length() > 240 ? description.substring(0, 240) + "…" : description);
        List<String> cycles = List.of("PRIMARY", "BASIC", "SECONDARY");
        List<String> cycleLabelsFr = List.of("Primaire", "Collège", "Lycée");
        List<String> cycleLabelsAr = List.of("ابتدائي", "إعدادي", "ثانوي");
        int correct = cycle == null ? 0 : cycle.ordinal();
        return new Generated(
                List.of(
                        step("intro", ar ? "اكتشف: " + safeTitle : "Découvrir : " + safeTitle,
                                ar ? "اكتشف: " + safeTitle : "Découvrir : " + safeTitle,
                                snippet, snippet, null),
                        step("concept", ar ? "الفكرة الأساسية" : "Idée clé",
                                ar ? "الفكرة الأساسية" : "Idée clé",
                                ar ? "اقرأ العنوان والأمثلة الواردة في المورد، ثم حدد الكلمات والمفاهيم الأساسية."
                                   : "Lisez le titre et les exemples de la ressource, puis repérez les mots et les idées essentiels.",
                                ar ? "اقرأ العنوان والأمثلة الواردة في المورد، ثم حدد الكلمات والمفاهيم الأساسية."
                                   : "Lisez le titre et les exemples de la ressource, puis repérez les mots et les idées essentiels.",
                                null),
                        step("example", ar ? "طريقة العمل" : "Méthode",
                                ar ? "طريقة العمل" : "Méthode",
                                ar ? "Observe un exemple, refais-le étape par étape, puis vérifie avec les questions ci-dessous."
                                   : "Observez un exemple, refaites-le étape par étape, puis vérifiez avec les questions ci-dessous.",
                                ar ? "Observe un exemple, refais-le étape par étape, puis vérifie avec les questions ci-dessous."
                                   : "Observez un exemple, refaites-le étape par étape, puis vérifiez avec les questions ci-dessous.",
                                null)),
                List.of(
                        question(1, ar ? "ما مصدر هذا المورد؟" : "D’où provient cette ressource ?",
                                ar ? "ما مصدر هذا المورد؟" : "D’où provient cette ressource ?",
                                List.of("Wikipédia / Wikimedia", "Éditeur privé", "Vidéo YouTube", "Forum"),
                                List.of("ويكيبيديا / ويكيميديا", "ناشر خاص", "فيديو يوتيوب", "منتدى"), 0,
                                ar ? "المورد من ويكيميديا." : "La ressource provient de Wikimedia.",
                                ar ? "المورد من ويكيميديا." : "La ressource provient de Wikimedia."),
                        question(2, ar ? "ما ترخيص هذا المورد؟" : "Quelle licence couvre cette ressource ?",
                                ar ? "ما ترخيص هذا المورد؟" : "Quelle licence couvre cette ressource ?",
                                List.of("CC BY-SA 4.0", "Tous droits réservés", "MIT", "Domaine public"),
                                List.of("CC BY-SA 4.0", "جميع الحقوق محفوظة", "MIT", "الملك العام"), 0,
                                ar ? "الترخيص هو CC BY-SA 4.0 مع واجب نسب المحتوى." : "La licence est CC BY-SA 4.0 avec attribution.",
                                ar ? "الترخيص هو CC BY-SA 4.0 مع واجب نسب المحتوى." : "La licence est CC BY-SA 4.0 avec attribution."),
                        question(3, ar ? "لأي مستوى صُنّف هذا المورد؟" : "Pour quel niveau cette ressource a-t-elle été classée ?",
                                ar ? "لأي مستوى صُنّف هذا المورد؟" : "Pour quel niveau cette ressource a-t-elle été classée ?",
                                cycleLabelsFr, cycleLabelsAr, correct,
                                ar ? "راجع المستوى المكتوب على بطاقة المورد." : "Reportez-vous au niveau indiqué sur la carte de la ressource.",
                                ar ? "راجع المستوى المكتوب على بطاقة المورد." : "Reportez-vous au niveau indiqué sur la carte de la ressource.")));
    }
}
