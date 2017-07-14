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
package edu.rice.cs.caper.bayou.application.experiments.low_level_sketches;

import java.util.List;
import java.util.stream.Collectors;

public class DExceptLowLevel extends DASTNodeLowLevel {

    String node = "DExcept";
    public List<DASTNodeLowLevel> _try;
    public List<DASTNodeLowLevel> _catch;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _try.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _catch.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
