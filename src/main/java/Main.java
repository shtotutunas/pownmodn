import common.Common;
import common.TaskExecutor;
import factorization.FactorizationDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.GoodPrimes;
import primes.Primes;
import scan.ScanSieve;
import scan.Scanner;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static final long base = 2;
    static final long target = -11;
    static final BigInteger solutionCeil = Common.e(1, 14);

    static final int threadsNumber = 1;
    static final int primeTestCertainty = 50;
    static final Boolean qrSievePrecalculated = null;

    static final Launch launch = Launch.solverPrecalculated();
    //static final Launch launch = Launch.factorizationsGenerator(5000, (long) 1e6, (long) 1e7, (long) 1e6, primeTestCertainty, threadsNumber);
    //static final Launch launch = Launch.fastScan(10000000, 1000);

    static final long scanLogThreshold = 5000000;
    static final boolean logSolutions = true;
    static final int maxLengthPerTask = 1<<22;
    static final int minParallelLength = 10;
    static final boolean loadFactorizationDB = true;
    static final boolean printRunStats = true;

    public static void main(String[] args) {
        long initStartTime = System.currentTimeMillis();
        FactorizationDB factorizationDB = null;
        if (loadFactorizationDB) {
            log.info("Start loading factorizations from file...");
            factorizationDB = FactorizationDB.initialize(BigInteger.valueOf(base), BigInteger.valueOf(target), primeTestCertainty);
            log.info("Loaded {} factorizations in {}ms", factorizationDB.size(), System.currentTimeMillis() - initStartTime);
        }

        long goodPrimesBound = launch.getGoodPrimesBound(solutionCeil);
        long primesBound = Math.max(launch.getPrimesBound(), goodPrimesBound);

        long primesStartTime = System.currentTimeMillis();
        log.info("Start generating primes up to {}...", primesBound);
        Primes primes = new Primes(primesBound);
        log.info("{} primes found in {}ms", primes.size(), System.currentTimeMillis() - primesStartTime);

        TaskExecutor executor = TaskExecutor.create(threadsNumber);
        GoodPrimes goodPrimes = GoodPrimes.generate(launch.getGoodPrimesBound(solutionCeil), base, target, primes, executor);
        ScanSieve scanSieve = new ScanSieve(BigInteger.valueOf(base), BigInteger.valueOf(target), primes, qrSievePrecalculated);
        Scanner scanner = new Scanner(BigInteger.valueOf(base), target, scanSieve, executor, maxLengthPerTask, minParallelLength);

        Solver solver = new Solver(base, target, solutionCeil, launch, goodPrimes,
                factorizationDB, launch.getFactorizer(primes), scanner, scanLogThreshold, logSolutions);
        log.info("Initialization finished in {}ms, start solving...", System.currentTimeMillis() - initStartTime);

        long solveStartTime = System.currentTimeMillis();
        solver.solve();
        log.info("Solving finished in {}ms, start summarizing...", System.currentTimeMillis() - solveStartTime);

        long summarizeStartTime = System.currentTimeMillis();
        launch.summarize(solver, primes);
        log.info("Summarizing finished in {}ms. Total time spent: {}ms", System.currentTimeMillis() - summarizeStartTime,
                System.currentTimeMillis() - initStartTime);

        if (printRunStats) {
            scanner.logScanStats();
            scanSieve.logPrimeSieveStats();
            printThreadsCpuStats();
        }
        executor.shutdown();
    }

    private static void printThreadsCpuStats() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        List<Map.Entry<Thread, Long>> list = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> !t.isDaemon())
                .map(t -> Map.entry(t, threadMXBean.getThreadCpuTime(t.getId())))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();

        StringBuilder buf = new StringBuilder("Thread CPU stats: sum = ")
                .append(String.format(Common.LOCALE, "%.3f", list.stream().mapToLong(Map.Entry::getValue).sum() / 1.0e9));

        for (Map.Entry<Thread, Long> entry : list) {
            buf.append('\n').append(entry.getKey().getName()).append(" --- ")
                    .append(String.format(Common.LOCALE, "%.3f", entry.getValue() / 1.0e9));
        }

        log.info(buf.toString());
    }
}
