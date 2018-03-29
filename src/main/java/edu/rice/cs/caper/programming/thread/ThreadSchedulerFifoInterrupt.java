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
package edu.rice.cs.caper.programming.thread;

import edu.rice.cs.caper.programming.numbers.NatNum32;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadSchedulerFifoInterrupt implements ThreadScheduler
{
    private final NatNum32 _concurrentLimit;

    private final Object _operationCompletedCondition = new Object();

    private int _inActionOperationsCount = 0;

    private final LinkedList<Thread> _waitingThreads = new LinkedList<>();

    public ThreadSchedulerFifoInterrupt(NatNum32 concurrentLimit)
    {
        _concurrentLimit = concurrentLimit;
    }


    @Override
    public <V,T extends Throwable> V schedule(Operation<V,T> operation) throws T
    {
        synchronized (_operationCompletedCondition)
        {
            if(!_waitingThreads.isEmpty() || _inActionOperationsCount == _concurrentLimit.AsInt)
            {
                _waitingThreads.add(Thread.currentThread());
                awaitTurn();
            }
            _inActionOperationsCount++;
        }

        try
        {
            return operation.apply();
        }
        finally
        {
            synchronized (_operationCompletedCondition)
            {
                _inActionOperationsCount--;
                if(!_waitingThreads.isEmpty())
                {
                    Thread nextToGo = _waitingThreads.remove();
                    nextToGo.interrupt();
                }
            }
        }
    }

    private void awaitTurn()
    {
        try
        {
            _operationCompletedCondition.wait();
        }
        catch (InterruptedException e)
        {
            // do nothing
        }
    }
}
