import common.Common;
import common.TaskExecutor;
import factorization.Factorization;
import factorization.GmpEcmFactorizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

// This test requires VM argument -Dgmpecm.path=%path_to_gmp-ecm_executable_file%
public class GmpEcmFactorizerTest {

    @Test
    public void testFactorize() {
        testFactorize(5, 10000, 1000, 1, 10, 20, 30, 40, 50, 100);
        testFactorize(5, 100, 10, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100);
    }

    private void testFactorize(int testsCount, long B1, long C, int threads, int... bitSize) {
        Random random = new Random(Objects.hash(B1, C, Arrays.hashCode(bitSize)));
        TaskExecutor executor = TaskExecutor.create(threads);
        GmpEcmFactorizer factorizer = new GmpEcmFactorizer(B1, C, TaskExecutor.create(threads), 20);
        for (int i = 0; i < testsCount; i++) {
            BigInteger[] p = IntStream.of(bitSize).mapToObj(bs -> new BigInteger(bs, random).nextProbablePrime()).toArray(BigInteger[]::new);
            Factorization f = factorizer.factorize(Common.multiply(p));
            Assertions.assertEquals(Factorization.fromPrimeFactors(List.of(p)), f);
        }
        executor.shutdown();
    }

}
