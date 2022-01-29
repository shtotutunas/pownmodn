import common.Common;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scan.ModPowCalculator;
import scan.ModPowCalculatorFactory;

import java.math.BigInteger;
import java.util.Random;

public class ModPowCalculatorTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testCalculate() {
        BigInteger[] bases = new BigInteger[] {BigInteger.TWO, BigInteger.valueOf(3), BigInteger.TEN, Common.MAX_INT,
                Common.MAX_LONG_SQRT_BIG, Common.MAX_LONG, BigInteger.TEN.pow(30)};
        int lowBitLength = 6;
        int highBitLength = 30;
        int testsPerBitLength = 15;

        long startTime = System.currentTimeMillis();
        BigInteger[] tests = TestUtils.generateTestNumbers(lowBitLength, highBitLength, testsPerBitLength, true, true, new Random(777));
        for (BigInteger base : bases) {
            ModPowCalculatorFactory factory = new ModPowCalculatorFactory(base);
            for (BigInteger exp : tests) {
                ModPowCalculator calculator = factory.createCalculator(exp);
                for (BigInteger mod : tests) {
                    if (mod.signum() > 0) {
                        BigInteger expected = base.modPow(exp, mod);
                        BigInteger actual = calculator.calculate(mod);
                        Assertions.assertEquals(expected, actual, () -> "base=" + base + ";  exp=" + exp + ";  mod=" + mod);
                    }
                }
            }
        }
        log.info("OK - tested for {} bases and {}x{} exp-mod pairs in {}ms", bases.length, tests.length, tests.length, System.currentTimeMillis() - startTime);
    }
}
