package factorization;

import common.TaskExecutor;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class GmpEcmFactorizer extends Factorizer {

    private final long B1;
    private final long C;
    private final TaskExecutor taskExecutor;

    public GmpEcmFactorizer(long B1, long C, TaskExecutor taskExecutor, int primeTestCertainty) {
        super(primeTestCertainty);
        this.B1 = B1;
        this.C = (taskExecutor != null) ? (long) Math.ceil(C * 1.0 / taskExecutor.getThreadsNumber()) : C;
        this.taskExecutor = taskExecutor;
    }

    @Override
    protected Factorization factorizeInternal(BigInteger N) {
        Future<BigInteger>[] tasks = new Future[taskExecutor.getThreadsNumber()];
        BlockingQueue<BigInteger> queue = new LinkedBlockingQueue<>();
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = taskExecutor.submit(() -> {
                // todo maybe there is way to reuse process for several factorization runs instead of creating new one every time?
                // if i pass several numbers one by one, gmp-ecm process quits with error after several iterations:
                // "Error, -bsaves/-bloads makes sense in batch mode only"

                try (GmpEcmProcess gmpEcm = GmpEcmProcess.create(B1, C)) {
                    BigInteger d = gmpEcm.tryFindFactor(N);
                    queue.add((d != null) ? d : BigInteger.ONE);
                    return d;
                }
            });
        }

        BigInteger D = null;
        for (int i = 0; i < tasks.length; i++) {
            try {
                BigInteger d = queue.take();
                if (N.equals(d)) {
                    D = N;
                } else if (!BigInteger.ONE.equals(d)) {
                    D = d;
                    for (Future<BigInteger> task : tasks) {
                        task.cancel(true);
                    }
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (N.equals(D)) {
            D = PollardRho.findDivisor(N, Long.MAX_VALUE);
        }

        if (D == null) {
            return Factorization.fromSingleFactor(N, false);
        } else {
            return processDivisor(D, N, this::factorizeInternal);
        }
    }

}
