import factorization.FactorizationDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static final long base = 2;
    static final long target = 5;
    static final BigInteger solutionCeil = BigInteger.TEN.pow(14);
    static final int threadsNumber = 6;
    static final int primeTestCertainty = 50;

    static final Launch launch = Launch.solverPrecalculated();
    //static final Launch launch = Launch.factorizationsGenerator(600, (long) 1e6, (long) 1e7, (long) 1e6, primeTestCertainty, threadsNumber);

    static final long scanLogThreshold = 15000000;
    static final boolean logSolutions = true;
    static final int maxLengthPerTask = 1<<24;
    static final int minParallelLength = 4;
    static final int sieveBound = 10000;
    static final int sieveLengthFactor = 5;

    public static void main(String[] args) {
        long initStartTime = System.currentTimeMillis();
        log.info("Start loading factorizations from file...");
        FactorizationDB factorizationDB = FactorizationDB.initialize(BigInteger.valueOf(base), BigInteger.valueOf(target), primeTestCertainty);
        log.info("Loaded {} factorizations in {}ms", factorizationDB.size(), System.currentTimeMillis() - initStartTime);
        Solver solver = new Solver(base, target, solutionCeil, launch, factorizationDB, threadsNumber, sieveBound, sieveLengthFactor,
                maxLengthPerTask, minParallelLength, scanLogThreshold, logSolutions);
        log.info("Initialization finished in {}ms, start solving...", System.currentTimeMillis() - initStartTime);

        long solveStartTime = System.currentTimeMillis();
        solver.solve();
        log.info("Solving finished in {}ms, start summarizing...", System.currentTimeMillis() - solveStartTime);

        long summarizeStartTime = System.currentTimeMillis();
        launch.summarize();
        log.info("Summarizing finished in {}ms. Total time spent: {}ms", System.currentTimeMillis() - summarizeStartTime,
                System.currentTimeMillis() - initStartTime);
        solver.shutdown();
    }

}
