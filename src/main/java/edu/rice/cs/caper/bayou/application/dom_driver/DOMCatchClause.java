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


import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

public class DOMCatchClause extends DOMNode implements Handler {

    final CatchClause clause;

    @Expose
    final String node = "DOMCatchClause";

    @Expose
    final DOMType _type;

    @Expose
    final String _variable; // terminal

    @Expose
    final DOMBlock _body;

    public DOMCatchClause(CatchClause clause) {
        this.clause = clause;
        this._type = new DOMType(clause.getException().getType()).handleAML();
        this._variable = clause.getException().getName().getIdentifier();
        this._body = new DOMBlock(clause.getBody()).handleAML();
    }

    @Override
    public DSubTree handle() {
        return new DOMBlock(clause.getBody()).handle();
    }

    @Override
    public DOMCatchClause handleAML() {
        return this;
    }
}
