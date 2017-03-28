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

import java.util.Collection;

/**
 * A concurrent buffer with a maximum capacity that, upon reaching said capacity, evicts some
 * element in the queue to ensure the new element can fit.
 *
 * @author Jose Muniz (jose@sumologic.com)
 *         Date: 4/5/13
 *         Time: 1:51 AM
 */
public abstract class BufferWithEviction<Q>
{

  private final long capacity;

  public BufferWithEviction(long capacity)
  {
    this.capacity = capacity;
  }

  public long getCapacity()
  {
    return capacity;
  }


  protected abstract Q evict();

  protected abstract boolean evict(long cost);

  public abstract int size();

  public abstract int drainTo(Collection<Q> collection, int max);

  public abstract boolean add(Q element);
}
