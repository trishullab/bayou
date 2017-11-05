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

import edu.rice.cs.caper.bayou.annotations.Evidence;
import java.util.HashMap;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestUtil {

    // NOTE: Bayou only supports one synthesis task in a given
    // program at a time, so please comment out the rest.

    /* Store a key value pair in a map */
    void store(HashMap<Integer,String> map, Integer key, String value) {
        { // Provide evidence within a separate block
            // Code should call "put"
            Evidence.apicalls("put");
        } // Synthesized code will replace this block
    }   
}
