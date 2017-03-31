# sumologic-log4j2-appender

[![Build Status](https://travis-ci.org/metamx/sumologic-log4j2-appender.svg?branch=master)](https://travis-ci.org/metamx/sumologic-log4j2-appender)

A Log4J 2 appender that sends straight to Sumo Logic.

Note: For the original Log4j appender, please see https://github.com/SumoLogic/sumo-log4j-appender
# Versioning
The version of this plugin will be of the format `X.Y.Z.a` where `X.Y.Z` is the log4j version it is compatible with,
and `a` is the release version of this library. 
# SumoLogicAppender
The `SumoLogicAppender` is a log4j2 appender that utilizes a SumoLogic HTTP Collector.
## Installation

The library can be added to your project using Maven Central by adding the following dependency to a POM file:

```xml
<dependency>
    <groupId>com.sumologic.plugins.log4j</groupId>
    <artifactId>sumologic-log4j2-appender</artifactId>
    <version>1.1</version>
</dependency>
```

## Usage

### Set up HTTP Hosted Collector Source in Sumo Logic

Follow these instructions for [setting up an HTTP Source](http://help.sumologic.com/Send_Data/Sources/HTTP_Source) in Sumo Logic.

### Log4J XML Configuration
Be sure to replace [collector-url] with the URL after creating an HTTP Hosted Collector Source in Sumo Logic.

`log4j2.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <SumoLogicAppender
                name="SumoAppender"
                url="[collector-url]">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS Z} [%t] %-5p %c - %m%n" />
        </SumoLogicAppender>
    </Appenders>
    <Loggers>
        <Root level="all" additivity="false">
            <AppenderRef ref="SumoAppender" />
        </Root>
    </Loggers>
</Configuration>
```

### Parameters
| Parameter          | Required? | Default Value | Description                                                                                                                                |
|--------------------|----------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| name               | Yes      |               | Name used to register Log4j Appender                                                                                                       |
| url                | Yes      |               | HTTP collection endpoint URL                                                                                                               |
| proxyHost          | No       |               | Proxy host IP address                                                                                                                      |
| proxyPort          | No       |               | Proxy host port number                                                                                                                     |
| proxyAuth          | No       |               | For basic authentication proxy, set to "basic". For NTLM authentication proxy, set to "ntlm". For no authentication proxy, do not specify. |
| proxyUser          | No       |               | Proxy host username for basic and NTLM authentication. For no authentication proxy, do not specify.                                        |
| proxyPassword      | No       |               | Proxy host password for basic and NTLM authentication. For no authentication proxy, do not specify.                                        |
| proxyDomain        | No       |               | Proxy host domain name for NTLM authentication only                                                                                        |
| retryInterval      | No       | 10000         | Retry interval (in ms) if a request fails                                                                                                  |
| connectionTimeout  | No       | 1000          | Timeout (in ms) for connection                                                                                                             |
| socketTimeout      | No       | 60000         | Timeout (in ms) for a socket                                                                                                               |
| messagesPerRequest | No       | 100           | Number of messages needed to be in the queue before flushing                                                                               |
| maxFlushInterval   | No       | 10000         | Maximum interval (in ms) between flushes                                                                                                   |
| sourceName         | No       |               | Source name to appear on Sumo Logic                                                                                                        |
| flushingAccuracy   | No       | 250           | How often (in ms) that the flushing thread checks the message queue                                                                        |
| maxQueueSizeBytes  | No       | 1000000       | Maximum capacity (in bytes) of the message queue                                                                                           |

### NOTE: 
The thread for sending data is a daemon thread. As such, if your connectivity to SumoLogic is disrupted and the
JVM tries to shutdown, it may not be able to send the last bit of data. Users should
ensure Log4j shutdown hooks are registered and handled properly to maximize the possiblity of sending the last data.

# SumoJsonLayout
The `SumoJsonLayout` is a JSON layout with namings and optimizations for sending log events to SumoLogic via a HTTP Collector.
The specific changes it has compared to the standard JSON layout are as follows:

* `timeMillis` is now `timestamp`
* `endOfBatch` is ignord
* `loggerFqcn` is ignored
* Charset is always `UTF-8`
* The output is always compact
* The output always ends with a newline `'\n'`
* The output always includes thread context map (MDC) if any such map exists, otherwise omits it from the log

## Usage
### Log4J XML Configuration
`log4j2.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <SumoLogicAppender
                name="SumoAppender"
                url="[collector-url]">
            <SumoJsonLayout />
        </SumoLogicAppender>
    </Appenders>
    <Loggers>
        <Root level="all" additivity="false">
            <AppenderRef ref="SumoAppender" />
        </Root>
    </Loggers>
</Configuration>
```
### Parameters
| Parameter          | Required? | Default Value | Description                                                                                     |
|--------------------|----------|---------------|--------------------------------------------------------------------------------------------------|
|locationInfo        | no       | false         | See the Log4j [Layout Info](https://logging.apache.org/log4j/2.x/manual/layouts.html#JSONLayout) |

# Development

To compile the plugin:
- Run "mvn clean package" on the pom.xml in the main level of this project.

# License

The Sumo Logic Log4j 2 Appender is published under the Apache Software License, Version 2.0. Please visit http://www.apache.org/licenses/LICENSE-2.0.txt for details.
