package scan;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import common.Common;
import common.Modules;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import primes.Primes;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class Scanner {
    private static final Logger log = LoggerFactory.getLogger(Scanner.class);

    private final BigInteger base;
    private final BigInteger target;
    private final long sieveBound;
    private final long sieveLengthFactor;

    private final BigInteger[] primes;
    private final int[][] inv;

    private final ExecutorService executor;
    private final int threadsNumber;
    private final int maxLengthPerTask;
    private final int minParallelLength;

    private final ModPowCalculatorFactory modPowCalculatorFactory;
    private final long scanLogThreshold;

    public Scanner(BigInteger base, BigInteger target, int sieveBound, int sieveLengthFactor, Primes primes,
                   ExecutorService executor, int threadsNumber, int maxLengthPerTask, int minParallelLength,
                   ModPowCalculatorFactory modPowCalculatorFactory, long scanLogThreshold)
    {
        assert base.compareTo(BigInteger.TWO) >= 0;
        assert sieveBound >= 0;
        assert threadsNumber > 0;
        assert maxLengthPerTask >= 2;

        this.base = base;
        this.target = target;
        this.sieveBound = sieveBound;
        this.sieveLengthFactor = sieveLengthFactor;

        Stream.Builder<BigInteger> primesBuf = Stream.builder();
        Stream.Builder<int[]> invBuf = Stream.builder();
        for (int i = 0; (i < primes.size()) && (primes.get(i) <= sieveBound); i++) {
            int p = (int) primes.get(i);
            primesBuf.add(BigInteger.valueOf(p));
            int[] inv = new int[p];
            for (int j = 1; j < p; j++) {
                inv[j] = Modules.pow(j, p-2, p);
            }
            invBuf.add(inv);
        }
        this.primes = primesBuf.build().toArray(BigInteger[]::new);
        this.inv = invBuf.build().toArray(int[][]::new);

        this.executor = executor;
        this.threadsNumber = threadsNumber;
        this.maxLengthPerTask = maxLengthPerTask;
        this.minParallelLength = minParallelLength;
        this.modPowCalculatorFactory = modPowCalculatorFactory;
        this.scanLogThreshold = scanLogThreshold;
    }

    public Pair<BigInteger[], Long> scan(BigInteger C, BigInteger A, BigInteger B, long length) {
        assert length > 0;
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

        long primeBound = (sieveBound/taskLength > sieveLengthFactor) ? taskLength*sieveLengthFactor : sieveBound;

        Stream.Builder<BigInteger> results = Stream.builder();
        AtomicLong counter = new AtomicLong(0);
        BiConsumer<BigInteger[], Integer> resultConsumer = (newResults, newCount) -> {
            synchronized (results) {
                if (newResults != null) {
                    for (BigInteger x : newResults) {
                        results.add(x);
                    }
                }
            }
            counter.addAndGet(newCount);
        };

        ModPowCalculator modPowCalculator = modPowCalculatorFactory.createCalculator(C);

        Task[] tasks = new Task[(int) tasksNumber];
        BigInteger start = B;
        BigInteger smallerStep = A.multiply(BigInteger.valueOf(taskLength));
        BigInteger biggerStep = smallerStep.add(A);
        long controlSum = 0;
        for (int i = 0; i < tasksNumber; i++) {
            int curLength = (i < plusOne) ? taskLength+1 : taskLength;
            tasks[i] = new Task(C, start, A, curLength, primeBound, modPowCalculator, resultConsumer);
            start = start.add((i < plusOne) ? biggerStep : smallerStep);
            controlSum += curLength;
        }
        assert controlSum == length;

        if ((threadsNumber < 2) || (executor == null)) {
            for (Task task : tasks) {
                task.run();
            }
        } else {
            Future<?>[] futures = new Future<?>[tasks.length];
            for (int i = 0; i < tasks.length; i++) {
                futures[i] = executor.submit(tasks[i]);
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return Pair.create(results.build().toArray(BigInteger[]::new), counter.get());
    }

    private BitSet generateBitSet(BigInteger start, BigInteger step, int length, long primeBound) {
        BigInteger bound = BigInteger.valueOf(primeBound);
        Map<BigInteger, Integer> toSkip = new HashMap<>();
        BigInteger cur = start;
        for (int i = 0; (i < length) && (cur.compareTo(bound) <= 0); i++) {
            toSkip.put(cur, i);
            cur = cur.add(step);
        }

        BitSet bitSet = new BitSet(length);
        for (int i = 0; (i < primes.length) && (primes[i].compareTo(bound) <= 0); i++) {
            int a = step.mod(primes[i]).intValueExact();
            long b = start.mod(primes[i]).intValueExact();
            int p = primes[i].intValueExact();
            int pStart;
            int pStep;
            if (b == 0) {
                pStart = 0;
                if (a == 0) {
                    return null;
                } else {
                    pStep = p;
                }
            } else {
                pStart = (int) Common.mod(-b*inv[i][a], p);
                if (a == 0) {
                    pStep = -1;
                } else {
                    pStep = p;
                }
            }

            if (pStep > 0) {
                for (int j = pStart; j < length; j += pStep) {
                    bitSet.set(j);
                }

                Integer r = toSkip.get(primes[i]);
                if (r != null) {
                    bitSet.clear(r);
                }
            }
        }
        return bitSet;
    }

    private class Task implements Runnable {
        private final BigInteger multiplier;
        private final BigInteger start;
        private final BigInteger step;
        private final int length;
        private final long primeBound;
        private final ModPowCalculator modPowCalculator;
        private final BiConsumer<BigInteger[], Integer> resultConsumer;

        private Task(BigInteger multiplier, BigInteger start, BigInteger step, int length, long primeBound,
                     ModPowCalculator modPowCalculator, BiConsumer<BigInteger[], Integer> resultConsumer)
        {
            this.multiplier = multiplier;
            this.start = start;
            this.step = step;
            this.length = length;
            this.primeBound = primeBound;
            this.modPowCalculator = modPowCalculator;
            this.resultConsumer = resultConsumer;
        }

        @Override
        public void run() {
            BitSet bits = generateBitSet(start, step, length, primeBound);
            if (bits == null) {
                resultConsumer.accept(null, 0);
                return;
            }
            BigInteger M = start;

            IntObjectMap<BigInteger> mSteps = new IntObjectHashMap<>();
            mSteps.put(1, step);

            Stream.Builder<BigInteger> result = null;
            int counter = 0;
            int prev = 0;
            for (int i = 0; i < length; i++) {
                if (bits.get(i)) {
                    continue;
                }
                int move = i - prev;
                if (move > 0) {
                    BigInteger addM = mSteps.get(move);
                    if (addM == null) {
                        BigInteger moveBI = BigInteger.valueOf(move);
                        addM = step.multiply(moveBI);
                        mSteps.put(move, addM);
                    }
                    M = M.add(addM);
                    prev = i;
                }

                counter++;
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
            resultConsumer.accept((result != null) ? result.build().toArray(BigInteger[]::new) : null, counter);

            if (length >= scanLogThreshold) {
                long taskStart = start.divide(step).longValueExact();
                long taskEnd = taskStart + length;
                log.info("Scanned for interval [{}, {}), checked {} candidates", taskStart, taskEnd, counter);
            }
        }
    }
}