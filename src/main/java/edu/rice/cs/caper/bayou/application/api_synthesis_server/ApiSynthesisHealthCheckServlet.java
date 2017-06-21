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

            String code = "import edu.rice.bayou.annotations.Evidence;\n" +
                    "\n" +
                    "public class TestIO {\n" +
                    "\n" +
                    "    @Evidence(keywords = \"read buffered line from the file\")\n" +
                    "    void __bayou_fill(String file) {\n" +
                    "    }\n" +
                    "\n" +
                    "}";

            Iterable<String> results = _synthesisStrategy.synthesise(code);

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
