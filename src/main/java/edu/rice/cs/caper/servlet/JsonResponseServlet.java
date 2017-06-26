package edu.rice.cs.caper.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by barnett on 6/6/17.
 */
public interface JsonResponseServlet
{
    /**
     * UTF-8 charset.
     */
    Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Appends the string form of the given JSON object to the body of the given HTTP response using the UTF-8
     * charset.
     *
     * @param obj the object to append
     * @param response the http response to which's body the object should be appended
     * @throws IOException if there is an error appending the object.
     */
    default void writeObjectToServletOutputStream(JSONObject obj, HttpServletResponse response) throws IOException
    {
        Logger logger = LogManager.getLogger(JsonResponseServlet.class.getName());
        logger.debug("entering");

        ByteBuffer byteBuffer = UTF_8.encode(obj.toString());
        byte[] responseBodyBytes=new byte[byteBuffer.limit()];
        byteBuffer.get(responseBodyBytes);

        response.getOutputStream().write(responseBodyBytes);

        logger.debug("exiting");
    }
}
