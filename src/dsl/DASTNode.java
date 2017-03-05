package dsl;

import synthesizer.Synthesizable;

import java.util.List;
import java.util.Set;

public abstract class DASTNode implements Synthesizable {
    public class TooManySequencesException extends Exception { }
    public abstract void updateSequences(List<Sequence> soFar, int max) throws TooManySequencesException;

    public abstract int numStatements();
    public abstract int numLoops();
    public abstract int numBranches();
    public abstract int numExcepts();

    public abstract Set<DAPICall> bagOfAPICalls();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
