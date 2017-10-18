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

/**
 * This is the parent class for all source-level features
 */
abstract public class SourceFeature {
    public String feature_name;


    @Override
    public boolean equals(Object other) {
        if(!(other instanceof SourceFeature)) return false;
        SourceFeature f = (SourceFeature) other;

        return f.feature_name.equals(this.feature_name);
    }
}
