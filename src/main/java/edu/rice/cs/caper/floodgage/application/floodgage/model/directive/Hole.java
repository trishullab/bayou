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
package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

import java.util.Collections;
import java.util.List;

public interface Hole
{
    String getId();

    List<Evidence> getEvidence();

    static Hole make(String id, List<Evidence> evidence)
    {
        return new Hole()
        {
            @Override
            public String getId()
            {
                return id;
            }

            @Override
            public List<Evidence> getEvidence()
            {
                return Collections.unmodifiableList(evidence);
            }
        };
    }
}
