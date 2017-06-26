package edu.rice.cs.caper.bayou.core.bayou_services_client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by barnett on 3/15/17.
 */
public class JsonMsgClientBase
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(JsonMsgClientBase.class.getName());


    /**
     * The hostname of the remote server to which requests will be routed.
     */
    protected final String host;

    /**
     * The port on _host to which requests will be routed.
     */
    protected final int port;

    /**
     * Creates a new client that communicates with a remote completion server at the given address and port
     * to fulfil code completion requests.
     *
     * @param host the hostname of the remove server. May not be null.
     * @param port the port on the remote server listening for connection requests. May not be negative.
     * @throws IllegalArgumentException if host is null.
     * @throws IllegalArgumentException if host is only whitespace.
     * @throws IllegalArgumentException if port is less than 0 or greater than 65535.
     */
    @SuppressWarnings("ConstantConditions") // has a problem throwing an exception after a check for null without
    public JsonMsgClientBase(String host, int port)
    {
        _logger.debug("entering");

        if(host == null)
            throw new IllegalArgumentException("host may not be null");

        if(host.trim().length() == 0)
            throw new IllegalArgumentException("host must not be only whitespace");

        if(port < 0 || port > 65535)
            throw new IllegalArgumentException("port must be in the range 0...65535 inclusive.");

        this.host = host;
        this.port = port;

        _logger.debug("exiting");
    }

    protected void sendRequest(JSONObject msg, OutputStream outStream) throws IOException
    {
        _logger.debug("entering");

        /*
         * Determine the byte content of the request message header and body.
         */
        byte[] requestMessageBodyBytes;
        {
            String jsonString = (msg.toString());
            ByteBuffer jsonStringEncoded = Charset.forName("UTF-8").encode(jsonString);
            requestMessageBodyBytes = new byte[jsonStringEncoded.limit()];
            System.arraycopy(jsonStringEncoded.array(), 0, requestMessageBodyBytes, 0, requestMessageBodyBytes.length);
        }

        byte[] requestMessageHeaderBytes =
                ByteBuffer.allocate(4).putInt(requestMessageBodyBytes.length).array();

        /*
         * Write the request message to the outStream.
         */
        outStream.write(requestMessageHeaderBytes);
        outStream.write(requestMessageBodyBytes);
        outStream.flush();

        _logger.debug("exiting");
    }

    protected JSONObject parseResponseMessageBodyToJson(String messageBody)
    {
        _logger.debug("entering parseResponseMessageBodyToJson");

        if(messageBody == null)
            throw new IllegalArgumentException("messageBody may not be null");

        /*
         * Parse messageBody into a JSON object.
         */
        JSONObject messageBodyObj;
        try
        {
            messageBodyObj = new JSONObject(messageBody);
        }
        catch (JSONException e) // hide from public callers on the stack we are using a specific JSON lib.
        {
            _logger.debug("exiting parseResponseMessageBodyToJson");
            throw new IllegalArgumentException(messageBody + " is not a valid JSON string", e);
        }

        /*
         * If server response is an error response, throw exception.
         */
        String successKey = "success";
        boolean success;
        try
        {
            success = messageBodyObj.getBoolean(successKey);
        }
        catch (JSONException e)
        {
            _logger.debug("exiting parseResponseMessageBodyToJson");
            throw new IllegalArgumentException("Given JSON "  + messageBody + " does not have boolean " +
                    successKey + " member.");
        }

        if(!success)
        {
            String errorMessageKey = "errorMessage";
            try
            {
                messageBodyObj.getString(errorMessageKey);
            }
            catch (JSONException e)
            {
                _logger.debug("exiting parseResponseMessageBodyToJson");
                throw new IllegalArgumentException("Given JSON " + messageBody + " does not have string " +
                        errorMessageKey + " member.");
            }
        }

        _logger.debug("exiting parseResponseMessageBodyToJson");
        return messageBodyObj;
    }

    /**
     * Reads a response message from the given InputStream.  Expected format as defined in
     * edu.rice.pliny.programs.code_completion_server.CodeCompletionServer class contract.
     *
     * @param in the stream from which to read the response message
     * @return the response message body (without header) encoded in the UTF-8 charset.
     * @throws IOException if there is a problem reading from in.
     * @throws IllegalArgumentException if in is null.
     */
    // n.b. static to facilitate testing without construction
    protected static String readResponseAsString(InputStream in) throws IOException
    {
        _logger.debug("entering readResponseAsString");

        if(in == null)
            throw new IllegalArgumentException("in may not be null");

        /*
         * Read response header.
         */
        int numBytesInMessageBody;
        {
            byte[] messageHeaderBytes = new byte[4];
            fillBuffer(messageHeaderBytes, in);
            numBytesInMessageBody = ByteBuffer.wrap(messageHeaderBytes).getInt();
        }

        /*
         * Read response body.
         */
        byte[] responseBodyBytes = new byte[numBytesInMessageBody];
        fillBuffer(responseBodyBytes, in);

        _logger.debug("exiting readResponseAsString");
        return new String(responseBodyBytes, "UTF-8"); // UTF-8 by response message format contract
    }

    /**
     * Copies the next buffer.length bytes from in to buffer starting at buffer[0].
     *
     * @param buffer the buffer to fill
     * @param in the source of bytes
     * @throws IOException if the end of in is read before buffer is filled.
     * @throws IOException if there is a problem reading from in.
     */
    // n.b. static to facilitate testing without construction
    static void fillBuffer(byte[] buffer, InputStream in) throws IOException
    {
        _logger.debug("entering fillBuffer");

        for(int i = 0; i<buffer.length; i++)
        {
            int nextByte = in.read();
            if(nextByte == -1)
                throw new IOException("Early end of stream detected.");

            buffer[i] = (byte)nextByte;
        }

        _logger.debug("exiting fillBuffer");
    }
}
