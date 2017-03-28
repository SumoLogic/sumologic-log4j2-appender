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

package com.sumologic.log4j.http;

import java.io.IOException;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class SumoHttpSenderTest
{
  private static final long RETRY_INTERVAL = 100;
  private static final String URL = "url";
  private static final String NAME = "some name";
  private static final String HOST = "some host";
  private static final String CATEGORY = "some category";

  @Test(timeout = 1000L)
  public void testBackoff()
  {
    final CloseableHttpClient httpClient = EasyMock.createNiceMock(CloseableHttpClient.class);
    final SumoHttpSender sender = defaultSender(httpClient);
    final long sleep = sender.exponentialBackoff(0);
    Assert.assertTrue(sleep >= 0);
    while (sleep == sender.exponentialBackoff(0)) {
      ;
    }
    for (int nTry = 0; nTry < 1000; ++nTry) {
      Assert.assertTrue(sender.exponentialBackoff(nTry) >= 0);
    }
  }

  @Test
  public void testInitialized()
  {
    Assert.assertTrue(defaultSender(EasyMock.createStrictMock(CloseableHttpClient.class)).isInitialized());
  }

  @Test
  public void testErrorSendRecovers() throws Exception
  {
    final byte[] body = "foo".getBytes("UTF-8");
    final CloseableHttpClient httpClient = EasyMock.createStrictMock(CloseableHttpClient.class);
    final Capture<HttpUriRequest> requestCapture = EasyMock.newCapture();
    final CloseableHttpResponse response = EasyMock.createStrictMock(CloseableHttpResponse.class);

    EasyMock.expect(httpClient.execute(EasyMock.capture(requestCapture)))
            .andThrow(new IOException("some exception"))
            .once()
            .andReturn(response)
            .once();
    EasyMock.expect(response.getStatusLine())
            .andReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "reason"))
            .once();
    EasyMock.expect(response.getEntity())
            .andReturn(new StringEntity("response string"))
            .once();

    EasyMock.replay(httpClient, response);

    final SumoHttpSender sender = defaultSender(httpClient);
    sender.send(body);

    EasyMock.verify(httpClient, response);

    final HttpUriRequest uriRequest = requestCapture.getValue();
    Assert.assertEquals("POST", uriRequest.getMethod());
    Assert.assertEquals(NAME, uriRequest.getHeaders("X-Sumo-Name")[0].getValue());
    Assert.assertEquals(CATEGORY, uriRequest.getHeaders("X-Sumo-Category")[0].getValue());
    Assert.assertEquals(HOST, uriRequest.getHeaders("X-Sumo-Host")[0].getValue());
  }

  @Test
  public void testErrorSendRecovers503() throws Exception
  {
    final byte[] body = "foo".getBytes("UTF-8");
    final CloseableHttpClient httpClient = EasyMock.createStrictMock(CloseableHttpClient.class);
    final CloseableHttpResponse response = EasyMock.createStrictMock(CloseableHttpResponse.class);
    EasyMock.expect(httpClient.execute(EasyMock.anyObject()))
            .andReturn(response)
            .times(2);
    EasyMock.expect(response.getStatusLine())
            .andReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 503, "reason"))
            .once()
            .andReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "reason"))
            .once();
    EasyMock.expect(response.getEntity()).andReturn(new StringEntity("response string")).once();

    EasyMock.replay(httpClient, response);

    final SumoHttpSender sender = defaultSender(httpClient);
    sender.send(body);

    EasyMock.verify(httpClient, response);
  }

  @Test(timeout = 1000)
  public void testLongSendInterrupted() throws Exception
  {
    final byte[] body = "foo".getBytes("UTF-8");
    final CloseableHttpClient httpClient = EasyMock.createStrictMock(CloseableHttpClient.class);
    final CloseableHttpResponse response = EasyMock.createStrictMock(CloseableHttpResponse.class);

    EasyMock.expect(httpClient.execute(EasyMock.anyObject()))
            .andThrow(new IOException("some exception"))
            .once();

    EasyMock.replay(httpClient, response);

    final SumoHttpSender sender = new SumoHttpSender(Long.MAX_VALUE >> 3, URL, httpClient, NAME, HOST, CATEGORY);
    final Thread thread = new Thread(() -> sender.send(body));
    thread.start();
    Assert.assertTrue(thread.isAlive());
    while (Thread.State.RUNNABLE.equals(thread.getState())) {
      Thread.sleep(1);
    }
    thread.interrupt();
    thread.join();
    Assert.assertFalse(thread.isAlive());
    EasyMock.verify(httpClient, response);
  }

  private static SumoHttpSender defaultSender(CloseableHttpClient httpClient)
  {
    return new SumoHttpSender(RETRY_INTERVAL, URL, httpClient, NAME, HOST, CATEGORY);
  }
}
