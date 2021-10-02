import factorization.FactorizationDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static final long base = 3;
    static final long target = 2;
    static final BigInteger solutionCeil = BigInteger.TEN.pow(16).multiply(BigInteger.TWO);

    static final int threadsNumber = 2;
    static final int primeTestCertainty = 50;
    static final Boolean QRSievePrecalculated = true;

    static final Launch launch = Launch.solverPrecalculated();
    //static final Launch launch = Launch.factorizationsGenerator(5000, (long) 1e6, (long) 1e7, (long) 1e6, primeTestCertainty, threadsNumber);
    //static final Launch launch = Launch.fastScan(5000000, 2000000);

    static final long scanLogThreshold = 10000000;
    static final boolean logSolutions = true;
    static final int maxLengthPerTask = 1<<23;
    static final int minParallelLength = 4;
    static final int sieveBound = 2000;
    static final int sieveLengthFactor = 10;

    public static void main(String[] args) {
        long initStartTime = System.currentTimeMillis();
        log.info("Start loading factorizations from file...");
        FactorizationDB factorizationDB = FactorizationDB.initialize(BigInteger.valueOf(base), BigInteger.valueOf(target), primeTestCertainty);
        log.info("Loaded {} factorizations in {}ms", factorizationDB.size(), System.currentTimeMillis() - initStartTime);
        Solver solver = new Solver(base, target, solutionCeil, launch, factorizationDB, threadsNumber, sieveBound, sieveLengthFactor,
                maxLengthPerTask, minParallelLength, QRSievePrecalculated, scanLogThreshold, logSolutions);
        log.info("Initialization finished in {}ms, start solving...", System.currentTimeMillis() - initStartTime);

        long solveStartTime = System.currentTimeMillis();
        solver.solve();
        log.info("Solving finished in {}ms, start summarizing...", System.currentTimeMillis() - solveStartTime);

        long summarizeStartTime = System.currentTimeMillis();
        launch.summarize(solver);
        log.info("Summarizing finished in {}ms. Total time spent: {}ms", System.currentTimeMillis() - summarizeStartTime,
                System.currentTimeMillis() - initStartTime);
        solver.shutdown();
    }

}
