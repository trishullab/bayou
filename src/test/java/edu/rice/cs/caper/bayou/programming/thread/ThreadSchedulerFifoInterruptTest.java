package edu.rice.cs.caper.bayou.programming.thread;

import edu.rice.cs.caper.programming.numbers.NatNum32;
import edu.rice.cs.caper.programming.thread.ThreadScheduler;
import edu.rice.cs.caper.programming.thread.ThreadSchedulerFifoInterrupt;

public class ThreadSchedulerFifoInterruptTest extends ThreadSchedulerTest
{
    @Override
    protected ThreadScheduler makeScheduler()
    {
        return new ThreadSchedulerFifoInterrupt(new NatNum32(2));
    }
}
