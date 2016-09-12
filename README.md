# sumologic-log4j2-appender

A Log4J 2 appender that sends straight to Sumo Logic.

Note: For the original Log4j appender, please see https://github.com/SumoLogic/sumo-log4j-appender

## Installation

The library can be added to your project using Maven Central by adding the following dependency to a POM file:

```
<dependency>
    <groupId>com.sumologic.plugins.log4j</groupId>
    <artifactId>TODO FILL ME IN</artifactId>
    <version>TODO</version>
</dependency>
```

## Usage

Here is a sample Log4j XML configuration file. Make sure to replace [collector-url] with the URL after creating an HTTP Hosted Collector Source in the Sumo Logic web application.

```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <SumoLogicAppender
                name="SumoAppender"
                url="[collector-url]">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS ZZZZ} [%t] %-5p %c - %m%n" />
        </SumoLogicAppender>
    </Appenders>
    <Loggers>
        <Root level="all" additivity="false">
            <AppenderRef ref="SumoAppender" />
        </Root>
    </Loggers>
</Configuration>
```

## Development

To compile the plugin:
- Run "mvn clean package" on the pom.xml in the main level of this project.

## License

The Sumo Logic Log4j 2 Appender is published under the Apache Software License, Version 2.0. Please visit http://www.apache.org/licenses/LICENSE-2.0.txt for details.
