package edu.rice.cs.caper.bayou.programming.thread;

import edu.rice.cs.caper.programming.numbers.NatNum32;
import edu.rice.cs.caper.programming.thread.ThreadScheduler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.Assert;
import org.junit.Test;

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

        List<Integer> queue = new Vector<>();

        scheduler.schedule(() -> {
            queue.add(1);
            return null;
        });

        scheduler.schedule(() -> {
            queue.add(2);
            return null;
        });

        scheduler.schedule(() -> {
            queue.add(3);
            return null;
        });

        Assert.assertEquals(new Integer(1), queue.get(0));
        Assert.assertEquals(new Integer(2), queue.get(1));
        Assert.assertEquals(new Integer(3), queue.get(2));
    }

    protected abstract ThreadScheduler makeScheduler();
}
