import common.Common;
import factorization.FactorizationDB;
import factorization.Factorizer;
import factorization.PollardPm1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.math.BigInteger;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class Launch {
    private static final Logger log = LoggerFactory.getLogger(Launch.class);

    public static Launch solverSimple(int factorizationMaxBitLength, Long pm1FirstBound, Long pm1SecondBound,
                                      long rhoIterations, int primeTestCertainty)
    {
        return new Launch() {
            @Override
            public long getPrimesBound(BigInteger solutionCeil) {
                return Common.max(super.getPrimesBound(solutionCeil), pm1FirstBound, pm1SecondBound);
            }
            @Override
            public boolean tryFactorize(int bitLength) {
                return bitLength <= factorizationMaxBitLength;
            }
            @Override
            public Factorizer getFactorizer(Primes primes) {
                PollardPm1 pollardPm1 = (pm1FirstBound != null) ? new PollardPm1(primes, pm1FirstBound, pm1SecondBound) : null;
                return new Factorizer(pollardPm1, primeTestCertainty, 1, rhoIterations);
            }
        };
    }

    public static Launch solverPrecalculated() {
        return new Launch() {
            @Override
            public boolean tryFactorize(int bitLength) {
                return false;
            }
            @Override
            public Factorizer getFactorizer(Primes primes) {
                return null;
            }
        };
    }

    public static Launch factorizationsGenerator(int maxBitLength, Long pm1FirstBound, Long pm1SecondBound,
                                                 long rhoIterations, int primeTestCertainty, int threadsNumber)
    {
        TreeMap<BigInteger, long[]> toFactor = new TreeMap<>();
        return new Launch() {
            @Override
            public Factorizer getFactorizer(Primes primes) {
                return new Factorizer(null, primeTestCertainty, 0, 0);
            }
            @Override
            public boolean tryFactorize(int bitLength) {
                return bitLength <= maxBitLength;
            }
            @Override
            public void registerFactorizationCall(BigInteger N, long base, int power, long target, long A) {
                toFactor.put(N, new long[] {base, power, target, A});
            }

            @Override
            public long scanLength(BigInteger scanLength) {
                return 0;
            }

            @Override
            public void summarize() {
                Primes primes = new Primes(Common.max(0L, pm1FirstBound, pm1SecondBound));
                PollardPm1 pollardPm1 = (pm1FirstBound != null) ? new PollardPm1(primes, pm1FirstBound, pm1SecondBound) : null;
                Factorizer factorizer = new Factorizer(pollardPm1, primeTestCertainty, 2, rhoIterations);
                FactorizationDB.logFactorizations(toFactor, factorizer, threadsNumber);
            }
        };
    }

    public static Launch fastScan(long goodPrimeBound, long maxScanLength) {
        BigInteger maxScanLengthBI = BigInteger.valueOf(maxScanLength);
        return new Launch() {
            @Override
            public long getPrimesBound(BigInteger solutionCeil) {
                return Math.min(goodPrimeBound, super.getPrimesBound(solutionCeil));
            }
            @Override
            public long getGoodPrimesBound(BigInteger solutionCeil) {
                return Math.min(goodPrimeBound, super.getPrimesBound(solutionCeil));
            }
            @Override
            public boolean tryFactorize(int bitLength) {
                return false;
            }
            @Override
            public Factorizer getFactorizer(Primes primes) {
                return null;
            }

            @Override
            public long scanLength(BigInteger scanLength) {
                return scanLength.compareTo(maxScanLengthBI) <= 0 ? scanLength.longValueExact() : 0;
            }
        };
    }

    private final NavigableSet<BigInteger> solutions = new TreeSet<>();

    public long getPrimesBound(BigInteger solutionCeil) {
        return solutionCeil.sqrt().longValueExact();
    }

    public long getGoodPrimesBound(BigInteger solutionCeil) {
        return solutionCeil.sqrt().longValueExact();
    }

    public abstract Factorizer getFactorizer(Primes primes);

    public abstract boolean tryFactorize(int bitLength);

    public long scanLength(BigInteger scanLength) {
        return scanLength.longValueExact();
    }

    public void addSolution(BigInteger solution) {
        solutions.add(solution);
    }

    public void registerFactorizationCall(BigInteger N, long base, int power, long target, long A) {}

    public void summarize() {
        log.info("Found {} solutions: {}", solutions.size(), solutions);
    }

    public List<BigInteger> getSolutions() {
        return List.copyOf(solutions);
    }
}
