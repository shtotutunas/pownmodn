import common.ModUtils;
import org.apache.commons.math3.primes.Primes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

class ModUtilsTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testModInverse() {
        int lowBitLength = 10;
        int highBitLength = 25;
        int testsPerBitLength = 20;

        long startTime = System.currentTimeMillis();
        BigInteger[] tests = TestUtils.generateTestNumbers(lowBitLength, highBitLength, testsPerBitLength, true, false, new Random(777));
        for (BigInteger a : tests) {
            for (BigInteger b : tests) {
                if (a.gcd(b).equals(BigInteger.ONE)) {
                    Assertions.assertEquals(a.modInverse(b), ModUtils.modInverse(a, b), () -> "a=" + a + ";  b=" + b);
                }
            }
        }
        log.info("OK - tested for {}x{} number pairs in {}ms", tests.length, tests.length, System.currentTimeMillis() - startTime);

    }

    @Test
    public void testDivideLinearSum() {
        long startTime = System.currentTimeMillis();
        int limit = 100;
        for (int p = 2; p <= limit; p++) {
            if (Primes.isPrime(p)) {
                for (int A = 1; A <= limit; A++) {
                    for (int B = 0; B < A; B++) {
                        testDivideLinearSum(A, B, p);
                    }
                }
            }
        }
        log.info("OK - tested for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    @Disabled
    public void testDivideLinearSumDebug() {
        testDivideLinearSum(2, 1, 3);
    }

    @Test
    public void testMerge() {
        long startTime = System.currentTimeMillis();
        int limit = 40;
        for (int a0 = 1; a0 <= limit; a0++) {
            for (int b0 = 0; b0 < a0; b0++) {
                for (int a1 = a0; a1 <= limit; a1++) {
                    for (int b1 = 0; b1 < a1; b1++) {
                        testMerge(a0, b0, a1, b1);
                    }
                }
            }
        }
        log.info("OK - tested for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    @Disabled
    public void testMergeDebug() {
        testMerge(2, 0, 4, 2);
    }

    private static void testDivideLinearSum(int A, int B, int p) {
        BigInteger[] ab = ModUtils.divideLinearSum(BigInteger.valueOf(A), BigInteger.valueOf(B), BigInteger.valueOf(p));
        if (ab == null) {
            for (long x = 0; x <= p; x++) {
                if ((A*x + B) % p == 0) {
                    throw new IllegalStateException("A=" + A + ", B=" + B + ", p=" + p + ", x=" + x);
                }
            }
            return;
        }
        long a = ab[0].longValueExact();
        long b = ab[1].longValueExact();
        long y = 0;
        for (long x = 0; x < 4L*p; x++) {
            long L = A*x + B;
            if (L%p == 0) {
                long R = p*(a*y + b);
                if (L != R) {
                    throw new IllegalStateException("A=" + A + ", B=" + B + ", p=" + p + ", x=" + x);
                }
                y++;
            }
        }
        if (y < 4) {
            throw new IllegalStateException("A=" + A + ", B=" + B + ", p=" + p + ", y=" + y);
        }
    }

    private static void testMerge(int a0, int b0, int a1, int b1) {
        BigInteger[] res1 = ModUtils.merge(BigInteger.valueOf(a0), BigInteger.valueOf(b0), BigInteger.valueOf(a1), BigInteger.valueOf(b1));
        BigInteger[] res2 = ModUtils.merge(BigInteger.valueOf(a1), BigInteger.valueOf(b1), BigInteger.valueOf(a0), BigInteger.valueOf(b0));
        if (!Arrays.equals(res1 ,res2)) {
            throw new IllegalStateException("a0=" + a0 + ", b0=" + b0 + ", a1=" + a1 + ", b1=" + b1);
        }

        if (res1 == null) {
            for (long x = 0; x <= a0; x++) {
                if ((a1*x+b1)%a0 == b0) {
                    throw new IllegalStateException("a0=" + a0 + ", b0=" + b0 + ", a1=" + a1 + ", b1=" + b1);
                }
            }
            return;
        }

        long A = res1[0].longValueExact();
        long B = res1[1].longValueExact();
        long y = 0;
        for (long x = 0; x <= 4L*a0; x++) {
            long L = a1*x+b1;
            if ((a1*x+b1)%a0 == b0) {
                long R = A*y + B;
                if (L != R) {
                    throw new IllegalStateException("a0=" + a0 + ", b0=" + b0 + ", a1=" + a1 + ", b1=" + b1);
                }
                y++;
            }
        }
        if (y < 4) {
            throw new IllegalStateException("a0=" + a0 + ", b0=" + b0 + ", a1=" + a1 + ", b1=" + b1);
        }
    }

    @Test
    public void testRebalanceDivisors() {
        long startTime = System.currentTimeMillis();
        int limit = 1000;
        for (int a = 1; a <= limit; a++) {
            for (int b = 1; b <= limit; b++) {
                testRebalanceDivisors(a, b);
            }
        }
        log.info("OK - tested for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    @Disabled
    public void testRebalanceDivisorsDebug() {
        testRebalanceDivisors(2, 4);
    }

    private static void testRebalanceDivisors(int a, int b) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger B = BigInteger.valueOf(b);
        BigInteger[] ab = ModUtils.rebalanceDivisors(A, B, A.gcd(B));
        String msg = "A=" + a + ", B=" + b + ": a=" + ab[0] + ", b=" + ab[1];
        Assertions.assertEquals(BigInteger.ZERO, A.mod(ab[0]), msg);
        Assertions.assertEquals(BigInteger.ZERO, B.mod(ab[1]), msg);
        Assertions.assertEquals(BigInteger.ONE, ab[0].gcd(ab[1]), msg);
        Assertions.assertEquals(A.multiply(B).divide(A.gcd(B)), ab[0].multiply(ab[1]), msg);
    }

    @Test
    @Disabled
    public void testModPowTimings() {
        int bitLength = 52;
        int numCnt = 100;
        testModPowTimings(bitLength, numCnt, false); // warmup
        testModPowTimings(bitLength, numCnt, true);
    }

    private void testModPowTimings(int bitLength, int numCnt, boolean logTimings) {
        BigInteger[] A = TestUtils.generateTestNumbers(bitLength, bitLength, numCnt, false, false, new Random(777));
        long[] a = Arrays.stream(A).mapToLong(BigInteger::longValueExact).toArray();

        BigInteger[][][] R = new BigInteger[numCnt][numCnt][numCnt];
        long[][][] r = new long[numCnt][numCnt][numCnt];
        long startTime;
        long runTime;
        long total = (long) numCnt * numCnt * (numCnt-1);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < numCnt; i++) {
            for (int j = 0; j < numCnt; j++) {
                for (int k = 0; k < numCnt; k++) {
                    if (i != k) {
                        r[i][j][k] = ModUtils.pow(a[i], a[j], a[k]);
                    }
                }
            }
        }
        runTime = System.currentTimeMillis() - startTime;
        if (logTimings) {
            log.info("Long implementation: {}ms --- {}ns per operation", runTime, String.format("%.2f", runTime * 1e6 / total));
        }

        startTime = System.currentTimeMillis();
        for (int i = 0; i < numCnt; i++) {
            for (int j = 0; j < numCnt; j++) {
                for (int k = 0; k < numCnt; k++) {
                    if (i != k) {
                        R[i][j][k] = A[i].modPow(A[j], A[k]);
                    }
                }
            }
        }
        runTime = System.currentTimeMillis() - startTime;
        if (logTimings) {
            log.info("BigInteger implementation: {}ms --- {}ns per operation", runTime, String.format("%.2f", runTime * 1e6 / total));
        }

        for (int i = 0; i < numCnt; i++) {
            for (int j = 0; j < numCnt; j++) {
                for (int k = 0; k < numCnt; k++) {
                    if (i != k) {
                        Assertions.assertEquals(R[i][j][k].longValueExact(), r[i][j][k]);
                    }
                }
            }
        }
    }

}
