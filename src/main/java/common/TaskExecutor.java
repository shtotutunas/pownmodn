package common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class TaskExecutor {

    private final int threadsNumber;
    private final ExecutorService executor;

    public static TaskExecutor create(int threadsNumber) {
        assert threadsNumber >= 0;
        if (threadsNumber < 2) {
            return new TaskExecutor(1, null);
        } else {
            return new TaskExecutor(threadsNumber, Executors.newFixedThreadPool(threadsNumber));
        }
    }

    private TaskExecutor(int threadsNumber, ExecutorService executor) {
        this.threadsNumber = threadsNumber;
        this.executor = executor;
    }

    public int getThreadsNumber() {
        return threadsNumber;
    }

    public <T> Future<T> submit(Supplier<T> task) {
        if (executor != null) {
            return executor.submit(task::get);
        } else {
            return CompletableFuture.completedFuture(task.get());
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
