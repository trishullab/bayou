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
package edu.rice.cs.caper.bayou.core.synthesizer;

public class SearchTarget {

    private final Type type;
    private final String name;

    public SearchTarget(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public SearchTarget(Type type) {
        this.type = type;
        this.name = null;
    }

    public boolean hasName() {
        return name != null;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
