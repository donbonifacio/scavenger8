package code.donbonifacio.scavenger8.processors;

import java.util.concurrent.BlockingQueue;

/**
 * Represents an object that can create a Runnable taks that is able to process
 * a given parameter T, and put it on an output queue.
 *
 * @param <T> the class of the object to process
 */
@FunctionalInterface
public interface RunnableWithQueueFactory<T> {

    /**
     * Creates a Runnable to process the object and put it on the output queue.
     *
     * @param object the object to process
     * @param outputQueue the output queue
     * @return a runnable task
     */
    Runnable create(T object, BlockingQueue<T> outputQueue);

}
