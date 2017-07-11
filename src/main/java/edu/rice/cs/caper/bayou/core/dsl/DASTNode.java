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
package edu.rice.cs.caper.bayou.core.dsl;


import edu.rice.cs.caper.bayou.core.synthesizer.Synthesizable;

import java.util.List;
import java.util.Set;

public abstract class DASTNode implements Synthesizable {
    public class TooManySequencesException extends Exception { }
    public class TooLongSequenceException extends Exception { }
    public abstract void updateSequences(List<Sequence> soFar, int max, int max_length) throws TooManySequencesException, TooLongSequenceException;

    public abstract int numStatements();
    public abstract int numLoops();
    public abstract int numBranches();
    public abstract int numExcepts();

    public abstract Set<DAPICall> bagOfAPICalls();

    public abstract Set<Class> exceptionsThrown();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
