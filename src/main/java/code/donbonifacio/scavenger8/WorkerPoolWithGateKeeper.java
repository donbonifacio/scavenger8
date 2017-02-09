package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.processors.RunnableWithQueueFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implements a worker pool that supports the processing of tasks that
 * process a PageInfo retrieved from an input queue, and put it on an
 * output queue.
 *
 * It creates a single thread executor that acts as a gate keeper. It
 * will gather PageInfo's, register metrics, process them via a runnable
 * factory and manage the executor's lifecycle.
 */
public final class WorkerPoolWithGateKeeper {

    private final BlockingQueue<PageInfo> inputQueue;
    private final BlockingQueue<PageInfo> outputQueue;
    private final ExecutorService workerPool;
    private final ExecutorService gateKeeper;
    private final AtomicLong taskCounter = new AtomicLong();
    private final AtomicLong processedCounter = new AtomicLong();
    private final RunnableWithQueueFactory<PageInfo> taskFactory;
    private final int nThreads;

    /**
     * Creates a new WorkerPoolWithGateKeeper.
     *
     * @param input the input queue
     * @param output the output queue
     * @param nThreads the number of worker threads to support
     * @param taskFactory a factory that creates taks
     */
    public WorkerPoolWithGateKeeper(final BlockingQueue<PageInfo> input,
                                    final BlockingQueue<PageInfo> output,
                                    final int nThreads,
                                    final RunnableWithQueueFactory<PageInfo> taskFactory) {
        this.inputQueue = checkNotNull(input);
        this.outputQueue = checkNotNull(output);
        this.taskFactory = checkNotNull(taskFactory);
        this.workerPool = Executors.newFixedThreadPool(nThreads);
        this.gateKeeper = Executors.newSingleThreadExecutor();
        checkArgument(nThreads > 0 && nThreads < 200, "Invalid number of threads");
        this.nThreads = nThreads;
    }

    /**
     * Utility class that will gather work, and send it to the worker pool.
     */
    private class Runner implements Runnable {

        /**
         * Main loop, gathers and distributes work.
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (taskCounter.get() >= nThreads) {
                        // back pressure
                        Thread.sleep(1000);
                        continue;
                    }

                    PageInfo page = inputQueue.take();

                    if (PageInfo.isPoison(page)) {
                        workerPool.shutdown();
                        workerPool.awaitTermination(10, TimeUnit.MINUTES);
                        outputQueue.put(page);
                        Thread.currentThread().interrupt();
                    } else {
                        taskCounter.incrementAndGet();
                        workerPool.execute(() -> {
                            Runnable runnable = taskFactory.create(page, outputQueue);
                            runnable.run();
                            taskCounter.decrementAndGet();
                            processedCounter.incrementAndGet();
                        });
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }

    }

    /**
     * Starts the main runner, in another Thread, and returns right away.
     */
    public void start() {
        gateKeeper.execute(new Runner());
        gateKeeper.shutdown();
    }

    /**
     * True if this service is shutdown.
     *
     * @return true if it's shutdown
     */
    public boolean isShutdown() {
        return workerPool.isShutdown();
    }

    /**
     * Gets the number of current tasks running and/or scheduled to run.
     *
     * @return the count of tasks
     */
    public long getTaskCount() {
        return taskCounter.get();
    }

    /**
     * Gets the number of processed tasks.
     *
     * @return the count of processed tasks
     */
    public long getProcessedCount() {
        return processedCounter.get();
    }

}
