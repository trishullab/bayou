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
package edu.rice.cs.caper.bayou.core.bayou_services_client.ap_synthesis;

import edu.rice.cs.caper.bayou.core.bayou_services_client.JsonMsgClientBase;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

/**
 * A client for sending API synthesis JSON requests to a remote server over a socket.
 */
public class ApiSynthesisClient extends JsonMsgClientBase
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisClient.class.getName());

    /**
     * Creates a new client that communicates with a remote synthesis server at the given address and port
     * to fulfil code completion requests.
     *
     * @param host the hostname of the remove server. May not be null.
     * @param port the port on the remote server listening for connection requests. May not be negative or greater than
     *             65535.
     * @throws IllegalArgumentException if host is null.
     * @throws IllegalArgumentException if host is only whitespace.
     * @throws IllegalArgumentException if port is less than 0 or greater than 65535.
     */
    public ApiSynthesisClient(String host, int port)
    {
        super(host, port);
    }

    /**
     * Sends an API synthesis request of the given code to the remote server and returns the solution responses
     * from the server.
     *
     * If any call to read() for the server's response from the underlying socket does not complete within
     * 30 seconds this method will throw a SocketTimeoutException.
     *
     * @param code the code to be examined by the remote server for synthesis. May not be null.
     * @return the result of the synthesis. Never null.
     * @throws IOException if there is a problem communicating with the remote server
     * @throws SynthesisError if a non-communication error occurs during synthesis
     * @throws IllegalArgumentException if code is null
     * @throws SocketTimeoutException if the server does not respond in a timely fashion
     */
    public List<String> synthesise(String code) throws IOException, SynthesisError
    {
        return synthesizeHelp(code, null);
    }

    /**
     * Sends an API synthesis request of the given code to the remote server and returns the solution responses
     * from the server.
     *
     * If any call to read() for the server's response from the underlying socket does not complete within
     * 30 seconds this method will throw a SocketTimeoutException.
     *
     * @param code the code to be examined by the remote server for synthesis. May not be null.
     * @param sampleCount the number of model samples to perform during synthesis
     * @return the result of the synthesis. Never null.
     * @throws IOException if there is a problem communicating with the remote server
     * @throws SynthesisError if a non-communication error occurs during synthesis
     * @throws IllegalArgumentException if code is null
     * @throws SocketTimeoutException if the server does not respond in a timely fashion
     */
    public List<String> synthesise(String code, int sampleCount) throws IOException, SynthesisError
    {
        return synthesizeHelp(code, sampleCount);
    }

    private List<String> synthesizeHelp(String code, Integer sampleCount) throws IOException, SynthesisError
    {
        _logger.debug("entering");

        if(code == null)
            throw new IllegalArgumentException("code may not be null");

        /*
         * Create request and send to server.
         */
        JSONObject requestMsg = new JSONObject();
        requestMsg.put("code", code);
        if(sampleCount != null)
            requestMsg.put("sample count", sampleCount);

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://" + host + ":" + port + "/apisynthesis");
        post.addHeader("Origin", "http://askbayou.com");
        post.setEntity(new ByteArrayEntity(requestMsg.toString(4).getBytes()));

        /*
         * Read and parse the response from the server.
         */
        JSONObject responseBodyObj;
        {
            HttpResponse response =  httpclient.execute(post);
            if(response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 400)
            {
                throw new IOException("Unexpected status code: " + response.getStatusLine().getStatusCode());
            }

            String responseBodyAsString;
            {
                byte[] responseBytes = IOUtils.toByteArray(response.getEntity().getContent());
                responseBodyAsString = new String(responseBytes);
            }

            try
            {
                responseBodyObj = parseResponseMessageBodyToJson(responseBodyAsString);
            }
            catch (IllegalArgumentException e)
            {
                _logger.debug("exiting");
                throw new SynthesisError(e.getMessage());
            }
        }

        _logger.debug("exiting");
        return parseResponseMessageBody(responseBodyObj);
    }

    /**
     * Extracts the ordered code completion results from the server's JSON response and returns them in List form.
     *
     * @param messageBodyObj the server's response
     * @return the value of the JSON results string array with name "results"
     * @throws SynthesisError if the success flag on the given JSON object is false.
     * @throws IllegalArgumentException if the JSON object is not formatted correctly.
     */
    private List<String> parseResponseMessageBody(JSONObject messageBodyObj) throws SynthesisError
    {
        _logger.debug("entering");

        if(messageBodyObj == null)
        {
            _logger.debug("exiting");
            throw new IllegalArgumentException("messageBody may not be null");
        }

        /*
         * Retrieve the value of the success field.
         */
        boolean successValue;
        {
            String SUCCESS = "success";
            if (!messageBodyObj.has(SUCCESS))
            {
                _logger.debug("exiting");
                throw new IllegalArgumentException("Given JSON does not have " + SUCCESS + " member: "
                                                   + messageBodyObj.toString());
            }

            try
            {
                successValue = messageBodyObj.getBoolean(SUCCESS);
            }
            catch (JSONException e)
            {
                _logger.debug("exiting");
                throw new IllegalArgumentException("Given JSON " + SUCCESS + " member is not a boolean: " +
                                                   messageBodyObj.toString());
            }
        }

        /*
         * If server response is an error response, throw exception.
         */
        if(!successValue)
        {
            String errorMessageValue = messageBodyObj.get("errorMessage").toString();
            if(errorMessageValue.equals("parseException"))
            {
                String EXCEPTION_MESSAGE = "exceptionMessage";
                if(messageBodyObj.has(EXCEPTION_MESSAGE))
                {
                    String exceptionMessage = messageBodyObj.get(EXCEPTION_MESSAGE).toString();
                    throw new ParseError(exceptionMessage);
                }

            }

            _logger.debug("exiting");
            throw new SynthesisError(errorMessageValue);
        }

        /*
         * Retrieve the completions from the response and populate them to completions.
         */
        LinkedList<String> completions = new LinkedList<>();
        {
            String resultsKey = "results";
            JSONArray completionsArray;
            {
                try
                {
                    completionsArray = messageBodyObj.getJSONArray(resultsKey);
                }
                catch (JSONException e)
                {
                    _logger.debug("exiting");
                    throw new IllegalArgumentException("Given JSON does not have string array " + resultsKey + "" +
                                                       " member: " + messageBodyObj.toString());
                }
            }

            for (int i = 0; i < completionsArray.length(); i++)
            {
                try
                {
                    completions.add(completionsArray.getString(i));
                }
                catch (JSONException e) // some element of the array not a String
                {
                    _logger.debug("exiting");
                    throw new IllegalArgumentException("Given JSON  has a non-string element in " + resultsKey +
                                                       " member: " + messageBodyObj.toString());
                }

            }
        }

        _logger.debug("exiting");
        return completions;
    }


}
