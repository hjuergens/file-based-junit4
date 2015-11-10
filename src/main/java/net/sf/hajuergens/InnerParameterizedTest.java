package net.sf.hajuergens;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;

import java.util.List;

@Parameterized.UseParametersRunnerFactory(BlockJUnit4ClassRunnerWithParametersFactory.class)
public class InnerParameterizedTest {

    //@Parameterized.Parameter(value = 0)
    private final String param;
    private final List rowEntries;

    public InnerParameterizedTest(String str, List rowEntries) {
        this.param = str;
        this.rowEntries = rowEntries;
    }

    @Parameterized.Parameters(name = "{index}: param={0}")
    public static String[] parametersForTest() {
        return new String[]{"H", "I", "J", "L"};
    }

    @Test
    public void test() {
        System.out.println(param);
    }

}
