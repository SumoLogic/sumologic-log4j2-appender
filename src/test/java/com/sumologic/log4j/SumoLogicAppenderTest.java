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

import com.sumologic.log4j.server.AggregatingHttpHandler;
import com.sumologic.log4j.server.MaterializedHttpRequest;
import com.sumologic.log4j.server.MockHttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static org.junit.Assert.*;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class SumoLogicAppenderTest {

    private static final int PORT = 10010;

    private MockHttpServer server;
    private AggregatingHttpHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new AggregatingHttpHandler();
        server = new MockHttpServer(PORT, handler);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null)
            server.stop();
    }

    @Test
    public void testMessagesWithMetadata() throws Exception {
        // See ./resources/log4j2.xml for definition
        Logger loggerInTest = LogManager.getLogger("TestAppender1");
        int numMessages = 5;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
            Thread.sleep(150);
        }

        assertEquals(numMessages, handler.getExchanges().size());
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            assertEquals(true, request.getBody().contains("info"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Name").equals("mySource"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Category").equals("myCategory"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Host").equals("myHost"));
        }
    }

    @Test
    public void testMessagesWithoutMetadata() throws Exception {
        // See ./resources/log4j2.xml for definition
        Logger loggerInTest = LogManager.getLogger("TestAppender2");
        int numMessages = 5;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
            Thread.sleep(150);
        }

        assertEquals(numMessages, handler.getExchanges().size());
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            assertEquals(true, request.getBody().contains("info"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Name") == null);
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Category") == null);
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Host") == null);
        }
    }
}
