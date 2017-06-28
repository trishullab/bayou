package edu.rice.cs.caper.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Optional base class for servlets that process POST requests but only tolerate a maximum number of bytes in the
 * request body. If larger requests are sent to the servlet, the servlet will respond with a JSON error message
 * as the body of the response.
 */
public abstract class SizeConstrainedPostBodyServlet extends HttpServlet
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(SizeConstrainedPostBodyServlet.class.getName());

    /**
     * The UTF-8 charset.
     */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * The maximum number of request body body bytes the handler will allow without responding with an error.
     */
    private final int _requestBodyMaxByteCount;

    /**
     * Whether an error response should be generated if the request contains no body.
     */
    private final boolean _allowEmptyBody;

    /**
     * @param requestBodyMaxByteCount The maximum number of request body body bytes the handler will allow without
     *                                responding with an error.
     * @param allowEmptyBody Whether an error response should be generated if the request contains no body.
     */
    public SizeConstrainedPostBodyServlet(int requestBodyMaxByteCount, boolean allowEmptyBody)
    {
        _logger.debug("entering");
        _requestBodyMaxByteCount = requestBodyMaxByteCount;
        _allowEmptyBody = allowEmptyBody;
        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        _logger.debug("entering");

        /*
         * Determine the character set used to encode the request body.
         */
        Charset requestBodyCharset;
        {
            String bodyEncoding = req.getCharacterEncoding();
            if (bodyEncoding == null) // if not specified explicitly by request, assume UTF-8
            {
                bodyEncoding = "UTF-8";
            }

            requestBodyCharset = Charset.forName(bodyEncoding);
        }

        ServletInputStream requestBodyReader;
        try
        {

            requestBodyReader = req.getInputStream();
        }
        catch (IOException e)
        {
            _logger.error(e.getMessage(), e);
            writeErrorResponse(e.getMessage(), resp);
            _logger.debug("exiting");
            return;
        }

        /*
         * Read up to _requestBodyMaxByteCount bytes from request body and store in bodyBytes
         */
        byte[] bodyBytes = new byte[_requestBodyMaxByteCount];
        int totalRequestBytesRead = 0;
        do
        {
            int remainingFreeBytesInBodyBytes = bodyBytes.length - totalRequestBytesRead;

            int numBytesRead;
            try
            {
                numBytesRead = requestBodyReader.read(bodyBytes, totalRequestBytesRead, remainingFreeBytesInBodyBytes);
            }
            catch (IOException e)
            {
                _logger.error(e.getMessage(), e);
                writeErrorResponse(e.getMessage(), resp);
                _logger.debug("exiting");
                return;
            }

            if(numBytesRead == -1)
                break;

            totalRequestBytesRead+=numBytesRead;

        }
        while(totalRequestBytesRead < bodyBytes.length);

        /*
         * Determine if either error case occurs:
         *
         * 1.) More request bytes remain after reading _requestBodyMaxByteCount bytes from request.
         * 2.) The request was empty and empty requests are not allowed.
         *
         * and if so respond with an error body and return.
         */
        boolean bytesRemainOutsideBodyBytes = false;
        try
        {
            bytesRemainOutsideBodyBytes = requestBodyReader.read() != -1;
        }
        catch (IOException e)
        {
            _logger.warn(e.getMessage());
            writeErrorResponse(e.getMessage(), resp);
            _logger.debug("exiting");
        }

        if(totalRequestBytesRead == _requestBodyMaxByteCount && bytesRemainOutsideBodyBytes)
        {
            String errorMessage = "Request body too large. Max " + _requestBodyMaxByteCount + " bytes.";
            _logger.warn(errorMessage);
            writeErrorResponse(errorMessage, resp);
            _logger.debug("exiting");
            return;
        }

        if(totalRequestBytesRead == 0 && !_allowEmptyBody)
        {
            String errorMessage = "Body may not be empty.";
            _logger.warn(errorMessage);
            writeErrorResponse(errorMessage, resp);
            _logger.debug("exiting");
            return;
        }

        /*
         * Encode the request body bytes as a string and process the request.
         */
        String bodyString = new String(bodyBytes, 0, totalRequestBytesRead, requestBodyCharset);
        try
        {
            doPost(req, resp, bodyString);
        }
        catch (Throwable e)
        {
            _logger.error(e.getMessage(), e);
            writeErrorResponse(e.getMessage(), resp);
            _logger.debug("exiting");
            return;
        }
        _logger.debug("exiting");
    }

    private void writeErrorResponse(String errorMessage, HttpServletResponse resp)
    {
        _logger.debug("entering");
        byte[] responseBodyBytes =
                UTF_8.encode(new ErrorJsonResponse(errorMessage).toString()).array();

        resp.setHeader("Content-Type", "application/json");
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        try
        {
            resp.getOutputStream().write(responseBodyBytes);
        }
        catch (IOException e)
        {
           _logger.error(e.getMessage(), e);
        }
        _logger.debug("exiting");
    }

    /**
     * Process a POST request containing the given body.
     *
     * @param req the request
     * @param resp the response
     * @param body the body of the request
     * @throws IOException if an error occurs processing the request
     */
    protected abstract void doPost(HttpServletRequest req, HttpServletResponse resp, String body) throws IOException;
}
