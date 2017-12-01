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

import java.util.function.*;
import java.util.*;

/**
 * When maven tests are run ensure that the pass program passes the test suite.
 */
public class PassProgramTest extends TestSuite
{
	@Override
	protected Function<List<String>,String> makeTestable()
	{
		return x -> {
		    StringBuffer buff = new StringBuffer();
		    Iterator<String> iter = x.iterator();
		    while (iter.hasNext()) {
		        buff.append(iter.next());
		    }
		    return buff.toString();
		};
	}
}
