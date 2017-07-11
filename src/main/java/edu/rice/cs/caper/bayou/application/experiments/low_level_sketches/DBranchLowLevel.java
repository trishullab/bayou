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

public class DBranchLowLevel extends DASTNodeLowLevel {

    String node = "DBranch";
    List<DAPICallLowLevel> _cond;
    List<DASTNodeLowLevel> _then;
    List<DASTNodeLowLevel> _else;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _cond.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _then.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _else.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
