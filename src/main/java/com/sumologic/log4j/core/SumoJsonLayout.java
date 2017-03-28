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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Strings;

/**
 * Borrowed heavily from org.apache.logging.log4j.core.layout.JsonLayout
 */
@Plugin(name = "SumoJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class SumoJsonLayout extends AbstractStringLayout
{
  private static final String CONTENT_TYPE = "application/json; charset=" + StandardCharsets.UTF_8.displayName();

  private final ObjectWriter objectWriter;

  @PluginFactory
  public static SumoJsonLayout createLayout(
      @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo
  )
  {
    final SimpleFilterProvider filters = new SimpleFilterProvider();
    final Set<String> except = new HashSet<>();
    if (!locationInfo) {
      except.add(JsonConstants.ELT_SOURCE);
    }
    except.add("loggerFqcn");
    except.add("endOfBatch");
    except.add(JsonConstants.ELT_NANO_TIME);
    filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
    final ObjectWriter writer = new Log4jJsonObjectMapper().writer(new MinimalPrettyPrinter());
    return new SumoJsonLayout(writer.with(filters));
  }

  SumoJsonLayout(ObjectWriter objectWriter)
  {
    super(StandardCharsets.UTF_8, null, null);
    this.objectWriter = objectWriter;
  }

  /**
   * Formats a {@link org.apache.logging.log4j.core.LogEvent}.
   *
   * @param event The LogEvent.
   *
   * @return The JSON representation of the LogEvent.
   */
  @Override
  public String toSerializable(final LogEvent event)
  {
    final StringBuilderWriter writer = new StringBuilderWriter();
    try {
      objectWriter.writeValue(writer, wrap(event));
      writer.write('\n');
      return writer.toString();
    }
    catch (final IOException e) {
      LOGGER.error(e);
      return Strings.EMPTY;
    }
  }

  // Overridden in tests
  LogEvent wrap(LogEvent event)
  {
    return new SumoLogEvent(event);
  }

  @Override
  public String getContentType()
  {
    return CONTENT_TYPE;
  }
}

// Jackson and SumoLogic friendly class
class SumoLogEvent implements LogEvent
{
  private final LogEvent delegate;

  SumoLogEvent(LogEvent delegate)
  {
    this.delegate = delegate;
  }

  @Override
  @JsonIgnore
  public Map<String, String> getContextMap()
  {
    return delegate.getContextMap();
  }

  @JsonProperty("contextMap")
  public Map<String, String> getNullableContextMap()
  {
    final Map<String, String> map = getContextMap();
    return map.isEmpty() ? null : map;
  }

  @Override
  public ThreadContext.ContextStack getContextStack()
  {
    return delegate.getContextStack();
  }

  @Override
  public String getLoggerFqcn()
  {
    return delegate.getLoggerFqcn();
  }

  @Override
  public Level getLevel()
  {
    return delegate.getLevel();
  }

  @Override
  public String getLoggerName()
  {
    return delegate.getLoggerName();
  }

  @Override
  public Marker getMarker()
  {
    return delegate.getMarker();
  }

  @Override
  public Message getMessage()
  {
    return delegate.getMessage();
  }

  @Override
  // See https://help.sumologic.com/Send_Data/Sources/04Reference_Information_for_Sources/Timestamps,_Time_Zones,_Time_Ranges,_and_Date_Formats#section_7
  @JsonProperty("timestamp")
  public long getTimeMillis()
  {
    return delegate.getTimeMillis();
  }

  @Override
  public StackTraceElement getSource()
  {
    return delegate.getSource();
  }

  @Override
  public String getThreadName()
  {
    return delegate.getThreadName();
  }

  @Override
  public Throwable getThrown()
  {
    return delegate.getThrown();
  }

  @Override
  public ThrowableProxy getThrownProxy()
  {
    return delegate.getThrownProxy();
  }

  @Override
  public boolean isEndOfBatch()
  {
    return delegate.isEndOfBatch();
  }

  @Override
  public boolean isIncludeLocation()
  {
    return delegate.isIncludeLocation();
  }

  @Override
  public void setEndOfBatch(boolean endOfBatch)
  {
    delegate.setEndOfBatch(endOfBatch);
  }

  @Override
  public void setIncludeLocation(boolean locationRequired)
  {
    delegate.setIncludeLocation(locationRequired);
  }

  @Override
  public long getNanoTime()
  {
    return delegate.getNanoTime();
  }
}
