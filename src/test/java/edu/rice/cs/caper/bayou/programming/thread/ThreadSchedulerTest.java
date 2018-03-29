/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.programming.thread;

import edu.rice.cs.caper.programming.numbers.NatNum32;
import edu.rice.cs.caper.programming.thread.ThreadScheduler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

public abstract class ThreadSchedulerTest
{
    @Test
    public void scheduleTestSerial()
    {
        ThreadScheduler scheduler = makeScheduler();

        List<Integer> numbers = new Vector<>();

        scheduler.schedule(() -> {
            numbers.add(1);
            return null;
        });

        scheduler.schedule(() -> {
            numbers.add(2);
            return null;
        });

        scheduler.schedule(() -> {
            numbers.add(3);
            return null;
        });

        Assert.assertEquals(new Integer(1), numbers.get(0));
        Assert.assertEquals(new Integer(2), numbers.get(1));
        Assert.assertEquals(new Integer(3), numbers.get(2));
    }

    @Test
    public void scheduleTest() throws InterruptedException
    {
        ThreadScheduler scheduler = makeScheduler();

        List<Integer> numbers = new Vector<>();

        Thread t1 = launchThread(() -> scheduler.schedule(() -> {
            numbers.add(1);
            return null;
        }));

        Thread t2 = launchThread(() -> scheduler.schedule(() -> {
            numbers.add(2);
            return null;
        }));

        Thread t3= launchThread(() -> scheduler.schedule(() -> {
            numbers.add(3);
            return null;
        }));

        t1.join();
        t2.join();
        t3.join();

        Assert.assertTrue(numbers.containsAll(Arrays.asList(1, 2, 3)));
    }

    private Thread launchThread(Runnable runnable)
    {
        Thread t =  new Thread(runnable);
        t.start();
        return t;
    }


    protected abstract ThreadScheduler makeScheduler();
}
