package net.sf.hajuergens;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(FileSuiteFactory.class)
@FileSuite.SuiteClass(value = InnerParameterizedTest.class)
public class CustomParameterizedSuite {

    static private final Pattern p = Pattern.compile("^[^~].*[.]xls[xm]?$");
    static private final Predicate<Path> matchNameSchema =
        (Path entry) -> p.matcher(entry.toString()).matches();
    static private final Predicate<Path> notDirectory =
        (Path entry) -> !entry.toFile().isDirectory();
    @Parameterized.Parameter private final File file;

    public CustomParameterizedSuite(File file) {
        this.file = file;
    }

    /**
     * Get all the non empty lines from all the files with the specific extension, recursively.
     *
     * @param path the path to start recursion
     * @return list of lines
     */
    private static Iterable<File> readAllFilesRecursively(String path) {
        List<File> lines = new ArrayList<>();
        try (final Stream<Path> pathStream = Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)) {
            pathStream.filter(notDirectory).filter(matchNameSchema)
                .forEach(p -> lines.add(p.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    @Parameterized.Parameters(name = "{index}: file={0}), type={1}")
    public static Iterable<File> data() {
        return readAllFilesRecursively("target/test-classes");
    }

    @Test
    public void test() {
        System.out.println(file);
    }

}

