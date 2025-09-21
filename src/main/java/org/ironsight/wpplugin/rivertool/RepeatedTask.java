package org.ironsight.wpplugin.rivertool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RepeatedTask {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void startTask(Runnable task) {

        // Schedule the task to run every 5 seconds
        scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);
    }

    public void stopTask() {
        // Shutdown the scheduler when you want to stop the task
        scheduler.shutdown();
    }
}

