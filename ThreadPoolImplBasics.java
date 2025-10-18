import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolImplBasics {

    private final BlockingQueue<Runnable> taskQueue;

    private final WorkThread[] workers;

    private volatile boolean isShutdown = false;


    private static final AtomicInteger poolNumer = new AtomicInteger(1);
    private final int poolId;


    public ThreadPoolImplBasics(int numOfThreads, int queueCapacity){
        if(numOfThreads <= 0){
            throw new IllegalArgumentException("Number of threads and queue capacity must be greater than zero.");
        }

        this.poolId = poolNumer.getAndIncrement();

        this.taskQueue = (queueCapacity > 0)? new LinkedBlockingDeque<>(queueCapacity)
                                           : new LinkedBlockingDeque<>();

        this.workers = new WorkThread[numOfThreads];

        for(int i = 0; i < numOfThreads; i++){
            workers[i] = new WorkThread(i);
            workers[i].start();
        }

        System.out.println("Thread Pool-" + poolId + " created with " + numOfThreads + " threads.");
    }


    public void submit(Runnable task){
        if(isShutdown){
            throw new IllegalStateException("ThreadPool is shutdown. Cannot accept new tasks.");
        }

        if(task == null){
            throw new IllegalArgumentException("Task cannot be null.");
        }

        try{
            taskQueue.put(task);
            System.out.println("Task submitted to Thread Pool-" + poolId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while submitting task.", e);
        }
    }

    public void shutdown(){
        isShutdown = true;
        for(WorkThread worker : workers){
            worker.interrupt();
        }
        System.out.println("Thread Pool-" + poolId + " is shutting down.");
    }

    public void awaitTermination() {
        for (WorkThread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for termination.", e);
            }
        }
        System.out.println("Thread Pool-" + poolId + " has terminated.");
    }

    public int getQueueSize() {
        System.out.println("Current Queue Size in Thread Pool-" + poolId + ": " + taskQueue.size());
        return taskQueue.size();
    }

    private class WorkThread extends Thread {
        private final int workerId;
        private int taskExecuted = 0;

        public WorkThread(int workerId){
            this.workerId = workerId;
        }

        @Override
        public void run() {

            try{
                while(!isInterrupted()){
                    Runnable task = taskQueue.take();
                    System.out.println("Worker-" + workerId + " in Thread Pool-" + poolId + " executing task.");
                    try {
                        task.run();
                        taskExecuted++;
                    } catch (RuntimeException e) {
                        System.err.println("Task execution failed in Worker-" + workerId + " of Thread Pool-" + poolId);
                        e.printStackTrace();
                    }
                }
            }catch (InterruptedException e) {
                // Thread interrupted during take or sleep
                System.out.println(getName() + " interrupted, will terminate");
            } finally {
                System.out.println("Worker-" + workerId + " in Thread Pool-" + poolId + " terminating. Total tasks executed: " + taskExecuted);
            }
        }
    }

    public static void main(String[] args) {
        ThreadPoolImplBasics threadPool = new ThreadPoolImplBasics(3, 5);

        for(int i = 1; i <= 10; i++){
            final int taskId = i;
            threadPool.submit(() -> {
                System.out.println("Task " + taskId + " is running.");
                try {
                    Thread.sleep(1000); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " completed.");
            });
        }

        threadPool.getQueueSize();

        threadPool.shutdown();
        threadPool.awaitTermination();

        ThreadPoolImplBasics threadPool2 = new ThreadPoolImplBasics(3, 5);

        for(int i = 1; i <= 10; i++){
            final int taskId = i;
            threadPool2.submit(() -> {
                System.out.println("Task " + taskId + " is running.");
                try {
                    Thread.sleep(1000); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " completed.");
            });
        }

        threadPool2.getQueueSize();

        threadPool2.shutdown();
        threadPool2.awaitTermination();
    }
    
}
