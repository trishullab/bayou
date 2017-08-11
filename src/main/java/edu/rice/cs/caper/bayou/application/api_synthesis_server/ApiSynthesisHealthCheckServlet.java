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
package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by barnett on 4/7/17.
 */
public class ApiSynthesisHealthCheckServlet extends HttpServlet
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisHealthCheckServlet.class.getName());

    /**
     * How requests should be fulfilled.
     */
    private final ApiSynthesisStrategy _synthesisStrategy = ApiSynthesisStrategy.fromConfig();


    /**
     * Public so Jetty can instantiate.
     */
    public ApiSynthesisHealthCheckServlet()
    {
        _logger.debug("entering");
        _logger.debug("exiting");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        _logger.debug("entering");

        /*
         * Perform a generic API synth call and check that no exceptions are generated and that at least one result
         * is found. If so return HTTP status 200. Otherwise, 500.
         */
        try
        {

            String code = "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                    "\n" +
                    "public class TestIO1 {\n" +
                    "\n" +
                    "    // Read from a file\n" +
                    "    void read(String file) {\n" +
                    "        Evidence.apicalls(\"readLine\");\n" +
                    "    }   \n" +
                    "}";

            Iterable<String> results = _synthesisStrategy.synthesise(code, 1);

            if (!results.iterator().hasNext())
            {
                _logger.error("health check failed due to empty results.");
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }

            resp.getWriter().write("Ok.");

        }
        catch (Throwable e)
        {
            _logger.error("health check failed due to exception", e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }

        _logger.debug("exiting");
    }


}
