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

package com.sumologic.log4j;

import com.sumologic.http.aggregation.SumoBufferFlusher;
import com.sumologic.http.sender.ProxySettings;
import com.sumologic.http.sender.SumoHttpSender;
import com.sumologic.http.queue.BufferWithEviction;
import com.sumologic.http.queue.BufferWithFifoEviction;
import com.sumologic.http.queue.CostBoundedConcurrentQueue;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Log4J 2 Appender that sends log messages to Sumo Logic.
 *
 * @author Ryan Miller (rmiller@sumologic.com)
 */
@Plugin(name="SumoLogicAppender", category="Core", elementType="appender", printObject=true)
public class SumoLogicAppender extends AbstractAppender {

    // User-definable defaults
    private static final int DEFAULT_CONNECTION_TIMEOUT = 1000;         // Connection timeout (ms)
    private static final int DEFAULT_SOCKET_TIMEOUT = 60000;            // Socket timeout (ms)
    private static final int DEFAULT_RETRY_INTERVAL = 10000;            // If a request fails, how often do we retry.
    private static final long DEFAULT_MESSAGES_PER_REQUEST = 100;       // How many messages need to be in the queue before we flush
    private static final long DEFAULT_MAX_FLUSH_INTERVAL = 10000;       // Maximum interval between flushes (ms)
    private static final long DEFAULT_FLUSHING_ACCURACY = 250;          // How often the flushed thread looks into the message queue (ms)
    private static final long DEFAULT_MAX_QUEUE_SIZE_BYTES = 1000000;   // Maximum message queue size (bytes)
    private static final boolean FLUSH_ALL_MESSAGES_BEFORE_STOPPING = false;   // Flush Before Stoping irrespective of  flushingAccuracy

    private SumoHttpSender sender;
    private SumoBufferFlusher flusher;
    volatile private BufferWithEviction<String> queue;
    private static final Logger logger = StatusLogger.getLogger();
    private static final String CLIENT_NAME = "log4j2-appender";

    protected SumoLogicAppender(String name, Filter filter,
                                Layout<? extends Serializable> layout, final boolean ignoreExceptions,
                                String url, ProxySettings proxySettings,
                                Integer retryInterval, Integer connectionTimeout, Integer socketTimeout,
                                Long messagesPerRequest, Long maxFlushInterval, String sourceName,
                                String sourceCategory, String sourceHost,
                                Long flushingAccuracy, Long maxQueueSizeBytes, Boolean flushAllBeforeStopping) {
        super(name, filter, layout, ignoreExceptions);

        // Initialize queue
        queue = new BufferWithFifoEviction<String>(maxQueueSizeBytes, new CostBoundedConcurrentQueue.CostAssigner<String>() {
         @Override
         public long cost(String e) {
             // Note: This is only an estimate for total byte usage, since in UTF-8 encoding,
             // the size of one character may be > 1 byte.
             return e.length();
         }
        });

        // Initialize sender
        sender = new SumoHttpSender();
        sender.setRetryInterval(retryInterval);
        sender.setConnectionTimeout(connectionTimeout);
        sender.setSocketTimeout(socketTimeout);
        sender.setUrl(url);
        sender.setSourceName(sourceName);
        sender.setSourceCategory(sourceCategory);
        sender.setSourceHost(sourceHost);
        sender.setProxySettings(proxySettings);
        sender.setClientHeaderValue(CLIENT_NAME);
        sender.init();

        // Initialize flusher
        flusher = new SumoBufferFlusher(flushingAccuracy,
                messagesPerRequest,
                maxFlushInterval,
                sender,
                queue,
                flushAllBeforeStopping);
        flusher.start();
    }

    @PluginFactory
    public static SumoLogicAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("url") String url,
            @PluginAttribute("proxyAuth") String proxyAuth,
            @PluginAttribute("proxyHost") String proxyHost,
            @PluginAttribute("proxyPort") Integer proxyPort,
            @PluginAttribute("proxyUser") String proxyUser,
            @PluginAttribute("proxyPassword") String proxyPassword,
            @PluginAttribute("proxyDomain") String proxyDomain,
            @PluginAttribute(value = "retryInterval", defaultInt = DEFAULT_RETRY_INTERVAL) Integer retryInterval,
            @PluginAttribute(value = "connectionTimeout", defaultInt = DEFAULT_CONNECTION_TIMEOUT) Integer connectionTimeout,
            @PluginAttribute(value = "socketTimeout", defaultInt = DEFAULT_SOCKET_TIMEOUT) Integer socketTimeout,
            @PluginAttribute(value = "messagesPerRequest", defaultLong = DEFAULT_MESSAGES_PER_REQUEST) Long messagesPerRequest,
            @PluginAttribute(value = "maxFlushInterval", defaultLong = DEFAULT_MAX_FLUSH_INTERVAL) Long maxFlushInterval,
            @PluginAttribute(value = "sourceName") String sourceName,
            @PluginAttribute(value = "sourceCategory") String sourceCategory,
            @PluginAttribute(value = "sourceHost") String sourceHost,
            @PluginAttribute(value = "flushingAccuracy", defaultLong = DEFAULT_FLUSHING_ACCURACY) Long flushingAccuracy,
            @PluginAttribute(value = "maxQueueSizeBytes", defaultLong = DEFAULT_MAX_QUEUE_SIZE_BYTES) Long maxQueueSizeBytes,
            @PluginAttribute(value = "flushAllBeforeStopping", defaultBoolean = FLUSH_ALL_MESSAGES_BEFORE_STOPPING) Boolean flushAllBeforeStopping) {

        if (name == null) {
            logger.error("No name provided for SumoLogicAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        if (url == null) {
            logger.error("No url provided for SumoLogicAppender");
            return null;
        }

        ProxySettings proxySettings = new ProxySettings(proxyHost, proxyPort, proxyAuth, proxyUser, proxyPassword, proxyDomain);

        return new SumoLogicAppender(name, filter, layout, true, url, proxySettings, retryInterval, connectionTimeout,
                socketTimeout, messagesPerRequest, maxFlushInterval, sourceName, sourceCategory,
                sourceHost, flushingAccuracy, maxQueueSizeBytes, flushAllBeforeStopping);
    }

    public void append(LogEvent event) {
        if (!checkEntryConditions()) {
            logger.warn("Appender not initialized. Dropping log entry");
            return;
        }

        String message = new String(getLayout().toByteArray(event));
        logger.debug("Sending message to Sumo: " + message);

        try {
            queue.add(message);
        } catch (Exception e) {
            logger.error("Unable to insert log entry into log queue. ", e);
        }
    }

    public void setSourceName(String sourceName) {
        if (sender != null)
            sender.setSourceName(sourceName);
    }

    public void setSourceCategory(String sourceCategory) {
        if (sender != null) {
            sender.setSourceCategory(sourceCategory);
        }
    }

    public void setUrl(String url) {
        if (sender != null) {
            sender.setUrl(url);
        }
    }

  @Override
  public boolean stop(final long timeout, final TimeUnit timeUnit) {
      logger.debug("Stopping SumoLogicAppender {}", getName());
      setStopping();
      final boolean stopped = super.stop(timeout, timeUnit, false);
      try {
          flusher.stop();
          logger.debug("flusher has been stopped");
          flusher = null;

          sender.close();
          sender = null;

      } catch (Exception e) {
        logger.error("Unable to close appender", e);
      }
      setStopped();
      logger.debug("SumoLogicAppender {} has been stopped", getName());
      return stopped;
  }

  // Private bits.

  private boolean checkEntryConditions() {
    return sender != null && sender.isInitialized();
  }

}
