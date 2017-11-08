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
package edu.rice.cs.caper.bayou.core.dom_driver;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;

public class RefinementNumber extends Refinement {
    boolean exists;
    float value;

    public RefinementNumber(Expression e) {
        if (knownConstants(e))
            return;

        if (! (e instanceof NumberLiteral)) {
            this.exists = false;
            this.value = 0;
            return;
        }

        this.exists = true;
        this.value = Float.parseFloat(((NumberLiteral) e).getToken());
    }

    private boolean knownConstants(Expression e) {
        if (! (e instanceof Name))
            return false;
        String s = ((Name) e).getFullyQualifiedName();
        if (Visitor.V().options.KNOWN_CONSTANTS_NUMBER.containsKey(s)) {
            this.exists = true;
            this.value = Visitor.V().options.KNOWN_CONSTANTS_NUMBER.get(s);
            return true;
        }
        return false;
    }
}
