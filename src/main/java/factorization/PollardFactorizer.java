package factorization;

import java.math.BigInteger;

public class PollardFactorizer extends Factorizer {

    private final PollardPm1 pollardPm1;
    private final int pm1Tries;
    private final long rhoIterations;

    public PollardFactorizer(PollardPm1 pollardPm1, int pm1Tries, long rhoIterations, int primeTestCertainty) {
        super(primeTestCertainty);
        this.pollardPm1 = pollardPm1;
        this.pm1Tries = pm1Tries;
        this.rhoIterations = rhoIterations;
    }

    @Override
    protected Factorization factorizeInternal(BigInteger N) {
        return factorizeInternal(N, pm1Tries);
    }

    protected Factorization factorizeInternal(BigInteger N, int pm1Tries) {
        if ((pm1Tries > 0) && (pollardPm1 != null)) {
            BigInteger divisor = pollardPm1.findDivisor(N, pm1Tries);
            if (divisor != null) {
                return processDivisor(divisor, N, n -> factorizeInternal(n, pm1Tries-1));
            }
        }
        return factorizeRho(N);
    }

    protected Factorization factorizeRho(BigInteger N) {
        if (rhoIterations > 0) {
            BigInteger divisor = PollardRho.findDivisor(N, rhoIterations);
            if (divisor != null) {
                return processDivisor(divisor, N, this::factorizeRho);
            }
        }
        return Factorization.fromSingleFactor(N, false);
    }

}
