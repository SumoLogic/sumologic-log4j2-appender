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

package com.sumologic.log4j;

import com.sumologic.log4j.aggregation.SumoBufferFlusher;
import com.sumologic.log4j.http.HttpProxySettingsCreator;
import com.sumologic.log4j.http.ProxySettings;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import com.sumologic.log4j.queue.BufferWithFifoEviction;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * Log4J 2 Appender that sends log messages to Sumo Logic.
 *
 * @author Ryan Miller (rmiller@sumologic.com)
 */
@Plugin(name = "SumoLogicAppender", category = "Core", elementType = "appender", printObject = true)
public class SumoLogicAppender extends AbstractAppender
{

  // User-definable defaults
  private static final int DEFAULT_CONNECTION_TIMEOUT = 1000;         // Connection timeout (ms)
  private static final int DEFAULT_SOCKET_TIMEOUT = 60000;            // Socket timeout (ms)
  private static final int DEFAULT_RETRY_INTERVAL = 10000;            // If a request fails, how often do we retry.
  private static final long DEFAULT_MESSAGES_PER_REQUEST = 100;       // How many messages need to be in the queue before we flush
  private static final long DEFAULT_MAX_FLUSH_INTERVAL = 10000;       // Maximum interval between flushes (ms)
  private static final long DEFAULT_FLUSHING_ACCURACY = 250;          // How often the flushed thread looks into the message queue (ms)
  private static final long DEFAULT_MAX_QUEUE_SIZE_BYTES = 1000000;   // Maximum message queue size (bytes)
  private static final long DEFAULT_MAX_FLUSH_TIMEOUT_MS =
      (DEFAULT_CONNECTION_TIMEOUT + DEFAULT_SOCKET_TIMEOUT + DEFAULT_RETRY_INTERVAL) * 3 + DEFAULT_FLUSHING_ACCURACY;
  private static final Logger logger = StatusLogger.getLogger();


  private final BufferWithEviction<byte[]> queue;
  private final SumoHttpSender sender;
  private final SumoBufferFlusher flusher;
  private final Filter filter;
  private final CloseableHttpClient httpClient;

