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

/**
 * A wrapper for synthesized programs along with some meta data
 */
public class SynthesisResult {

    /**
     * The synthesized program
     */
    private String program;

    /**
     * The number of $ variables (variables needed from environment) in the program
     */
    private int num$Variables;

    SynthesisResult(String program, int num$Variables) {
        this.program = program;
        this.num$Variables = num$Variables;
    }

    public String getProgram() {
        return program;
    }

    public int getNum$Variables() {
        return num$Variables;
    }
}
