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

        String objAsString = obj.toString(2);
        logger.trace("objAsString=" + objAsString);
        ByteBuffer byteBuffer = UTF_8.encode(objAsString);
        byte[] responseBodyBytes=new byte[byteBuffer.limit()];
        byteBuffer.get(responseBodyBytes);

        response.getOutputStream().write(responseBodyBytes);

        logger.debug("exiting");
    }
}
