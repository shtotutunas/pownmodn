import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

}
