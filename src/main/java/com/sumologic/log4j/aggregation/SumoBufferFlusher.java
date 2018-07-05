/**
 *    _____ _____ _____ _____    __    _____ _____ _____ _____
 *   |   __|  |  |     |     |  |  |  |     |   __|     |     |
 *   |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 *   |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 *                UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.http.SumoBufferFlushingTask;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;

import java.util.concurrent.*;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher {
    private SumoBufferFlushingTask flushingTask;
    private ScheduledFuture future;
    private ScheduledExecutorService executor;
    private long flushingAccuracy;


    public SumoBufferFlusher(
            long flushingAccuracy,
            long messagesPerRequest,
            long maxFlushInterval,
            SumoHttpSender sender,
            BufferWithEviction<String> buffer) {
        this.flushingAccuracy = flushingAccuracy;
        flushingTask = new SumoBufferFlushingTask(buffer);
        flushingTask.setMessagesPerRequest(messagesPerRequest);
        flushingTask.setMaxFlushInterval(maxFlushInterval);
        flushingTask.setSender(sender);
    }

    public void start() {
        /* Start flushing! */

        executor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("SumoBufferFlusherThread");
                    thread.setDaemon(true);
                    return thread;
                }
            });


        future =
            executor.
                scheduleAtFixedRate(flushingTask, 0, flushingAccuracy, TimeUnit.MILLISECONDS);

    }


    public void stop() {
        // Keep the current task running until it's done sending
        if (future != null) {
            future.cancel(false);
            future = null;
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        // To satisfy needsFlushing in BufferFlushingTask for last flush before dying
        flushingTask.setMessagesPerRequest(1L);
        flushingTask.run();
    }


}
