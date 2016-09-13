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

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A queue with a maximum capacity, where capacity is defined as the sum of the lengths of the
 * strings it contains.  It implements a strict subset of the functionality of interface
 * <tt>java.util.Queue</tt>
 *
 * @author Jose Muniz (jose@sumologic.com)
 */
public class CostBoundedConcurrentQueue<T> {

    public static interface CostAssigner<T> {
        public long cost(T e);
    }

    private LinkedBlockingQueue<T> queue;
    private CostAssigner<T> costAssigner;

    private long capacity = 0;
    private AtomicLong cost = new AtomicLong(0);


    public CostBoundedConcurrentQueue(long capacity, CostAssigner<T> costAssigner) {
        this.queue = new LinkedBlockingQueue<T>();
        this.costAssigner = costAssigner;
        this.capacity = capacity;
    }


    /**
     * Return the sum of the costs of all the elements contained in the queue.
     * @return the cost
     */
    public long cost() {
        return cost.get();
    }

    /**
     * Return the number of elements in the queue.
     * @return the count
     */
    public int size() {
        return queue.size();
    }


    /**
     * Removes all available elements from this queue and adds them to the given collection.
     *
     * @param collection Destination collection
     * @return the number of elements transferred
     */
    public int drainTo(Collection<T> collection) {

        assert collection.isEmpty();

        int elementsDrained = queue.drainTo(collection);
        for (T e: collection) {
            cost.addAndGet(- costAssigner.cost(e));
        }

        return elementsDrained;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without
     * violating capacity restrictions, returning true upon success and false if no space is
     * currently available.
     *
     * @param e Element to insert
     * @return true if element was successfully inserted;
     *         false is no space is currently available.
     */
    public boolean offer(T e) {
        long eCost = costAssigner.cost(e);

        // Atomically check capacity and optimistically increase usage
        synchronized (this) {
            if (eCost + cost.get() > capacity) {
                return false;
            } else {
                cost.addAndGet(eCost);
            }
        }

        // Underlying queue is unbounded, so this is guaranteed to succeed.
        return queue.add(e);
    }

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     * @return The head of this queue
     */
    public T poll() {
        T e = queue.poll();
        if (e != null)
            cost.addAndGet(-costAssigner.cost(e));

        return e;
    }


}
