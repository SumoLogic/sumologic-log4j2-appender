[![Build Status](https://api.travis-ci.org/SumoLogic/sumologic-log4j2-appender.svg?branch=master)](https://travis-ci.org/SumoLogic/sumologic-log4j2-appender)
[![codecov.io](https://codecov.io/github/SumoLogic/sumologic-log4j2-appender/coverage.svg?branch=master)](https://codecov.io/github/SumoLogic/sumologic-log4j2-appender?branch=master)

# sumologic-log4j2-appender

A Log4j2 appender that sends straight to Sumo Logic.

For the Logback appender, please see https://github.com/SumoLogic/sumologic-logback-appender

## Installation

The library can be added to your project using Maven Central by adding the following dependency to a POM file:

```
<dependency>
    <groupId>com.sumologic.plugins.log4j</groupId>
    <artifactId>sumologic-log4j2-appender</artifactId>
    <version>2.0.1</version>
</dependency>
```

## Usage

### Set up HTTP Hosted Collector Source in Sumo Logic

Follow these instructions for [setting up an HTTP Source](https://help.sumologic.com/docs/send-data/hosted-collectors/http-source/) in Sumo Logic.

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

**Note:** We recommending starting your `PatternLayout` pattern with a date and time such as `%d{yyyy-MM-dd HH:mm:ss,SSS Z}` for two reasons:

1. Having a consistent prefix that starts every message is necessary for multiline boundary detection to learn the message prefix needed to group mutiline messages, such as stack traces.
2. Sumo only supports [certain time formats](https://help.sumologic.com/03Send-Data/Sources/04Reference-Information-for-Sources/Timestamps%2C-Time-Zones%2C-Time-Ranges%2C-and-Date-Formats), and accidentally using an invalid time format could cause [message time discrepancies](https://help.sumologic.com/03Send-Data/Collector-FAQs/Troubleshooting-time-discrepancies).

### Parameters
| Parameter             | Required? | Default Value | Description                                                                                                                                |
|-----------------------|----------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| name                  | Yes      |               | Name used to register Log4j Appender                                                                                                       |
| url                   | Yes      |               | HTTP collection endpoint URL                                                                                                               |
| sourceName            | No       | "Http Input"              | Source name to appear when searching on Sumo Logic by `_sourceName`                                                                                                        |
| sourceHost            | No       | Client IP Address              | Source host to appear when searching on Sumo Logic by `_sourceHost`                                                                                                         |
| sourceCategory        | No       | "Http Input"              | Source category to appear when searching on Sumo Logic by `_sourceCategory`                                                                                                         |
| proxyHost             | No       |               | Proxy host IP address                                                                                                                      |
| proxyPort             | No       |               | Proxy host port number                                                                                                                     |
| proxyAuth             | No       |               | For basic authentication proxy, set to "basic". For NTLM authentication proxy, set to "ntlm". For no authentication proxy, do not specify. |
| proxyUser             | No       |               | Proxy host username for basic and NTLM authentication. For no authentication proxy, do not specify.                                        |
| proxyPassword         | No       |               | Proxy host password for basic and NTLM authentication. For no authentication proxy, do not specify.                                        |
| proxyDomain           | No       |               | Proxy host domain name for NTLM authentication only                                                                                        |
| retryInterval         | No       | 10000         | Retry interval (in ms) if a request fails                                                                                                  |
| connectionTimeout     | No       | 1000          | Timeout (in ms) for connection                                                                                                             |
| socketTimeout         | No       | 60000         | Timeout (in ms) for a socket                                                                                                               |
| messagesPerRequest    | No       | 100           | Number of messages needed to be in the queue before flushing                                                                               |
| maxFlushInterval      | No       | 10000         | Maximum interval (in ms) between flushes                                                                                                   |
| flushingAccuracy      | No       | 250           | How often (in ms) that the flushing thread checks the message queue                                                                        |
| maxQueueSizeBytes     | No       | 1000000       | Maximum capacity (in bytes) of the message queue
| flushAllBeforeStopping| No       | false         | Flush all messages before stopping regardless of flushingAccuracy
| retryableHttpCodeRegex| No       | ^5.*         | Regular expression specifying which HTTP error code(s) should be retried during sending. By default, all 5xx error codes will be retried.

#### Example with Optional Parameters
`log4j2.xml`:

```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <SumoLogicAppender
                name="SumoAppender"
                url="[collector-url]"
                flushAllBeforeStopping="true"
                sourceHost="Appender-$${env:HOSTNAME}"
                sourceCategory="${sys:appName}"
                proxyHost="1.2.3.4"
                proxyPort="3128">
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
### TLS 1.2 Requirement

Sumo Logic only accepts connections from clients using TLS version 1.2 or greater. To utilize the content of this repo, ensure that it's running in an execution environment that is configured to use TLS 1.2 or greater.

## Development

To compile the plugin:
- Run "mvn clean package" on the pom.xml in the main level of this project.
- To test running a locally built JAR file, you may need to manually add the following dependencies to your project:
```
    <dependencies>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.19.0</version>
        </dependency>

        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.13</version>
        </dependency>
    </dependencies>
```

## License

The Sumo Logic Log4j 2 Appender is published under the Apache Software License, Version 2.0. Please visit http://www.apache.org/licenses/LICENSE-2.0.txt for details.
