package code.donbonifacio.scavenger8.processors;

import code.donbonifacio.scavenger8.WorkerPoolWithGateKeeper;

/**
 * This trait represents a service that can process a stream of PageInfos'
 */
public interface PipelineProcessor {

    /**
     * Gets the worker pool in usage.
     *
     * @return the worker pool
     */
    default WorkerPoolWithGateKeeper getWorkerPool() {
        throw new IllegalArgumentException("No worker pool");
    }

    /**
     * Starts the service.
     */
    default void start() {
        getWorkerPool().start();
    }

    /**
     * True if this service is shutdown.
     *
     * @return true if it's shutdown
     */
    default boolean isShutdown() {
        return getWorkerPool().isShutdown();
    }

    /**
     * Gets the number of current tasks running and/or scheduled to run.
     *
     * @return the count of tasks
     */
    default long getTaskCount() {
        return getWorkerPool().getTaskCount();
    }

    /**
     * Gets the number of processed tasks.
     *
     * @return the count of processed tasks
     */
    default long getProcessedCount() {
        return getWorkerPool().getProcessedCount();
    }

}
