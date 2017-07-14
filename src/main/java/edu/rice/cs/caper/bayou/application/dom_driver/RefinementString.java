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

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StringLiteral;

import java.util.regex.Pattern;

public class RefinementString extends Refinement {
    boolean exists;
    int length;
    boolean containsPunct;

    public RefinementString(Expression e) {
        if (knownConstants(e))
            return;

        if ( !(e instanceof StringLiteral)) {
            this.exists = false;
            this.length = 0;
            this.containsPunct = false;
            return;
        }

        String s = ((StringLiteral) e).getLiteralValue();
        this.exists = true;
        this.length = s.length();
        this.containsPunct = hasPunct(s);
    }

    private boolean knownConstants(Expression e) {
        if (! (e instanceof Name))
            return false;
        String s = ((Name) e).getFullyQualifiedName();
        if (Visitor.V().options.KNOWN_CONSTANTS_STRING.containsKey(s)) {
            String v = Visitor.V().options.KNOWN_CONSTANTS_STRING.get(s);
            this.exists = true;
            this.length = v.length();
            this.containsPunct = hasPunct(v);

            return true;
        }
        return false;
    }

    private boolean hasPunct(String s) {
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        return p.matcher(s).find();
    }
}
