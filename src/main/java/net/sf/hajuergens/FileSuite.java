package net.sf.hajuergens;

import org.junit.runner.Runner;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.io.File;
import java.lang.annotation.*;
import java.text.MessageFormat;
import java.util.*;

public class FileSuite extends Suite {
    private static final ParametersRunnerFactory DEFAULT_FACTORY = new BlockJUnit4ClassRunnerWithParametersFactory();
    private static final List<Runner> NO_RUNNERS = Collections.emptyList();

    private final List<Runner> runners;
    private final List<Object> params;

    public FileSuite(TestWithParameters test) throws Throwable {
        super(InnerParameterizedTest.class, NO_RUNNERS);

        Parameterized.Parameters parameters = this.getParametersMethod().getAnnotation(Parameterized.Parameters.class);

        params = test.getParameters();

        File file = new File(params.get(0).toString());
        if (!file.exists()) throw new RuntimeException("The file " + file + " does not exists.");

        List<Object> primaryKeyList = CustomParameterizedSuite.getPrimaryKeyList(file);

        ParametersRunnerFactory runnerFactory = getParametersRunnerFactory(InnerParameterizedTest.class);

        this.runners = Collections.unmodifiableList(this.createRunnersForParameters(primaryKeyList, parameters.name(), runnerFactory));
    }

    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    private static TestWithParameters createTestWithParameters(TestClass testClass, String pattern, int index, Object[] parameters) {
        String finalPattern = pattern.replaceAll("\\{index\\}", Integer.toString(index));
        String name = MessageFormat.format(finalPattern, parameters);
        return new TestWithParameters("[" + name + "]", testClass, Arrays.asList(parameters));
    }

    protected String getName() {
        File file = new File(params.get(0).toString());
        String fName = removeExtension(file.getName());
        String className = getTestClass().getName().substring(getTestClass().getName().lastIndexOf(".") + 1);
        String name = MessageFormat.format("{0}->\"{1}\"", className, fName);
        return name;
    }

    private ParametersRunnerFactory getParametersRunnerFactory(Class<?> klass) throws InstantiationException, IllegalAccessException {
        Parameterized.UseParametersRunnerFactory annotation = klass.getAnnotation(Parameterized.UseParametersRunnerFactory.class);
        if (annotation == null) {
            return DEFAULT_FACTORY;
        } else {
            Class factoryClass = annotation.value();
            return (ParametersRunnerFactory) factoryClass.newInstance();
        }
    }

    private List<Runner> createRunnersForParameters(Iterable<Object> allParameters, String namePattern, ParametersRunnerFactory runnerFactory) throws InitializationError, Exception {
        try {
            List list = this.createTestsForParameters(allParameters, namePattern);
            ArrayList runners = new ArrayList();

            for (Object anE : list) {
                TestWithParameters test = (TestWithParameters) anE;
                runners.add(runnerFactory.createRunnerForTestWithParameters(test));
            }

            return runners;
        } catch (ClassCastException var8) {
            throw this.parametersMethodReturnedWrongType();
        }
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        List methods = this.getTestClass().getAnnotatedMethods(Parameterized.Parameters.class);
        Iterator it = methods.iterator();

        FrameworkMethod each;
        do {
            if (!it.hasNext()) {
                throw new Exception("No public static parameters method on class " + this.getTestClass().getName());
            }

            each = (FrameworkMethod) it.next();
        } while (!each.isStatic() || !each.isPublic());

        return each;
    }

    private TestWithParameters createTestWithNotNormalizedParameters(String pattern, int index, Object parametersOrSingleParameter) {
        Object[] parameters = parametersOrSingleParameter instanceof Object[] ? (Object[]) parametersOrSingleParameter : new Object[]{parametersOrSingleParameter};
        return createTestWithParameters(this.getTestClass(), pattern, index, parameters);
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        String className = this.getTestClass().getName();
        String methodName = this.getParametersMethod().getName();
        String message = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.", className, methodName);
        return new Exception(message);
    }

    private List<TestWithParameters> createTestsForParameters(Iterable<Object> allParameters, String namePattern) throws Exception {
        int i = 0;
        ArrayList children = new ArrayList();
        Iterator it = allParameters.iterator();

        while (it.hasNext()) {
            Object parametersOfSingleTest = it.next();
            children.add(this.createTestWithNotNormalizedParameters(namePattern, i++, parametersOfSingleTest));
        }

        return children;
    }

    private ParametersRunnerFactory getParameterizedTestClass(Class<?> klass) throws InstantiationException, IllegalAccessException {
        ParameterizedTestClass annotation = (ParameterizedTestClass) klass.getAnnotation(ParameterizedTestClass.class);
        if (annotation == null) {
            return DEFAULT_FACTORY;
        } else {
            Class factoryClass = annotation.value();
            return (ParametersRunnerFactory) factoryClass.newInstance();
        }
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Target({ElementType.TYPE})
    public @interface ParameterizedTestClass {
        Class<?> value() default InnerParameterizedTest.class;
    }

}
