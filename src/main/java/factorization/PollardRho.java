package factorization;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.UnaryOperator;

public class PollardRho {

    private static final int minGcdCallDelay = 100;
    private static final int maxGcdCallDelay = 100000;
    private static final Random random = new Random(777);

    public static BigInteger findDivisor(BigInteger N, Long iterations) {
        assert N.compareTo(BigInteger.ONE) > 0;
        if (!N.testBit(0)) {
            return BigInteger.TWO;
        }

        UnaryOperator<BigInteger> F = a -> a.multiply(a).add(BigInteger.ONE).mod(N);
        double sqrt = (iterations != null) ? Math.sqrt(iterations) : Math.sqrt(N.doubleValue());
        GcdDetector gcdDetector = new GcdDetector(N, (int) Math.max(minGcdCallDelay, Math.min(maxGcdCallDelay, sqrt)));

        BigInteger x = new BigInteger(N.bitLength(), random);
        BigInteger xx = x;
        if (iterations == null) {
            iterations = Long.MAX_VALUE;
        }
        for (long i = 0; i < iterations; i++) {
            x = F.apply(x);
            xx = F.apply(F.apply(xx));
            BigInteger d = gcdDetector.add(xx.subtract(x));
            if (d != null && !N.equals(d)) {
                return d;
            }
        }
        return null;
    }

}
