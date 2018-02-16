package edu.rice.cs.caper.programming.thread;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ThreadScheduler
{
    <T> T schedule(Supplier<T> operation);
}
