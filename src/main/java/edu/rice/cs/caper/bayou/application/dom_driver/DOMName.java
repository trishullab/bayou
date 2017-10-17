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
import org.eclipse.jdt.core.dom.Name;

public class DOMName extends DOMExpression {

    @Expose
    final String node = "DOMName";

    @Expose
    final String _name;

    public DOMName(Name name) {
        this._name = name.getFullyQualifiedName();
    }

    @Override
    public DOMName handleAML() {
        return this;
    }
}
