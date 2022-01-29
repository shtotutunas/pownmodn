import factorization.Factorization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.apache.commons.math3.primes.Primes.isPrime;

public class PrimesTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testGeneratePrimes() {
        long startTime = System.currentTimeMillis();
        int limit = 100000;
        Primes primes = new Primes(limit);
        Set<Long> set = IntStream.range(0, primes.size()).mapToObj(primes::get).collect(Collectors.toUnmodifiableSet());
        for (int i = 2; i <= limit; i++) {
            Assertions.assertEquals(isPrime(i), set.contains((long) i), "Wrong for " + i);
        }
        log.info("OK - tested for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    void testFindDivisors() {
        long startTime = System.currentTimeMillis();
        int limit = 30000;
        Primes primes = new Primes(100);
        for (int n = 1; n <= limit; n++) {
            long[] act = Factorization.generateDivisors(primes.factorize(n));
            LongStream.Builder buf = LongStream.builder();
            for (int d = 1; d*d <= n; d++) {
                if (n%d == 0) {
                    buf.add(d).add(n/d);
                }
            }
            long[] exp = buf.build().sorted().distinct().toArray();
            Assertions.assertArrayEquals(exp, act, "n=" + n);
        }
        log.info("OK - tested for limit={} in {}ms", limit, System.currentTimeMillis() - startTime);
    }

    @Test
    void testFloorIdx() {
        Primes primes = new Primes(100);
        int n = (int) primes.get(primes.size()-1);
        for (int i = -n; i < 2; i++) {
            Assertions.assertEquals(primes.floorIdx(i), -1, "i=" + i);
        }
        for (int i = 2; i < n; i++) {
            int idx = primes.floorIdx(i);
            Assertions.assertTrue(primes.get(idx) <= i, "i=" + i);
            Assertions.assertTrue(primes.get(idx+1) > i, "i=" + i);
        }
        for (int i = n; i < 2*n; i++) {
            Assertions.assertEquals(primes.floorIdx(i), primes.size()-1, "i=" + i);
        }
    }

}
