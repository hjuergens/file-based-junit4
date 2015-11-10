package net.sf.hajuergens;

import net.sf.hajuergens.FileSuite.SuiteClass;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.io.File;

/**
 *
 */
public class FileSuiteFactory implements ParametersRunnerFactory {
    private static Class<?> getAnnotatedClass(Class<?> klass) throws InitializationError {
        SuiteClass annotation = klass.getAnnotation(SuiteClass.class);
        if (annotation == null) {
            throw new InitializationError(String.format("class '%s' must have a SuiteClass annotation", klass.getName()));
        }
        return annotation.value();
    }

    /**
     * Creates a test suite with parameters containing a file.
     *
     * @param test a test with parameters
     * @return {@code FileSuite} if the test contains the file.
     * @throws InitializationError
     */
    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
        try {
            Class<?> javaClass = test.getTestClass().getJavaClass();
            Class<?> annotatedClass = getAnnotatedClass(javaClass);
            boolean containsFile = false;
            for (Object param : test.getParameters()) {
                containsFile |= param instanceof File;
            }
            if (!containsFile) throw new IllegalArgumentException("Test parameters do not contain any file.");
            return new FileSuite(annotatedClass, test);
        } catch (Throwable throwable) {
            throw new InitializationError(throwable);
        }
    }
}
