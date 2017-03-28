/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package com.sumologic.log4j.core;

import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.Charsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.util.Strings;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SumoJsonLayoutTest
{
  static ConfigurationFactory cf = new BasicConfigurationFactory();


  @AfterClass
  public static void cleanupClass()
  {
    ConfigurationFactory.removeConfigurationFactory(cf);
    ThreadContext.clearAll();
  }

  @BeforeClass
  public static void setupClass()
  {
    ThreadContext.clearAll();
    ConfigurationFactory.setConfigurationFactory(cf);
    final LoggerContext ctx = LoggerContext.getContext();
    ctx.reconfigure();
  }

  @Test
  public void testContentType() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    Assert.assertEquals("application/json; charset=UTF-8", sumoJsonLayout.getContentType());
  }

  @Test
  public void testCharset()
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    Assert.assertEquals(Charsets.UTF_8, sumoJsonLayout.getCharset());
  }

  @Test
  public void testEvent() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final LogEvent logEvent = baseBuilder()
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\"}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testEventWithMDC() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final Map<String, String> contextMap = new HashMap<>();
    contextMap.put("foo", "bar");
    final LogEvent logEvent = baseBuilder()
        .setContextMap(contextMap)
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\",\"contextMap\":{\"foo\":\"bar\"}}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testEventsWithContextStack() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final MutableThreadContextStack mutableThreadContextStack = new MutableThreadContextStack();
    mutableThreadContextStack.add("context stack");
    final LogEvent logEvent = baseBuilder()
        .setContextStack(mutableThreadContextStack)
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\",\"contextStack\":[\"context stack\"]}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testEventsWithEmptyContextMap() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final LogEvent logEvent = baseBuilder()
        .setContextMap(new HashMap<>())
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\"}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testWithLocation() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(true);
    final LogEvent logEvent = baseBuilder()
        .setIncludeLocation(true)
        .setSource(buildSte())
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\",\"source\":{\"class\":\"class\",\"method\":\"method\",\"file\":\"file\",\"line\":10}}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testWithoutLocation() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final LogEvent logEvent = baseBuilder()
        .setIncludeLocation(true)
        .setSource(buildSte())
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\"}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testThrowable() throws Exception
  {
    final SumoJsonLayout sumoJsonLayout = SumoJsonLayout.createLayout(false);
    final Throwable throwable = new RuntimeException("test exception");
    throwable.setStackTrace(new StackTraceElement[]{buildSte()});
    final LogEvent logEvent = baseBuilder()
        .setThrown(throwable)
        .build();
    final String jsonString = "{\"timestamp\":12345,\"thread\":\"some thread\",\"level\":\"ERROR\",\"loggerName\":\"some name\",\"message\":\"some message\",\"thrown\":{\"commonElementCount\":0,\"localizedMessage\":\"test exception\",\"message\":\"test exception\",\"name\":\"java.lang.RuntimeException\",\"extendedStackTrace\":[{\"class\":\"class\",\"method\":\"method\",\"file\":\"file\",\"line\":10,\"exact\":false,\"location\":\"?\",\"version\":\"?\"}]}}\n";
    Assert.assertEquals(jsonString, sumoJsonLayout.toSerializable(logEvent));
  }

  @Test
  public void testSerializeHandlesErrorCorrectly() throws Exception
  {
    final ObjectWriter writer = EasyMock.createStrictMock(ObjectWriter.class);
    final LogEvent event = Log4jLogEvent.newBuilder().build();
    final IOException ioe = new IOException("some exception");
    writer.writeValue(EasyMock.<StringBuilderWriter>anyObject(), EasyMock.eq(event));
    EasyMock.expectLastCall().andThrow(ioe).once();
    EasyMock.replay(writer);
    final SumoJsonLayout sumoJsonLayout = new SumoJsonLayout(writer)
    {
      @Override
      LogEvent wrap(LogEvent event)
      {
        return event;
      }
    };
    Assert.assertEquals(Strings.EMPTY, sumoJsonLayout.toSerializable(event));
    EasyMock.verify(writer);
  }

  @Test
  public void testSumoWrapper()
  {
    final Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    final LogEvent logEvent = Log4jLogEvent
        .newBuilder()
        .setContextMap(map)
        .setSource(buildSte())
        .setThrown(new RuntimeException())
        .setThrownProxy(new ThrowableProxy(new RuntimeException()))
        .setIncludeLocation(true)
        .setContextStack(new MutableThreadContextStack())
        .setTimeMillis(1234)
        .setNanoTime(9876)
        .setEndOfBatch(true)
        .setLevel(Level.ALL)
        .setThreadName("thread name")
        .setLoggerName("logger name")
        .setLoggerFqcn("logger fqcn")
        .setMessage(new SimpleMessage("some message"))
        .setMarker(new MarkerManager.Log4jMarker("marker name"))
        .build();
    final SumoLogEvent sumoLogEvent = new SumoLogEvent(logEvent);
    Assert.assertEquals(logEvent.getMessage(), sumoLogEvent.getMessage());
    Assert.assertEquals(logEvent.getContextMap(), sumoLogEvent.getContextMap());
    Assert.assertEquals(logEvent.getContextStack(), sumoLogEvent.getContextStack());
    Assert.assertEquals(logEvent.getLevel(), sumoLogEvent.getLevel());
    Assert.assertEquals(logEvent.getLoggerFqcn(), sumoLogEvent.getLoggerFqcn());
    Assert.assertEquals(logEvent.getLoggerName(), sumoLogEvent.getLoggerName());
    Assert.assertEquals(logEvent.getMarker(), sumoLogEvent.getMarker());
    Assert.assertEquals(logEvent.getNanoTime(), sumoLogEvent.getNanoTime());
    Assert.assertEquals(logEvent.getSource(), sumoLogEvent.getSource());
    Assert.assertEquals(logEvent.getThreadName(), sumoLogEvent.getThreadName());
    Assert.assertEquals(logEvent.getThrownProxy(), sumoLogEvent.getThrownProxy());
    Assert.assertEquals(logEvent.getTimeMillis(), sumoLogEvent.getTimeMillis());
    Assert.assertEquals(logEvent.isEndOfBatch(), sumoLogEvent.isEndOfBatch());
    Assert.assertEquals(logEvent.isIncludeLocation(), sumoLogEvent.isIncludeLocation());
  }

  static Log4jLogEvent.Builder baseBuilder()
  {
    return Log4jLogEvent
        .newBuilder()
        .setEndOfBatch(true)
        .setLevel(Level.ERROR)
        .setLoggerFqcn("do not include")
        .setMessage(new SimpleMessage("some message"))
        .setLoggerName("some name")
        .setTimeMillis(12345)
        .setThreadName("some thread");
  }

  static StackTraceElement buildSte()
  {
    return new StackTraceElement(
        "class",
        "method",
        "file",
        10
    );
  }
}
