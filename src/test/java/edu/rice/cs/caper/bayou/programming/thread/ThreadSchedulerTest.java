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
