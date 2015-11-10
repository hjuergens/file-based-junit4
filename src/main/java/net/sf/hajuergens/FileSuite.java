package net.sf.hajuergens;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.junit.runner.Runner;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.text.MessageFormat;
import java.util.*;

import static java.text.MessageFormat.format;

class FileSuite extends Suite {
    private static final ParametersRunnerFactory DEFAULT_FACTORY = new BlockJUnit4ClassRunnerWithParametersFactory();
    private static final List<Runner> NO_RUNNERS = Collections.emptyList();

    private final List<Runner> runners;
    private final List<Object> params;

    /**
     * @param klass InnerParameterizedTest.class
     * @param test  a test with parameters containing a file
     * @throws Throwable
     */
    public FileSuite(Class<?> klass, TestWithParameters test) throws Throwable {
        super(klass, NO_RUNNERS);

        Parameterized.Parameters parameters = this.getParametersMethod().getAnnotation(Parameterized.Parameters.class);

        params = test.getParameters();

        final File file = new File(params.get(0).toString());
        if (!file.exists()) throw new RuntimeException(format("A file {0} does not exists in test parameters.", file));

        List<Object> primaryKeyList = getPrimaryKeyList(file);

        ParametersRunnerFactory runnerFactory = getParametersRunnerFactory(klass);

        runners = Collections.unmodifiableList(
            createRunnersForParameters(primaryKeyList, parameters.name(), runnerFactory));
    }

    private static <T> List<T> copyIterator(Iterator<T> iter) {
        List<T> copy = new ArrayList<>();
        while (iter.hasNext())
            copy.add(iter.next());
        return copy;
    }

    private static List<Object> getPrimaryKeyList(File file)
        throws IOException, InvalidFormatException {
        List<Object> primaryKeyList = new LinkedList<>();

        Workbook wb = WorkbookFactory.create(file);
        Sheet sheet = wb.getSheet("Tabelle1");

        if (sheet == null) {
            String msg = MessageFormat.format("No sheet \"{0}\" in workbook {1}.", "Tabelle1", file);
            throw new RuntimeException(msg);
        }
        for (int rownum = sheet.getFirstRowNum() + 1; rownum < sheet.getLastRowNum() + 1; rownum++) {
            Row row = sheet.getRow(rownum);
            Cell cell = row.getCell(0, Row.CREATE_NULL_AS_BLANK);
            if (cell.getStringCellValue().isEmpty()) continue;
            String id = cell.getStringCellValue();

            primaryKeyList.add(new Object[]{id, copyIterator(row.iterator())});
        }
        return primaryKeyList;
    }

    private static String removeExtension(String filename) {
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
        return MessageFormat.format("{0}->\"{1}\"", className, fName);
    }

    private ParametersRunnerFactory getParametersRunnerFactory(AnnotatedElement klass)
        throws InstantiationException, IllegalAccessException {
        UseParametersRunnerFactory annotation =
            klass.getAnnotation(UseParametersRunnerFactory.class);
        if (annotation == null) {
            return DEFAULT_FACTORY;
        } else {
            Class factoryClass = annotation.value();
            return (ParametersRunnerFactory) factoryClass.newInstance();
        }
    }

    private List<Runner> createRunnersForParameters(Iterable<Object> allParameters, String namePattern, ParametersRunnerFactory runnerFactory) throws Exception {
        try {
            List list = this.createTestsForParameters(allParameters, namePattern);
            List<Runner> runners = new ArrayList<>();

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

    private List<TestWithParameters> createTestsForParameters(Iterable<Object> allParameters,
        String namePattern) {
        int i = 0;
        List<TestWithParameters> children = new ArrayList<>();

        for (Object parametersOfSingleTest : allParameters) {
            children.add(this.createTestWithNotNormalizedParameters(namePattern, i++, parametersOfSingleTest));
        }

        return children;
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    /**
     * The <code>SuiteClass</code> annotation specifies the classes to be run when a class
     * annotated with <code>@RunWith(Suite.class)</code> is run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface SuiteClass {
        /**
         * @return the classes to be run
         */
        Class<?> value();
    }

}
