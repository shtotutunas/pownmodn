import common.ModUtils;
import common.TaskExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import primes.GoodPrimes;
import primes.Primes;
import scan.ScanSieve;
import scan.Scanner;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class SolverTest {

    private final TaskExecutor executor = TaskExecutor.create(1);

    @Test
    public void testSolverSimple() {
        int[] bases = new int[] {2, 3, 4, 5, 6};
        int absT = 30;
        int ceil = 5000000;
        Primes primes = new Primes((int) Math.sqrt(ceil) + 1);
        for (int base : bases) {
            testSolverSimple(base, absT, ceil, primes);
        }
    }

    public void testSolverSimple(int base, int absT, int ceil, Primes primes) {
        Map<Integer, NavigableSet<BigInteger>> solutions = new HashMap<>();
        for (int i = 1; i <= ceil; i++) {
            int r = (int) ModUtils.pow(base, i, i);
            while (r <= absT) {
                solutions.computeIfAbsent(r, x -> new TreeSet<>()).add(BigInteger.valueOf(i));
                r += i;
            }
            r = (r%i)-i;
            while (r >= -absT) {
                solutions.computeIfAbsent(r, x -> new TreeSet<>()).add(BigInteger.valueOf(i));
                r -= i;
            }
        }

        for (int t = 5; t <= absT; t++) {
            Launch launch = Launch.solverSimple(100, 10000L, 100000L, 100, 20);
            GoodPrimes goodPrimes = GoodPrimes.generate(launch.getGoodPrimesBound(BigInteger.valueOf(ceil)), base, t, primes, executor);
            ScanSieve scanSieve = new ScanSieve(BigInteger.valueOf(base), BigInteger.valueOf(t), primes, Math.abs(t) <= 15);
            Scanner scanner = new Scanner(BigInteger.valueOf(base), t, scanSieve, executor, 1<<23, 4);
            Solver solver = new Solver(base, t, BigInteger.valueOf(ceil), launch, goodPrimes,
                    null, launch.getFactorizer(primes), scanner, Long.MAX_VALUE, false);
            solver.solve();

            BigInteger T = BigInteger.valueOf(t);
            TreeSet<BigInteger> actual = new TreeSet<>(launch.getSolutions());
            actual.forEach(x -> Assertions.assertEquals(BigInteger.valueOf(base).modPow(x, x), T.mod(x),
                    "base=" + base + ", target=" + T + ", x=" + x));
            Assertions.assertEquals(solutions.getOrDefault(t, new TreeSet<>()), actual.headSet(BigInteger.valueOf(ceil), true),
                    "base=" + base + ", target=" + t);
        }
    }
}
