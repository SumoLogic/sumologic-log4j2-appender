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
package com.sumologic.log4j.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class BufferWithFifoEvictionTest
{

  private BufferWithFifoEviction<Integer> queue;

  private CostBoundedConcurrentQueue.CostAssigner<Integer> countCost;
  private CostBoundedConcurrentQueue.CostAssigner<Integer> valueCost;


  @Before
  public void setUp()
  {
    countCost =
        new CostBoundedConcurrentQueue.CostAssigner<Integer>()
        {
          @Override
          public long cost(Integer e)
          {
            return 1;
          }
        };

    valueCost =
        new CostBoundedConcurrentQueue.CostAssigner<Integer>()
        {
          @Override
          public long cost(Integer e)
          {
            return e;
          }
        };
  }

  @Test
  public void testEnforceBottomless()
  {
    queue = new BufferWithFifoEviction<Integer>(2, countCost);
    queue.add(1);
    queue.add(2);
    assertEquals(2, queue.size());

    for (int i = 0; i < 100; i++) {
      queue.add(3);
    }

    assertEquals(2, queue.size());
  }

  @Test
  public void testDrainTo()
  {
    queue = new BufferWithFifoEviction<Integer>(10, countCost);
    for (int i = 0; i < queue.getCapacity(); i++) {
      queue.add(i);
    }

    List<Integer> result = new ArrayList<Integer>(10);
    queue.drainTo(result, 10);

    assertEquals(10, result.size());

    for (int i = 0; i < queue.size(); i++) {
      assertEquals((Object) i, result.get(i));
    }

  }

  @Test
  public void testEviction()
  {
    queue = new BufferWithFifoEviction<Integer>(3, countCost);
    for (int i = 1; i <= 5; i++) {
      queue.add(i);

    }

    List<Integer> result = new ArrayList<Integer>(3);
    queue.drainTo(result, 3);

    assertEquals(3, result.size());
    assertEquals(Arrays.asList(3, 4, 5), result);

  }

  @Test
  public void testInsertLarge()
  {
    queue = new BufferWithFifoEviction<Integer>(1 + 2 + 3 + 4 + 5, valueCost);
    for (int i = 1; i <= 5; i++) {
      queue.add(i);
    }

    assertFalse(queue.add(1000));

    List<Integer> result = new ArrayList<Integer>(3);
    queue.drainTo(result, 5);

    assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

  }

  @Test
  public void testComplexEviction()
  {
    queue = new BufferWithFifoEviction<Integer>(1 + 2 + 3 + 4 + 5, valueCost);
    for (int i = 1; i <= 5; i++) {
      queue.add(i);
    }

    assertFalse(queue.add(100));
    assertTrue(queue.add(6));

    List<Integer> result = new ArrayList<Integer>(3);
    queue.drainTo(result, 3);

    assertEquals(3, result.size());
    assertEquals(Arrays.asList(4, 5, 6), result);

  }


}
