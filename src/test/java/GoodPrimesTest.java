import org.apache.commons.math3.util.ArithmeticUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.GoodPrimes;
import primes.Primes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.math3.primes.Primes.isPrime;

public class GoodPrimesTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testGoodPrimes() {
        long startTime = System.currentTimeMillis();
        int bLimit = 9;
        int tLimit = 15;
        int pLimit = 3000;
        int threads = 4;
        ExecutorService executor = (threads > 1) ? Executors.newFixedThreadPool(threads) : null;
        Primes primes = new Primes(pLimit);

        for (int base = 2; base <= bLimit; base++) {
            Map<Long, Map<Long, Long>> A = new HashMap<>();
            Map<Long, Map<Long, Long>> B = new HashMap<>();
            for (long n = 2; n <= pLimit; n++) {
                var ab = TestUtils.generateAB(base, (int) n, tLimit);
                A.put(n, ab.getFirst());
                B.put(n, ab.getSecond());
            }

            for (long t = -tLimit; t <= tLimit; t++) {
                GoodPrimes gp = GoodPrimes.generate(pLimit, base, t, primes, executor, threads);
                Map<Long, Long> a = new LinkedHashMap<>();
                Map<Long, Long> b = new LinkedHashMap<>();
                for (int i = 0; i < gp.size(); i++) {
                    Assertions.assertEquals(gp.getGcdAB(i), ArithmeticUtils.gcd(gp.getA(i), gp.getB(i)));
                    a.put(gp.get(i), gp.getA(i));
                    b.put(gp.get(i), gp.getB(i));
                }

                for (int i = 0; i < primes.size(); i++) {
                    long p = primes.get(i);
                    Long expA = A.get(p).get(t);
                    Long expB = B.get(p).get(t);
                    Long actA = a.get(p);
                    Long actB = b.get(p);
                    if (actA != null) {
                        expB %= expA;
                        Assertions.assertEquals(expA, actA, "base=" + base + ", t=" + t + ", p=" + p);
                        Assertions.assertEquals(expB, actB, "base=" + base + ", t=" + t + ", p=" + p);
                    } else if (expA != null) {
                        long gcd = ArithmeticUtils.gcd(expA, expB);
                        Assertions.assertTrue(gcd > 1, "base=" + base + ", t=" + t + ", p=" + p);
                        boolean found = false;
                        for (long d = 2; !found && d <= gcd; d++) {
                            if (gcd%d == 0) {
                                if (isPrime((int) d) && !a.containsKey(d)) {
                                    found = true;
                                }
                                if (!A.get(d).containsKey(t)) {
                                    found = true;
                                }
                            }
                        }
                        Assertions.assertTrue(found, "base=" + base + ", t=" + t + ", p=" + p);
                    }
                }
            }
        }
        log.info("OK - tested for base<={}, |target|<={}, good primes <={} in {}ms", bLimit, tLimit, pLimit, System.currentTimeMillis() - startTime);
    }
}
