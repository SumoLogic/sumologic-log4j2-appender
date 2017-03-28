/**
 *  _____ _____ _____ _____    __    _____ _____ _____ _____
 * |   __|  |  |     |     |  |  |  |     |   __|     |     |
 * |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 * |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 * UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.http.SumoBufferFlusherThread;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.logging.log4j.Logger;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher
{
  private final long maxFlushTimeoutMs;
  private final Logger logger;
  private final SumoBufferFlusherThread flushingThread;
  private final long flushingAccuracy;


  public SumoBufferFlusher(
      long flushingAccuracy,
      long messagesPerRequest,
      long maxFlushInterval,
      long maxFlushTimeoutMs,
      SumoHttpSender sender,
      BufferWithEviction<byte[]> buffer,
      Logger logger
  )
  {
    this.flushingAccuracy = flushingAccuracy;
    this.maxFlushTimeoutMs = maxFlushTimeoutMs;
    this.logger = logger;

    flushingThread = new SumoBufferFlusherThread(
        buffer,
        sender,
        flushingAccuracy,
        maxFlushInterval,
        messagesPerRequest
    );
  }

  public void start()
  {
    flushingThread.start();
  }

  public void stop() throws InterruptedException
  {
    flushingThread.setTerminating();
    flushingThread.interrupt();

    // Keep the current task running until it's done sending

    flushingThread.join(maxFlushTimeoutMs + flushingAccuracy + 1);
    if (flushingThread.isAlive()) {
      logger.warn("Timed out waiting for buffer flusher to finish.");
    }
  }
}
