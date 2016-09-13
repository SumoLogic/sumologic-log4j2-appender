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
package com.sumologic.log4j.queue;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import static com.sumologic.log4j.queue.CostBoundedConcurrentQueue.CostAssigner;
/**
 * Buffer for one concurrent producer and one concurrent consumer which takes members of
 * the queue in batches.
 *
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/6/13
 * Time: 3:29 PM
 */
public class BufferWithFifoEviction<T> extends BufferWithEviction<T> {
    private static final Logger logger = StatusLogger.getLogger();
    private CostBoundedConcurrentQueue<T> queue;
    private CostAssigner<T> costAssigner;

    public BufferWithFifoEviction(long capacity, CostAssigner<T> costAssigner) {
        super(capacity);

        if (costAssigner == null) {
            throw new IllegalArgumentException("CostAssigner cannot be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        this.queue = new CostBoundedConcurrentQueue<T>(capacity, costAssigner);
        this.costAssigner = costAssigner;
    }

    @Override
    protected T evict() {
        return queue.poll();
    }

    /**
     * Make room for inserting an element with cost <tt>cost</tt>
     * @param cost the desired cost to evict
     * @return true if eviction was successful, false otherwise.
     */
    protected boolean evict(long cost) {

        int numEvicted = 0;


        if (cost > getCapacity()) return false;

        long targetCost = getCapacity() - cost;
        do {
            numEvicted++;
            evict();
        } while (queue.cost() > targetCost);

        if (numEvicted > 0) {
            logger.warn("Evicted " + numEvicted + " messages from buffer");
        }

        return true;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public int drainTo(Collection<T> collection) {
        return queue.drainTo(collection);
    }

    @Override
    synchronized public boolean add(T element) {
        boolean wasSuccessful = queue.offer(element);
        if (! wasSuccessful) {
            evict(costAssigner.cost(element));
            return queue.offer(element);
        }

        return true;
    }
}
