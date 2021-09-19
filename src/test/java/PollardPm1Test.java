import factorization.PollardPm1;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import primes.Primes;

import java.math.BigInteger;
import java.util.Random;

public class PollardPm1Test {
    @Test
    @Disabled
    public void testFindDivisor() {
        Primes primes = new Primes(100000000);
        PollardPm1 first = new PollardPm1(primes, 100000000, null);
        BigInteger P = BigInteger.probablePrime(500, new Random());

        long startTime = System.currentTimeMillis();
        System.out.println(first.findDivisor(P.multiply(BigInteger.probablePrime(21, new Random())), 1));
        System.out.println("First done in " + (System.currentTimeMillis() - startTime) + "ms");

    }
}