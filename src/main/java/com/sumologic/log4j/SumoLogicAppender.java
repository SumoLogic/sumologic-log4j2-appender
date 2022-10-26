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
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
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
    private static final int DEFAULT_MESSAGES_PER_REQUEST = 100;        // How many messages need to be in the queue before we flush
    private static final long DEFAULT_MAX_FLUSH_INTERVAL = 10000;       // Maximum interval between flushes (ms)
    private static final long DEFAULT_FLUSHING_ACCURACY = 250;          // How often the flushed thread looks into the message queue (ms)
    private static final long DEFAULT_MAX_QUEUE_SIZE_BYTES = 1000000;   // Maximum message queue size (bytes)
    private static final boolean FLUSH_ALL_MESSAGES_BEFORE_STOPPING = false;   // Flush Before Stoping irrespective of  flushingAccuracy
    private static final String DEFAULT_RETRY_HTTP_CODE_REGEX = "^5.*"; // Retry for any 5xx HTTP response code

    private SumoHttpSender sender;
    private SumoBufferFlusher flusher;
    volatile private BufferWithEviction<String> queue;
    private static final Logger logger = StatusLogger.getLogger();
    private static final String CLIENT_NAME = "log4j2-appender";

    protected SumoLogicAppender(String name, Filter filter,
                                Layout<? extends Serializable> layout, final boolean ignoreExceptions,
                                String url, ProxySettings proxySettings,
                                Integer retryInterval, Integer connectionTimeout, Integer socketTimeout,
                                Integer messagesPerRequest, Long maxFlushInterval, String sourceName,
                                String sourceCategory, String sourceHost,
                                Long flushingAccuracy, Long maxQueueSizeBytes, Boolean flushAllBeforeStopping,
                                String retryableHttpCodeRegex) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);

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
        sender.setRetryIntervalMs(retryInterval);
        sender.setConnectionTimeoutMs(connectionTimeout);
        sender.setSocketTimeoutMs(socketTimeout);
        sender.setUrl(url);
        sender.setSourceName(sourceName);
        sender.setSourceCategory(sourceCategory);
        sender.setSourceHost(sourceHost);
        sender.setProxySettings(proxySettings);
        sender.setClientHeaderValue(CLIENT_NAME);
        sender.setRetryableHttpCodeRegex(retryableHttpCodeRegex);
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

    /**
     * Deprecated method. Please use the builder API by calling {@link #newBuilder() newBuilder()} function.
     */
    @PluginFactory
    @Deprecated
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
            @PluginAttribute(value = "messagesPerRequest", defaultInt = DEFAULT_MESSAGES_PER_REQUEST) Integer messagesPerRequest,
            @PluginAttribute(value = "maxFlushInterval", defaultLong = DEFAULT_MAX_FLUSH_INTERVAL) Long maxFlushInterval,
            @PluginAttribute(value = "sourceName") String sourceName,
            @PluginAttribute(value = "sourceCategory") String sourceCategory,
            @PluginAttribute(value = "sourceHost") String sourceHost,
            @PluginAttribute(value = "flushingAccuracy", defaultLong = DEFAULT_FLUSHING_ACCURACY) Long flushingAccuracy,
            @PluginAttribute(value = "maxQueueSizeBytes", defaultLong = DEFAULT_MAX_QUEUE_SIZE_BYTES) Long maxQueueSizeBytes,
            @PluginAttribute(value = "flushAllBeforeStopping", defaultBoolean = FLUSH_ALL_MESSAGES_BEFORE_STOPPING) Boolean flushAllBeforeStopping,
            @PluginAttribute(value = "retryableHttpCodeRegex", defaultString = DEFAULT_RETRY_HTTP_CODE_REGEX) String retryableHttpCodeRegex) {

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
                sourceHost, flushingAccuracy, maxQueueSizeBytes, flushAllBeforeStopping, retryableHttpCodeRegex);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SumoLogicAppender> {
        private final int DEFAULT_CONNECTION_TIMEOUT = 1000;
        private final int DEFAULT_SOCKET_TIMEOUT = 60000;
        private final int DEFAULT_RETRY_INTERVAL = 10000;
        private final int DEFAULT_MESSAGES_PER_REQUEST = 100;
        private final long DEFAULT_MAX_FLUSH_INTERVAL = 10000L;
        private final long DEFAULT_FLUSHING_ACCURACY = 250L;
        private final long DEFAULT_MAX_QUEUE_SIZE_BYTES = 1000000L;
        private final String DEFAULT_RETRY_HTTP_CODE_REGEX = "^5.*";
        private final boolean FLUSH_ALL_MESSAGES_BEFORE_STOPPING = false;

        @PluginBuilderAttribute
        @Required(message = "Name is required for SumoLogicAppender")
        private String name;
        @PluginElement("layout")
        private Layout<? extends Serializable> layout;
        @PluginElement("Filter")
        private Filter filter;
        @PluginBuilderAttribute
        @Required(message = "Url is required for SumoLogicAppender")
        private String url;
        @PluginBuilderAttribute
        private String proxyAuth;
        @PluginBuilderAttribute
        private String proxyHost;
        @PluginBuilderAttribute
        private Integer proxyPort = 0;
        @PluginBuilderAttribute
        private String proxyUser;
        @PluginBuilderAttribute
        private String proxyPassword;
        @PluginBuilderAttribute
        private String proxyDomain;
        @PluginBuilderAttribute
        private int retryInterval = DEFAULT_RETRY_INTERVAL;
        @PluginBuilderAttribute
        private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        @PluginBuilderAttribute
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        @PluginBuilderAttribute
        private int messagesPerRequest = DEFAULT_MESSAGES_PER_REQUEST;
        @PluginBuilderAttribute
        private long maxFlushInterval = DEFAULT_MAX_FLUSH_INTERVAL;
        @PluginBuilderAttribute
        private String sourceName;
        @PluginBuilderAttribute
        private String sourceCategory;
        @PluginBuilderAttribute
        private String sourceHost;
        @PluginBuilderAttribute
        private long flushingAccuracy = DEFAULT_FLUSHING_ACCURACY;
        @PluginBuilderAttribute
        private long maxQueueSizeBytes = DEFAULT_MAX_QUEUE_SIZE_BYTES;
        @PluginBuilderAttribute
        private boolean flushAllBeforeStopping = FLUSH_ALL_MESSAGES_BEFORE_STOPPING;
        @PluginBuilderAttribute
        private String retryableHttpCodeRegex = DEFAULT_RETRY_HTTP_CODE_REGEX;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        public Builder setFilter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setUrl(final String url) {
            this.url = url;
            return this;
        }

        public Builder setProxyAuth(final String proxyAuth) {
            this.proxyAuth = proxyAuth;
            return this;
        }

        public Builder setProxyHost(final String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder setProxyPort(final Integer proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder setProxyUser(final String proxyUser) {
            this.proxyUser = proxyUser;
            return this;
        }

        public Builder setProxyPassword(final String proxyPassword) {
            this.proxyPassword = proxyPassword;
            return this;
        }

        public Builder setProxyDomain(final String proxyDomain) {
            this.proxyDomain = proxyDomain;
            return this;
        }

        public Builder setRetryInterval(final int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public Builder setConnectionTimeout(final int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setSocketTimeout(final int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder setMessagesPerRequest(final int messagesPerRequest) {
            this.messagesPerRequest = messagesPerRequest;
            return this;
        }

        public Builder setMaxFlushInterval(final long maxFlushInterval) {
            this.maxFlushInterval = maxFlushInterval;
            return this;
        }

        public Builder setSourceName(final String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        public Builder setSourceCategory(final String sourceCategory) {
            this.sourceCategory = sourceCategory;
            return this;
        }

        public Builder setSourceHost(final String sourceHost) {
            this.sourceHost = sourceHost;
            return this;
        }

        public Builder setFlushingAccuracy(final long flushingAccuracy) {
            this.flushingAccuracy = flushingAccuracy;
            return this;
        }

        public Builder setMaxQueueSizeBytes(final long maxQueueSizeBytes) {
            this.maxQueueSizeBytes = maxQueueSizeBytes;
            return this;
        }

        public Builder setFlushAllBeforeStopping(final boolean flushAllBeforeStopping) {
            this.flushAllBeforeStopping = flushAllBeforeStopping;
            return this;
        }

        public Builder setRetryableHttpCodeRegex(final String retryableHttpCodeRegex) {
            this.retryableHttpCodeRegex = retryableHttpCodeRegex;
            return this;
        }

        @Override
        public SumoLogicAppender build() {
            return SumoLogicAppender.createAppender(name, layout, filter, url, proxyAuth, proxyHost, proxyPort, proxyUser,
                    proxyPassword, proxyDomain, retryInterval, connectionTimeout, socketTimeout, messagesPerRequest, maxFlushInterval,
                    sourceName, sourceCategory, sourceHost, flushingAccuracy, maxQueueSizeBytes, flushAllBeforeStopping, retryableHttpCodeRegex);
        }
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