  protected SumoLogicAppender(
      final String name,
      final Filter filter,
      final Layout<? extends Serializable> layout,
      final boolean ignoreExceptions,
      final String url, ProxySettings proxySettings,
      final int retryInterval,
      final int connectionTimeout,
      final int socketTimeout,
      final long messagesPerRequest,
      final long maxFlushInterval,
      final String sourceName,
      final String sourceHost,
      final String sourceCategory,
      final long flushingAccuracy,
      final long maxQueueSizeBytes,
      final long maxFlushTimeout
  )
  {
    super(name, filter, layout, ignoreExceptions);
    this.filter = filter;

    // Initialize queue
    queue = new BufferWithFifoEviction<>(maxQueueSizeBytes, e -> {
      // Note: This is only an estimate for total byte usage, since in UTF-8 encoding,
      // the size of one character may be > 1 byte.
      return e.length;
    });

    final RequestConfig requestConfig = RequestConfig
        .custom()
        .setSocketTimeout(socketTimeout)
        .setConnectTimeout(connectionTimeout)
        .build();

    final HttpClientBuilder builder = HttpClients
        .custom()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .setDefaultRequestConfig(requestConfig);

    final HttpProxySettingsCreator creator = new HttpProxySettingsCreator(proxySettings);
    creator.configureProxySettings(builder);

    httpClient = builder.build();
    // Initialize sender
    sender = new SumoHttpSender(
        retryInterval,
        url,
        httpClient,
        sourceName,
        sourceHost,
        sourceCategory
    );

    // Initialize flusher
    flusher = new SumoBufferFlusher(
        flushingAccuracy,
        messagesPerRequest,
        maxFlushInterval,
        maxFlushTimeout,
        sender,
        queue,
        logger
    );

    if (flushingAccuracy + socketTimeout + connectionTimeout >= maxFlushTimeout) {
      logger.warn("Max flush timeout is small! Logs during shutdown likely to be lost");
    }
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
      @PluginAttribute(value = "messagesPerRequest", defaultLong = DEFAULT_MESSAGES_PER_REQUEST)
          Long messagesPerRequest,
      @PluginAttribute(value = "maxFlushInterval", defaultLong = DEFAULT_MAX_FLUSH_INTERVAL) Long maxFlushInterval,
      @PluginAttribute(value = "maxFlushTimeout", defaultLong = DEFAULT_MAX_FLUSH_TIMEOUT_MS) Long maxFlushTimeout,
      @PluginAttribute(value = "sourceName") String sourceName,
      @PluginAttribute(value = "sourceHost") String sourceHost,
      @PluginAttribute(value = "sourceCategory") String sourceCategory,
      @PluginAttribute(value = "flushingAccuracy", defaultLong = DEFAULT_FLUSHING_ACCURACY) Long flushingAccuracy,
      @PluginAttribute(value = "maxQueueSizeBytes", defaultLong = DEFAULT_MAX_QUEUE_SIZE_BYTES) Long maxQueueSizeBytes
  )
  {

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

    ProxySettings proxySettings = new ProxySettings(
        proxyHost,
        proxyPort,
        proxyAuth,
        proxyUser,
        proxyPassword,
        proxyDomain
    );

    return new SumoLogicAppender(
        name,
        filter,
        layout,
        true,
        url,
        proxySettings,
        // To make sure nothing is being weird
        Optional.ofNullable(retryInterval).orElse(DEFAULT_RETRY_INTERVAL),
        Optional.ofNullable(connectionTimeout).orElse(DEFAULT_CONNECTION_TIMEOUT),
        Optional.ofNullable(socketTimeout).orElse(DEFAULT_SOCKET_TIMEOUT),
        Optional.ofNullable(messagesPerRequest).orElse(DEFAULT_MESSAGES_PER_REQUEST),
        Optional.ofNullable(maxFlushInterval).orElse(DEFAULT_MAX_FLUSH_INTERVAL),
        sourceName,
        Optional.ofNullable(sourceHost).orElse(ManagementFactory.getRuntimeMXBean().getName()),
        sourceCategory,
        Optional.ofNullable(flushingAccuracy).orElse(DEFAULT_FLUSHING_ACCURACY),
        Optional.ofNullable(maxQueueSizeBytes).orElse(DEFAULT_MAX_QUEUE_SIZE_BYTES),
        Optional.ofNullable(maxFlushTimeout).orElse(DEFAULT_MAX_FLUSH_TIMEOUT_MS)
    );
  }

  public void append(LogEvent event)
  {
    if (!checkEntryConditions()) {
      logger.warn("Appender not initialized. Dropping log entry");
      return;
    }

    final byte[] message = getLayout().toByteArray(event);

    try {
      queue.add(message);
    }
    catch (Exception e) {
      logger.error("Unable to insert log entry into log queue.", e);
    }
  }

  @Override
  public void start()
  {
    setStarting();
    if (filter != null) {
      filter.start();
    }
    flusher.start();
    setStarted();
  }

  @Override
  public void stop()
  {
    setStopping();
    // Add suppressed exceptions, and only throw if we had suppressed exceptions
    final Exception exception = new RuntimeException("Error stopping SumoLogicAppender");
    try {
      try {
        flusher.stop();
      }
      catch (Exception e) {
        exception.addSuppressed(e);
      }

      if (filter != null) {
        try {
          filter.stop();
        }
        catch (Exception e) {
          exception.addSuppressed(e);
        }
      }
      try {
        httpClient.close();
      }
      catch (Exception e) {
        exception.addSuppressed(e);
      }
      if (exception.getSuppressed().length > 0) {
        throw exception;
      }
    }
    catch (Exception e) {
      logger.error("Unable to close appender", e);
    }
    setStopped();
  }

  // Private bits.

  private boolean checkEntryConditions()
  {
    return sender != null && sender.isInitialized();
  }

}
