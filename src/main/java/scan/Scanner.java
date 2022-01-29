package scan;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import common.Common;
import common.TaskExecutor;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Scanner {
    private static final Logger log = LoggerFactory.getLogger(Scanner.class);

    private final BigInteger base;
    private final BigInteger target;
    private final long targetLong;

    private final TaskExecutor executor;
    private final int maxLengthPerTask;
    private final int minParallelLength;

    private final ScanSieve scanSieve;
    private final ModPowCalculatorFactory modPowCalculatorFactory;

    private final AtomicLong solutionCheckCount = new AtomicLong(0);

    public Scanner(BigInteger base, long target, ScanSieve scanSieve, TaskExecutor executor, int maxLengthPerTask, int minParallelLength) {
        assert base.compareTo(BigInteger.TWO) >= 0;
        assert maxLengthPerTask >= 2;

        this.base = base;
        this.target = BigInteger.valueOf(target);
        this.targetLong = target;

        this.executor = executor;
        this.maxLengthPerTask = maxLengthPerTask;
        this.minParallelLength = minParallelLength;

        this.scanSieve = scanSieve;
        this.modPowCalculatorFactory = new ModPowCalculatorFactory(base);
    }

    public Pair<BigInteger[], Long> scan(BigInteger C, BigInteger A, BigInteger B, long length, boolean checkCandidates) {
        assert length > 0;
        assert C.signum() > 0;
        assert A.signum() > 0;
        if (B.signum() == 0) {
            B = A;
        }
        assert B.signum() > 0;

        int threadsNumber = executor.getThreadsNumber();
        long tasksNumber;
        if (length/threadsNumber < minParallelLength) {
            tasksNumber = 1;
        } else {
            tasksNumber = (length - 1)/maxLengthPerTask + 1;
            if (tasksNumber%threadsNumber != 0) {
                tasksNumber += threadsNumber - (tasksNumber%threadsNumber);
            }
        }
        int taskLength = (int) (length / tasksNumber);
        long plusOne = length % tasksNumber;

        ScanSieve.BitSetGenerator bitSetGenerator = scanSieve.createBitSetGenerator(B, A, length, C.testBit(0));
        ModPowCalculator modPowCalculator = modPowCalculatorFactory.createCalculator(C);

        Task[] tasks = new Task[(int) tasksNumber];
        BigInteger start = B;
        BigInteger smallerStep = A.multiply(BigInteger.valueOf(taskLength));
        BigInteger biggerStep = smallerStep.add(A);
        long shift = 0;
        for (int i = 0; i < tasksNumber; i++) {
            int curLength = (i < plusOne) ? taskLength+1 : taskLength;
            tasks[i] = new Task(C, start, A, shift, curLength, bitSetGenerator, modPowCalculator, checkCandidates);
            start = start.add((i < plusOne) ? biggerStep : smallerStep);
            shift += curLength;
        }
        assert shift == length;

        Future<Pair<BigInteger[], Long>>[] futures = new Future[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = executor.submit(tasks[i]);
        }

        Stream.Builder<BigInteger> solutions = Stream.builder();
        long counter = 0;
        for (Future<Pair<BigInteger[], Long>> future : futures) {
            try {
                Pair<BigInteger[], Long> result = future.get();
                if (result != null) {
                    if (result.getFirst() != null) {
                        for (BigInteger x : result.getFirst()) {
                            solutions.add(x);
                        }
                    }
                    counter += result.getSecond();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Pair.create(solutions.build().toArray(BigInteger[]::new), counter);
    }

    private class Task implements Supplier<Pair<BigInteger[], Long>> {
        private final BigInteger multiplier;
        private final BigInteger start;
        private final BigInteger step;
        private final long from;
        private final int length;
        private final ScanSieve.BitSetGenerator bitSetGenerator;
        private final ModPowCalculator modPowCalculator;
        private final boolean checkCandidates;

        private Task(BigInteger multiplier, BigInteger start, BigInteger step, long from, int length, ScanSieve.BitSetGenerator bitSetGenerator,
                     ModPowCalculator modPowCalculator, boolean checkCandidates)
        {
            this.multiplier = multiplier;
            this.start = start;
            this.step = step;
            this.from = from;
            this.length = length;
            this.bitSetGenerator = bitSetGenerator;
            this.modPowCalculator = modPowCalculator;
            this.checkCandidates = checkCandidates;
        }

        @Override
        public Pair<BigInteger[], Long> get() {
            BitSet bits = bitSetGenerator.generate(from, length);
            if (bits == null) {
                return null;
            }

            Stream.Builder<BigInteger> result = null;
            long counter = 0;
            int startWithBig = 0;
            if ((start.compareTo(Common.MAX_LONG) <= 0) && (step.compareTo(Common.MAX_LONG) <= 0)) {
                long startLong = start.longValueExact();
                long stepLong = step.longValueExact();
                startWithBig = (int) Math.min((Long.MAX_VALUE-startLong)/stepLong, length-1) + 1;

                for (int i = 0; i < startWithBig; i++) {
                    if (bits.get(i)) {
                        continue;
                    }
                    counter++;
                    if (checkCandidates) {
                        solutionCheckCount.incrementAndGet();
                        long m = startLong + stepLong*i;
                        if (modPowCalculator.calculate(m) == Common.mod(targetLong, m)) {
                            BigInteger M = BigInteger.valueOf(m);
                            BigInteger candidate = M.multiply(multiplier);
                            if (base.modPow(candidate, candidate).equals(Common.mod(target, candidate))) {
                                if (result == null) {
                                    result = Stream.builder();
                                }
                                result.add(M);
                            }
                        }
                    }
                }
            }

            if (startWithBig < length) {
                BigInteger M = start;
                int prev = 0;
                IntObjectMap<BigInteger> mSteps = new IntObjectHashMap<>();
                mSteps.put(1, step);

                for (int i = startWithBig; i < length; i++) {
                    if (bits.get(i)) {
                        continue;
                    }
                    counter++;
                    if (checkCandidates) {
                        int move = i - prev;
                        if (move > 0) {
                            BigInteger addM = mSteps.get(move);
                            if (addM == null) {
                                addM = step.multiply(BigInteger.valueOf(move));
                                mSteps.put(move, addM);
                            }
                            M = M.add(addM);
                            prev = i;
                        }

                        solutionCheckCount.incrementAndGet();
                        if (modPowCalculator.calculate(M).equals(Common.mod(target, M))) {
                            BigInteger candidate = M.multiply(multiplier);
                            if (base.modPow(candidate, candidate).equals(Common.mod(target, candidate))) {
                                if (result == null) {
                                    result = Stream.builder();
                                }
                                result.add(M);
                            }
                        }
                    }
                }
            }

            return Pair.create((result != null) ? result.build().toArray(BigInteger[]::new) : null, counter);
        }
    }

    public void logScanStats() {
        log.info("Scanner stats --- total solution checks: {}", solutionCheckCount.get());
    }

}
