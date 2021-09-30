package factorization;

import common.Common;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.math.BigInteger;
import java.util.Arrays;

public class PollardPm1 {
    private static final Logger log = LoggerFactory.getLogger(PollardPm1.class);

    private static final int gcdCallDelay = 1000;

    private final long firstLimit;
    private final Long secondLimit;
    private final Primes primes;
    private final BigInteger[] M;
    private final int[] mStepStart;
    private final int[] mStepEnd;

    public PollardPm1(Primes primes, long firstLimit, Long secondLimit) {
        if (firstLimit < 2) {
            throw new IllegalArgumentException("First limit should be at least 2");
        }
        this.firstLimit = firstLimit;
        this.secondLimit = secondLimit;
        this.primes = primes;

        long startTime = System.currentTimeMillis();
        log.info("Start initializing Pollard P-1 with firstLimit={}, secondLimit={}...", firstLimit, secondLimit);
        Pair<int[], int[]> pair = generateMSteps();
        this.mStepStart = pair.getFirst();
        this.mStepEnd = pair.getSecond();
        this.M = generateM();
        long bitLength = Arrays.stream(M).mapToLong(BigInteger::bitLength).sum();
        log.info("Pollard P-1 initialized in {}ms, sum of bit lengths {} ({}MB)", System.currentTimeMillis() - startTime, bitLength,
                String.format("%.2f", bitLength / ((double) (1<<23))));
    }

    public BigInteger findDivisor(BigInteger N, int tries) {
        assert N.compareTo(BigInteger.ONE) > 0;
        BigInteger a;
        BigInteger b = null;
        for (int i = 0; i < tries; i++) {
            a = BigInteger.valueOf((i < primes.size()) ? primes.get(i) : i+2);
            BigInteger gcd = N.gcd(a);
            if (!gcd.equals(BigInteger.ONE)) {
                if (N.equals(gcd)) {
                    return null;
                } else {
                    return gcd;
                }
            }

            b = a;
            for (int j = 0; j < M.length; j++) {
                BigInteger nextB = b.modPow(M[j], N);
                BigInteger d = N.gcd(nextB.subtract(BigInteger.ONE));
                if (d.equals(N)) {
                    d = tryMSteps(b, N, j);
                }
                if (d.equals(N)) {
                    b = BigInteger.ZERO;
                    break;
                } else if (!d.equals(BigInteger.ONE)) {
                    return d;
                }
                b = nextB;
            }
            if (b.signum() != 0) {
                break;
            }
        }
        return runSecondStage(N, b);
    }

    public BigInteger runSecondStage(BigInteger N, BigInteger b) {
        if (secondLimit == null) {
            return null;
        }
        int pi = mStepEnd[mStepEnd.length-1];
        if (pi >= primes.size()) {
            return null;
        }
        long p = primes.get(pi);
        assert p >= 3;
        BigInteger H = b.modPow(BigInteger.valueOf(p), N);
        GcdDetector gcdDetector = new GcdDetector(N, gcdCallDelay);
        BigInteger g = gcdDetector.add(H.subtract(BigInteger.ONE));
        if (g != null) {
            return N.equals(g) ? null : g;
        }

        BigInteger[] D = new BigInteger[300];
        pi++;
        if (pi >= primes.size()) {
            return null;
        }
        long nextP = primes.get(pi);
        while (nextP <= secondLimit) {
            int diff = (int) (nextP - p);
            int idx = (diff>>1) - 1;
            if (D[idx] == null) {
                D[idx] = b.modPow(BigInteger.valueOf(diff), N);
            }
            H = H.multiply(D[idx]).mod(N);
            g = gcdDetector.add(H.subtract(BigInteger.ONE));
            if (g != null) {
                return N.equals(g) ? null : g;
            }
            p = nextP;
            pi++;
            if (pi < primes.size()) {
                nextP = primes.get(pi);
            } else {
                break;
            }
        }

        g = gcdDetector.finish();
        return !N.equals(g) ? g : null;
    }

    private BigInteger tryMSteps(BigInteger b, BigInteger N, int step) {
        for (int i = mStepStart[step]; i < mStepEnd[step]; i++) {
            b = b.modPow(BigInteger.valueOf(maxPower(primes.get(i))), N);
            BigInteger d = N.gcd(Common.mod(b.subtract(BigInteger.ONE), N));
            if (!d.equals(BigInteger.ONE)) {
                return d;
            }
        }
        return BigInteger.ONE;
    }

    private Pair<int[], int[]> generateMSteps() {
        int n = 0;
        while ((n < primes.size()) && (primes.get(n) <= firstLimit)) {
            n++;
        }
        int size = (int) Math.sqrt(n);
        int[] mStepStart = new int[size];
        int[] mStepEnd = new int[size];
        mStepStart[0] = 0;
        mStepEnd[size-1] = n;
        for (int i = 1; i < size; i++) {
            mStepEnd[i-1] = (int) Math.round(n * (i*1.0) / size);
            mStepStart[i] = mStepEnd[i-1];
        }
        return Pair.create(mStepStart, mStepEnd);
    }

    private BigInteger[] generateM() {
        BigInteger[] M = new BigInteger[mStepStart.length];
        for (int i = 0; i < mStepStart.length; i++) {
            BigInteger[] P = new BigInteger[mStepEnd[i] - mStepStart[i]];
            for (int j = mStepStart[i]; j < mStepEnd[i]; j++) {
                P[j-mStepStart[i]] = BigInteger.valueOf(maxPower(primes.get(j)));
            }
            M[i] = Common.multiply(P);
        }
        return M;
    }

    private long maxPower(long p) {
        long pk = 1;
        while (firstLimit/pk >= p) {
            pk *= p;
        }
        return pk;
    }
}
