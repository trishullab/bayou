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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import java.util.List;
import java.util.UUID;

/**
 * Records the input and solutions to a code synthesis request.
 */
public interface SynthesisLogger
{
    /**
     * Logs a synthesis input and solutions for a given request.
     *
     * @param requestId a unique identifier for the request.
     * @param searchCode the input code of the request.
     * @param results the generated synthesis solutions for the request.
     */
    void log(UUID requestId, String searchCode, Iterable<String> results);
}
