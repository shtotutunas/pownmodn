import common.Modules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class SolverTest {

    @Test
    public void testSolve() {
        int[] bases = new int[] {2, 3, 4, 5, 6};
        int absT = 30;
        int ceil = 5000000;
        for (int base : bases) {
            testSolve(base, absT, ceil);
        }
    }

    public void testSolve(int base, int absT, int ceil) {
        Map<Integer, NavigableSet<BigInteger>> solutions = new HashMap<>();
        for (int i = 1; i <= ceil; i++) {
            int r = (int) Modules.pow(base, i, i);
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

        for (int t = -absT; t <= absT; t++) {
            Launch launch = Launch.solverSimple(100, 10000L, 100000L, 100, 20);
            Solver solver = new Solver(base, t, BigInteger.valueOf(ceil), launch, null, 4,
                    100, 10, 1<<23, 4, Long.MAX_VALUE, false);
            solver.solve();

            BigInteger T = BigInteger.valueOf(t);
            TreeSet<BigInteger> actual = new TreeSet<>(launch.getSolutions());
            actual.forEach(x -> Assertions.assertEquals(Modules.pow(BigInteger.valueOf(base), x, x), T.mod(x),
                    "base=" + base + ", target=" + T + ", x=" + x));
            Assertions.assertEquals(solutions.getOrDefault(t, new TreeSet<>()), actual.headSet(BigInteger.valueOf(ceil), true),
                    "base=" + base + ", target=" + t);
        }
    }
}
