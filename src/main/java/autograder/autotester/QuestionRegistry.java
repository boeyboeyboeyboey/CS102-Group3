package autograder.autotester;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestionRegistry {
    private static final Map<String, Question> QUESTIONS = createQuestions();

    private QuestionRegistry() { }

    public static Question find(String id) {
        Question question = QUESTIONS.get(id);
        if (question == null) {
            throw new IllegalArgumentException("unknown randomized question: " + id
                    + "; registered questions: " + String.join(", ", QUESTIONS.keySet()));
        }
        return question;
    }

    public static List<String> questionIds() {
        return List.copyOf(QUESTIONS.keySet());
    }

    private static Map<String, Question> createQuestions() {
        Map<String, Question> questions = new LinkedHashMap<>();
        register(questions, new Q1());
        register(questions, new Q2());
        register(questions, new Q1a());
        register(questions, new Q1b());
        register(questions, new Q2a());
        register(questions, new Q2b());
        register(questions, new Q3());
        return Collections.unmodifiableMap(new LinkedHashMap<>(questions));
    }

    private static void register(Map<String, Question> questions, Question question) {
        question.validate();
        if (questions.putIfAbsent(question.getClassName(), question) != null) {
            throw new IllegalStateException("duplicate randomized question: " + question.getClassName());
        }
    }
}
