# sumologic-log4j2-appender

A Log4J 2 appender that sends straight to Sumo Logic.

Note: For the original Log4j appender, please see https://github.com/SumoLogic/sumo-log4j-appender

## Installation

The library can be added to your project using Maven Central by adding the following dependency to a POM file:

```
<dependency>
    <groupId>com.sumologic.plugins.log4j</groupId>
    <artifactId>sumologic-log4j2-appender</artifactId>
    <version>1.2</version>
</dependency>
```

## Usage

### Set up HTTP Hosted Collector Source in Sumo Logic

Follow these instructions for [setting up an HTTP Source](http://help.sumologic.com/Send_Data/Sources/HTTP_Source) in Sumo Logic.

### Log4J XML Configuration
Be sure to replace [collector-url] with the URL after creating an HTTP Hosted Collector Source in Sumo Logic.

`log4j2.xml`:

```
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
| sourceName         | No       | "Http Input"              | Source name to appear when searching on Sumo Logic by `_sourceName`                                                                                                        |
| sourceHost         | No       | Client IP Address              | Source host to appear when searching on Sumo Logic by `_sourceHost`                                                                                                         |
| sourceCategory         | No       | "Http Input"              | Source category to appear when searching on Sumo Logic by `_sourceCategory`                                                                                                         |
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
| flushingAccuracy   | No       | 250           | How often (in ms) that the flushing thread checks the message queue                                                                        |
| maxQueueSizeBytes  | No       | 1000000       | Maximum capacity (in bytes) of the message queue                                                                                           |

## Development

To compile the plugin:
- Run "mvn clean package" on the pom.xml in the main level of this project.
- To test running a locally built JAR file, you may need to manually add the following dependencies to your project:
```
    <dependencies>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.8.2</version>
        </dependency>

        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.2</version>
        </dependency>
    </dependencies>
```

## License

The Sumo Logic Log4j 2 Appender is published under the Apache Software License, Version 2.0. Please visit http://www.apache.org/licenses/LICENSE-2.0.txt for details.
