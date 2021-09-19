import common.Common;
import common.Modules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;
import primes.Restrictions;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RestrictionsTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testCalculate() {
        long startTime = System.currentTimeMillis();
        int limit = 150;
        Primes primes = new Primes(limit);
        for (int i = 0; i < primes.size(); i++) {
            int p = (int) primes.get(i);
            for (int base = 2; base < p; base++) {
                for (int target = -p+1; target < p; target++) {
                    Assertions.assertArrayEquals(calculateRestrictionSimple(base, p, target), Restrictions.calculateRestriction(base, p, target),
                            "base=" + base + ", p=" + p + ", target=" + target);
                }
            }
        }
        log.info("Checked restrictions for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    @Disabled
    public void testDebug() {
        log.info("{}", calculateRestrictionSimple(2, 6, 1));
        log.info("{}", Restrictions.calculateRestriction(2, 6, 1));
    }

    private static long[] calculateRestrictionSimple(int base, int p, int target) {
        target = (int) Common.mod(target, p);
        long x = Modules.pow(base, p, p);
        long t = x;
        int pos = -1;
        for (int i = 1; i <= p; i++) {
            if (t == target) {
                pos = i;
            }
            t = (t*x)%p;
            if (t == x) {
                return (pos >= 0) ? new long[] {i, pos%i} : null;
            }
        }
        return null;
    }

    @Test
    public void testMerge() {
        long startTime = System.currentTimeMillis();
        int bLimit = 12;
        int nLimit = 1000;
        for (int base = 2; base <= bLimit; base++) {
            Map<Long, Map<Long, Long>> A = new HashMap<>();
            Map<Long, Map<Long, Long>> B = new HashMap<>();
            for (long n = 2; n <= nLimit; n++) {
                var ab = TestUtils.generateAB(base, n, n-1);
                A.put(n, ab.getFirst());
                B.put(n, ab.getSecond());
            }

            for (long i = 2; i <= nLimit; i++) {
                for (long j = 2; i*j <= nLimit; j++) {
                    for (long t = -Math.min(i, j)+1; t < Math.min(i, j); t++) {
                        Long Ai = A.get(i).get(t);
                        Long Bi = B.get(i).get(t);
                        Long Aj = A.get(j).get(t);
                        Long Bj = B.get(j).get(t);
                        Long Aij = A.get(i*j).get(t);
                        Long Bij = B.get(i*j).get(t);
                        if ((Ai != null) && (Bi != null) && (Aj != null) && (Bj != null)) {
                            BigInteger[] AB = Restrictions.merge(base, t, i, Ai, Bi, j, Aj, Bj);
                            String msg = "base=" + base + ", target=" + t + ", C=" + i + ", A=" + Ai + ", B=" + Bi
                                    + ", p=" + j + ", a=" + Aj + ", b=" + Bi
                                    + ": expected A=" + Aij + ", B=" + Bij
                                    + "; actual=" + Arrays.toString(AB);
                            if (AB == null) {
                                Assertions.assertNull(AB, msg);
                            } else {
                                Assertions.assertEquals(BigInteger.valueOf(Aij), AB[0], msg);
                                Assertions.assertEquals(BigInteger.valueOf(Bij), AB[1], msg);
                            }
                        }
                    }
                }
            }
        }
        log.info("Checked merge for baseLimit={}, nLimit={} in {}ms", bLimit, nLimit, System.currentTimeMillis() - startTime);
    }

    @Test
    @Disabled
    public void testMergeDebug() {
        BigInteger[] ab = Restrictions.merge(2, 3,
                5, 4, 3,
                29, 28, 5);
        log.info("{}", (Object) ab);
    }
}
