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
package edu.rice.cs.caper.bayou.application.dom_driver;

import edu.rice.cs.caper.bayou.core.dsl.Sequence;

import java.util.List;
import java.util.Set;

public class DOMNode implements HandlerAML {
    public class TooManySequencesException extends Exception { }
    public class TooLongSequenceException extends Exception { }

    public class NotImplementedException extends RuntimeException { }

    @Override
    public DOMNode handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        throw new NotImplementedException();
    }

    public Set<String> bagOfAPICalls() {
        throw new NotImplementedException();
    }

    public void updateSequences(List<Sequence> soFar, int max, int max_length)
        throws TooManySequencesException, TooLongSequenceException {
        throw new NotImplementedException();
    }

    public int numStatements() {
        throw new NotImplementedException();
    }

    public int numLoops() {
        throw new NotImplementedException();
    }

    public int numBranches() {
        throw new NotImplementedException();
    }

    public int numExcepts() {
        throw new NotImplementedException();
    }

    public String toAML() { throw new NotImplementedException(); }

    public String toString() { throw new NotImplementedException(); }
}
