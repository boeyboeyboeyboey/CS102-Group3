package autograder.config;

import autograder.model.AssessmentDefinition;
import autograder.model.QuestionDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Reads the assessment and its questions from a properties file. */
final class AssessmentConfigLoader {
    private AssessmentConfigLoader() {
    }

    static AssessmentDefinition load(Properties properties, Path baseDirectory,
                                     Pattern questionDirectoryPattern,
                                     Pattern javaSourcePattern) throws IOException {
        String assessmentId = PropertyParsers.required(properties, "assessment.id");
        String assessmentName = PropertyParsers.required(properties, "assessment.name");
        List<String> questionIds = PropertyParsers.parseNames(
                PropertyParsers.required(properties, "assessment.question.ids"),
                "assessment.question.ids");
        List<QuestionDefinition> questions = new ArrayList<>();

        for (String questionId : questionIds) {
            questions.add(loadQuestion(properties, baseDirectory, questionDirectoryPattern,
                    javaSourcePattern, questionId));
        }
        AssessmentDefinition assessment = new AssessmentDefinition(assessmentId, assessmentName, questions);
        return canonicalizeAssessment(assessment);
    }

    private static QuestionDefinition loadQuestion(Properties properties, Path baseDirectory,
                                                   Pattern questionDirectoryPattern,
                                                   Pattern javaSourcePattern, String questionId) {
        String propertyPrefix = "assessment.question." + questionId + ".";
        String folder = PropertyParsers.required(properties, propertyPrefix + "folder");
        String sourceFile = PropertyParsers.required(properties, propertyPrefix + "source.file");
        validateQuestionNames(folder, sourceFile, questionDirectoryPattern, javaSourcePattern);
        Path testerFile = ConfigPathPolicy.resolvePath(baseDirectory,
                PropertyParsers.required(properties, propertyPrefix + "tester.file"));
        int caseCount = PropertyParsers.positiveInt(properties, propertyPrefix + "case.count");
        double pointsPerCase = PropertyParsers.positiveDouble(properties,
                propertyPrefix + "points.per.case");
        List<Path> supportFiles = ConfigPathPolicy.pathList(baseDirectory,
                properties.getProperty(propertyPrefix + "support.files"));
        List<Path> fixtures = ConfigPathPolicy.pathList(baseDirectory,
                properties.getProperty(propertyPrefix + "fixtures"));
        List<String> companionSourceFiles = PropertyParsers.parseOptionalNames(
                properties.getProperty(propertyPrefix + "companion.source.files"));
        validateCompanionSources(companionSourceFiles, javaSourcePattern);
        return new QuestionDefinition(questionId, folder, sourceFile, testerFile,
                caseCount, pointsPerCase, supportFiles, fixtures, companionSourceFiles);
    }

    private static void validateQuestionNames(String folder, String sourceFile,
                                              Pattern questionDirectoryPattern,
                                              Pattern javaSourcePattern) {
        if (!questionDirectoryPattern.matcher(folder).matches()) {
            throw new IllegalArgumentException("assessment folder does not match its pattern: " + folder);
        }
        Path sourcePath = Path.of(sourceFile);
        if (sourcePath.isAbsolute() || sourcePath.getNameCount() != 1) {
            throw new IllegalArgumentException("assessment source file must be a file name: " + sourceFile);
        }
        if (!javaSourcePattern.matcher(sourceFile).matches()) {
            throw new IllegalArgumentException("assessment source file does not match its pattern: "
                    + sourceFile);
        }
    }

    private static void validateCompanionSources(List<String> companionSourceFiles,
                                                 Pattern javaSourcePattern) {
        for (String companionSourceFile : companionSourceFiles) {
            Path companionPath = Path.of(companionSourceFile);
            if (companionPath.isAbsolute() || companionPath.getNameCount() != 1
                    || !javaSourcePattern.matcher(companionSourceFile).matches()) {
                throw new IllegalArgumentException("invalid companion source file: "
                        + companionSourceFile);
            }
        }
    }

    private static AssessmentDefinition canonicalizeAssessment(AssessmentDefinition assessment)
            throws IOException {
        List<QuestionDefinition> questions = new ArrayList<>();
        for (QuestionDefinition question : assessment.getQuestions()) {
            List<Path> supportFiles = new ArrayList<>();
            for (Path path : question.getSupportFiles()) {
                supportFiles.add(ConfigPathPolicy.canonicalize(path));
            }
            List<Path> fixtures = new ArrayList<>();
            for (Path path : question.getFixtures()) {
                fixtures.add(ConfigPathPolicy.canonicalize(path));
            }
            questions.add(new QuestionDefinition(question.getId(), question.getFolder(),
                    question.getSourceFile(), ConfigPathPolicy.canonicalize(question.getTesterFile()),
                    question.getCaseCount(), question.getPointsPerCase(), supportFiles,
                    fixtures, question.getCompanionSourceFiles()));
        }
        return new AssessmentDefinition(assessment.getId(), assessment.getName(), questions);
    }
}
