package factorization;

import common.Common;
import org.apache.commons.math3.primes.Primes;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;

public abstract class Factorizer {
    private final int primeTestCertainty;

    public Factorizer(int primeTestCertainty) {
        this.primeTestCertainty = primeTestCertainty;
    }

    public Factorization factorize(BigInteger N) {
        return factorize(N, this::factorizeInternal);
    }

    private Factorization factorize(BigInteger N, Function<BigInteger, Factorization> factorizer) {
        assert N.signum() > 0;
        if (N.equals(BigInteger.ONE)) {
            return Factorization.fromPrimeFactors(List.of());
        }
        if (N.compareTo(Common.MAX_INT) <= 0) {
            return Factorization.fromPrimeFactors(Primes.primeFactors(N.intValueExact())
                    .stream().map(BigInteger::valueOf).toList());
        }
        if (N.isProbablePrime(primeTestCertainty)) {
            return Factorization.fromSingleFactor(N,true);
        }
        return factorizer.apply(N);
    }

    protected abstract Factorization factorizeInternal(BigInteger N);

    protected Factorization processDivisor(BigInteger divisor, BigInteger N, Function<BigInteger, Factorization> factorizer) {
        assert divisor.compareTo(BigInteger.ONE) > 0;
        assert divisor.compareTo(N) < 0;
        BigInteger[] dr = N.divideAndRemainder(divisor);
        assert dr[1].signum() == 0;

        Factorization a = factorize(divisor, factorizer);
        Factorization b = factorize(dr[0], factorizer);
        return Factorization.multiply(a, b);
    }
}
