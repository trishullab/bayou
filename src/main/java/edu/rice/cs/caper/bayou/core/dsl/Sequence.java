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

import java.util.ArrayList;
import java.util.List;

public class Sequence {
    final List<String> calls;

    public Sequence() {
        this.calls = new ArrayList<>();
    }

    public Sequence(List<String> calls) {
        this.calls = new ArrayList<>(calls);
    }

    public List<String> getCalls() {
        return calls;
    }

    public void addCall(String apiCall) {
        calls.add(apiCall);
    }

    /* check if this is a subsequence of "seq" from index 0 */
    public boolean isSubsequenceOf(Sequence seq) {
        if (calls.size() > seq.calls.size())
            return false;
        for (int i = 0; i < calls.size(); i++)
            if (! calls.get(i).equals(seq.calls.get(i)))
                return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Sequence))
            return false;
        Sequence seq = (Sequence) o;
        return calls.equals(seq.calls);
    }

    @Override
    public int hashCode() {
        int code = 17;
        for (String call : calls)
            code = 31 * code + call.hashCode();
        return code;
    }
}
