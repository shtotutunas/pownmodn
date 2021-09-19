import common.Common;
import common.Modules;
import factorization.Factorization;
import factorization.FactorizationDB;
import factorization.Factorizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.GoodPrimes;
import primes.Primes;
import primes.Restrictions;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Solver {
    private static final Logger log = LoggerFactory.getLogger(Solver.class);

    private final long base;
    private final long target;
    private final BigInteger baseBig;
    private final BigInteger targetBig;
    private final double log2Base;

    private final BigInteger solutionCeil;

    private final Primes primes;
    private final GoodPrimes goodPrimes;

    private final ExecutorService executor;
    private final FactorizationDB factorizationDB;
    private final Factorizer factorizer;
    private final Scanner scanner;
    private final long scanLogThreshold;
    private final boolean logSolutions;

    private final Launch launch;
    private final long[] primeStack;

    public Solver(long base, long target, BigInteger solutionCeil, Launch launch, FactorizationDB factorizationDB, int threadsNumber,
                  int sieveBound, int sieveLengthFactor, int maxLengthPerTask, int minParallelLength,
                  long scanLogThreshold, boolean logSolutions)
    {
        assert base >= 2;
        this.base = base;
        this.baseBig = BigInteger.valueOf(base);
        this.target = target;
        this.targetBig = BigInteger.valueOf(target);
        this.log2Base = Math.log(base) / Math.log(2);
        this.solutionCeil = solutionCeil;

        long primesBound = launch.getPrimesBound(solutionCeil);
        long startTime = System.currentTimeMillis();
        log.info("Start generating primes up to {}...", primesBound);
        this.primes = new Primes(launch.getPrimesBound(solutionCeil));
        log.info("{} primes found in {}ms", primes.size(), System.currentTimeMillis() - startTime);

        this.executor = (threadsNumber > 1) ? Executors.newScheduledThreadPool(threadsNumber) : null;
        this.goodPrimes = GoodPrimes.generate(launch.getGoodPrimesBound(solutionCeil), base, target, primes, executor, threadsNumber);

        this.factorizationDB = factorizationDB;
        this.factorizer = launch.getFactorizer(primes);
        this.scanner = new Scanner(baseBig, targetBig, sieveBound, sieveLengthFactor, primes, executor, threadsNumber,
                maxLengthPerTask, minParallelLength, scanLogThreshold);
        this.scanLogThreshold = scanLogThreshold;
        this.logSolutions = logSolutions;

        this.launch = launch;
        this.primeStack = new long[solutionCeil.bitLength()];
    }

    public void solve() {
        // scanning prime solutions
        if (target == base) {
            scan(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, solutionCeil.longValueExact(), 0);
        } else {
            scan(BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, Math.max(base, base-target), 0);
        }

        pognali(0, 0, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO);
    }

    private void pognali(int pos, int gpPos, BigInteger C, BigInteger A, BigInteger B) {
        BigInteger div = solutionCeil.divide(C);
        BigInteger am = div.subtract(B);
        if (am.signum() < 0) {
            return;
        }
        BigInteger scanLength = am.divide(A).add(BigInteger.ONE);

        boolean factorized = tryFactorization(C, A, pos);
        if (!factorized) {
            scan(C, A, B, launch.scanLength(scanLength), pos);
        }

        long bound = div.sqrt().longValueExact();
        for (int i = gpPos; (i < goodPrimes.size()) && (goodPrimes.get(i) <= bound); i++) {
            if (pos == 0) {
                long gcd = goodPrimes.getGcdAB(i);
                if (gcd > 1) {
                    continue;
                }
            }

            BigInteger P = BigInteger.valueOf(goodPrimes.get(i));
            BigInteger[] newAB = Restrictions.merge(baseBig, targetBig, C, A, B,
                    P, BigInteger.valueOf(goodPrimes.getA(i)), BigInteger.valueOf(goodPrimes.getB(i)));
            if (newAB != null) {
                BigInteger gcd = newAB[0].gcd(newAB[1]);
                if (gcd.compareTo(BigInteger.ONE) > 0) {
                    continue;
                }
                primeStack[pos] = goodPrimes.get(i);
                pognali(pos+1, i, C.multiply(P), newAB[0], newAB[1]);
            }
        }
    }

    private boolean tryFactorization(BigInteger C, BigInteger A, int pos) {
        if (C.equals(BigInteger.ONE)) {
            return true;
        }
        Factorization factorization = null;
        if ((factorizationDB != null) && C.compareTo(Common.MAX_INT) <= 0) {
            factorization = factorizationDB.get(C.intValueExact());
        }

        if (factorization == null) {
            long bitLengthForecast = Math.round(log2Base * C.doubleValue());
            if (!launch.tryFactorize((bitLengthForecast <= Integer.MAX_VALUE) ? (int) bitLengthForecast : Integer.MAX_VALUE)) {
                return false;
            }
            BigInteger F = baseBig.pow(C.intValueExact()).subtract(targetBig).abs();
            if (F.signum() == 0) {
                return false;
            }

            long startTime = System.currentTimeMillis();
            String logStr = base + "^" + C + ((target > 0) ? "" : "+") + (-target);
            log.info("Factorizing {} where {} = {}", logStr, C, stackToString(pos));
            factorization = factorizer.factorize(F);
            launch.registerFactorizationCall(F, base, C.intValueExact(), target, A.longValueExact());
            log.info("Factorized in {}ms with {} composites: {} = {}", System.currentTimeMillis() - startTime, factorization.compositeCount(), logStr, factorization);
        }

        factorization.forEachDivisor(d -> {
            BigInteger N = C.multiply(d);
            if (Common.mod(Modules.pow(baseBig, N, N).subtract(targetBig), N).signum() == 0) {
                if (logSolutions) {
                    log.info("Found solution: {} = {}", N, stackToString(d, pos));
                }
                launch.addSolution(N);
            }
        });
        return (factorization.compositeCount() == 0);
    }

    private void scan(BigInteger C, BigInteger A, BigInteger B, long length, int pos) {
        if (length <= 0) {
            return;
        }
        long startTime = System.currentTimeMillis();
        if (length >= scanLogThreshold) {
            log.info("Start scanning {} * ({}x + {}) for x in [0; {}]...", stackToString(pos), A, B, length-1);
        }
        Pair<BigInteger[], Long> result = scanner.scan(C, A, B, length);
        for (BigInteger m : result.getFirst()) {
            BigInteger solution = C.multiply(m);
            if (logSolutions) {
                log.info("Found solution: {} = {}", solution, stackToString(m, pos));
            }
            launch.addSolution(solution);
        }
        if (length >= scanLogThreshold) {
            log.info("Scanned {} * ({}x + {}) for x in [0; {}] in {}ms: checked {} candidates ({}%)", stackToString(pos), A, B, length-1,
                    System.currentTimeMillis() - startTime, result.getSecond(), result.getSecond() * 100.0 / length);
        }
    }

    private String stackToString(int pos) {
        return stackToString(null, pos);
    }

    private String stackToString(BigInteger lastFactor, int pos) {
        if (lastFactor == null) {
            lastFactor = BigInteger.ONE;
        }
        if ((pos == 0) && BigInteger.ONE.equals(lastFactor)) {
            return "1";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pos; i++) {
            if (buf.length() > 0) {
                buf.append(" * ");
            }
            buf.append(primeStack[i]);
        }
        if (!BigInteger.ONE.equals(lastFactor)) {
            if (buf.length() > 0) {
                buf.append(" * ");
            }
            buf.append(lastFactor);
        }
        return buf.toString();
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}