import common.Common;
import factorization.FactorizationDB;
import factorization.Factorizer;
import factorization.PollardPm1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class Launch {
    private static final Logger log = LoggerFactory.getLogger(Launch.class);

    public static Launch solverSimple(int factorizationMaxBitLength, Long pm1FirstBound, Long pm1SecondBound,
                                      long rhoIterations, int primeTestCertainty)
    {
        return new Launch() {
            @Override
            public long getPrimesBound() {
                return Common.max(pm1FirstBound, pm1SecondBound, super.getPrimesBound());
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
        Map<BigInteger, BigInteger> numToExp = new HashMap<>();
        Map<BigInteger, Long> expToCand = new HashMap<>();
        return new Launch() {
            @Override
            public long getPrimesBound() {
                return Common.max(pm1FirstBound, pm1SecondBound, super.getPrimesBound());
            }
            @Override
            public Factorizer getFactorizer(Primes primes) {
                return new Factorizer(null, primeTestCertainty, 0, 0);
            }
            @Override
            public boolean tryFactorize(int bitLength) {
                return bitLength <= maxBitLength;
            }
            @Override
            public void registerFactorizationCall(BigInteger N, BigInteger base, BigInteger exp, BigInteger target, BigInteger A) {
                numToExp.put(N, exp);
            }
            @Override
            public boolean checkCandidates() {
                return true;
            }
            @Override
            public void registerScan(BigInteger N, long candidates) {
                expToCand.put(N, candidates);
            }

            @Override
            public void summarize(Solver solver, Primes primes) {
                PollardPm1 pollardPm1 = (pm1FirstBound != null) ? new PollardPm1(primes, pm1FirstBound, pm1SecondBound) : null;
                Factorizer factorizer = new Factorizer(pollardPm1, primeTestCertainty, 2, rhoIterations);
                SortedMap<BigInteger, Long> scanTime = numToExp.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue,
                        e -> expToCand.getOrDefault(e.getValue(), 0L), (x, y) -> x, TreeMap::new));
                FactorizationDB.logFactorizations(solver.getBase(), solver.getTarget(), scanTime, factorizer, threadsNumber);
            }
        };
    }

    public static Launch fastScan(long goodPrimeBound, long maxScanLength) {
        BigInteger maxScanLengthBI = BigInteger.valueOf(maxScanLength);
        return new Launch() {
            @Override
            public long getGoodPrimesBound(BigInteger solutionCeil) {
                return Math.min(goodPrimeBound, super.getGoodPrimesBound(solutionCeil));
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
                return scanLength.compareTo(maxScanLengthBI) <= 0 ? scanLength.longValueExact() : maxScanLength;
            }
        };
    }

    private final NavigableSet<BigInteger> solutions = new TreeSet<>();

    public long getPrimesBound() {
        return 0;
    }

    public long getGoodPrimesBound(BigInteger solutionCeil) {
        return solutionCeil.sqrt().longValueExact();
    }

    public abstract Factorizer getFactorizer(Primes primes);

    public abstract boolean tryFactorize(int bitLength);

    public long scanLength(BigInteger scanLength) {
        return scanLength.longValueExact();
    }

    public boolean checkCandidates() {
        return true;
    }

    public void addSolution(BigInteger solution) {
        solutions.add(solution);
    }

    public void registerFactorizationCall(BigInteger N, BigInteger base, BigInteger exp, BigInteger target, BigInteger A) {}

    public void registerScan(BigInteger N, long candidates) {}

    public void summarize(Solver solver, Primes primes) {
        log.info("Found {} solutions: {}", solutions.size(), solutions);
    }

    public List<BigInteger> getSolutions() {
        return List.copyOf(solutions);
    }
}
