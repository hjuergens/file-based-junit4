package net.sf.hajuergens;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(CustomParameterizedSuite.RunnerFactory.class)
public class CustomParameterizedSuite {

    static Pattern p = Pattern.compile("^[^~].*[.]xls[xm]?$");
    static private Predicate<Path> matchNameSchema = (Path entry) -> p.matcher(entry.toString()).matches();
    static private Predicate<Path> notDirectory = (Path entry) -> !entry.toFile().isDirectory();
    @Parameterized.Parameter
    public File file;
    public CustomParameterizedSuite(File i) {
        this.file = i;
    }

    /**
     * Get all the non empty lines from all the files with the specific extension, recursively.
     *
     * @param path the path to start recursion
     * @return list of lines
     */
    private static List<File> readAllFilesRecursively(final String path) {
        final List<File> lines = new ArrayList<>();
        try (final Stream<Path> pathStream = Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)) {
            pathStream
                    .filter((p) -> notDirectory.test(p))
                    .filter((p) -> matchNameSchema.test(p)).forEach(p -> lines.add(p.toFile()));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    @Parameterized.Parameters(name = "{index}: file={0}), type={1}")
    public static Iterable<File> data() {
        return readAllFilesRecursively("target/classes");
    }

    public static <T> List<T> copyIterator(Iterator<T> iter) {
        List<T> copy = new ArrayList<T>();
        while (iter.hasNext())
            copy.add(iter.next());
        return copy;
    }
    static List<Object> getPrimaryKeyList(File file) throws IOException, InvalidFormatException {
        List<Object> primaryKeyList = new LinkedList<>();

        Workbook wb = WorkbookFactory.create(file);
        Sheet sheet = wb.getSheet("Tabelle1");

        if (sheet == null) {
            String msg = MessageFormat.format("No sheet \"{0}\" in workbook {1}.", "Tabelle1", file);
            throw new RuntimeException(msg);
        }
        for (int rownum = sheet.getFirstRowNum() + 1; rownum < sheet.getLastRowNum()+1; rownum++) {
            Row row = sheet.getRow(rownum);
            Cell cell = row.getCell(0, Row.CREATE_NULL_AS_BLANK);
            if (cell.getStringCellValue().isEmpty()) continue;
            String id = cell.getStringCellValue();

            primaryKeyList.add(new Object[]{id, copyIterator(row.iterator())});
        }
        return primaryKeyList;
    }

    @Test
    public void test() {
        System.out.println(file);
    }

    public static class RunnerFactory implements ParametersRunnerFactory {
        @Override
        public org.junit.runner.Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
            try {
                return new FileSuite(test);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new InitializationError(throwable);
            }
        }
    }

}

