package factorization;

import common.Common;
import org.apache.commons.math3.primes.Primes;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class Factorizer {

    private final PollardPm1 pollardPm1;
    private final int primeTestCertainty;
    private final int pm1Tries;
    private final long rhoIterations;

    public Factorizer(PollardPm1 pollardPm1, int primeTestCertainty, int pm1Tries, long rhoIterations)
    {
        this.pollardPm1 = pollardPm1;
        this.primeTestCertainty = primeTestCertainty;
        this.pm1Tries = pm1Tries;
        this.rhoIterations = rhoIterations;
    }

    public Factorization factorize(BigInteger N) {
        return factorize(N, rhoIterations, pm1Tries);
    }

    public Factorization factorize(BigInteger N, long rhoIterations, int pm1Tries) {
        assert N.signum() > 0;
        if (N.equals(BigInteger.ONE)) {
            return Factorization.fromPrimeFactors(List.of());
        }
        if (N.compareTo(Common.MAX_INT) <= 0) {
            return Factorization.fromPrimeFactors(Primes.primeFactors(N.intValueExact())
                    .stream().map(BigInteger::valueOf).collect(Collectors.toUnmodifiableList()));
        }

        if (N.isProbablePrime(primeTestCertainty)) {
            return Factorization.fromSingleFactor(N,true);
        }

        if ((pm1Tries > 0) && (pollardPm1 != null)) {
            BigInteger divisor = pollardPm1.findDivisor(N, pm1Tries);
            if (divisor != null) {
                return processDivisor(divisor, N, rhoIterations, pm1Tries);
            }
        }

        if (rhoIterations > 0) {
            BigInteger divisor = PollardRho.findDivisor(N, rhoIterations);
            if (divisor != null) {
                return processDivisor(divisor, N, rhoIterations, 0);
            }
        }

        return Factorization.fromSingleFactor(N, false);
    }

    private Factorization processDivisor(BigInteger divisor, BigInteger N, long rhoIterations, int pm1Tries) {
        assert divisor.compareTo(BigInteger.ONE) > 0;
        assert divisor.compareTo(N) < 0;
        BigInteger[] dr = N.divideAndRemainder(divisor);
        assert dr[1].signum() == 0;

        Factorization a = factorize(divisor, rhoIterations, pm1Tries);
        Factorization b = factorize(dr[0], rhoIterations, pm1Tries);
        return Factorization.multiply(a, b);
    }
}
