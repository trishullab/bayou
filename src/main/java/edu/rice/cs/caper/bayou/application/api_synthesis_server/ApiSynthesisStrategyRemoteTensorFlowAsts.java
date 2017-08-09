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

import edu.rice.cs.caper.bayou.core.synthesizer.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A synthesis strategy that relies on a remote server that uses TensorFlow to create ASTs from extracted evidence.
 */
class ApiSynthesisStrategyRemoteTensorFlowAsts extends ExtractEvidenceGenAstsSynthesizeTemplate
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger =
            LogManager.getLogger(ApiSynthesisStrategyRemoteTensorFlowAsts.class.getName());

    /**
     * The network name of the tensor flow host server.
     */
    private final String _tensorFlowHost;

    /**
     * The port the tensor flow host on which connections requests are expected.
     */
    private final int _tensorFlowPort;

    /**
     * The maximum amount of time to wait on a response from the tensor flow server on each request.
     */
    private final int _maxNetworkWaitTimeMs;


    /**
     * @param tensorFlowHost  The network name of the tensor flow host server. May not be null.
     * @param tensorFlowPort The port the tensor flow host server on which connections requests are expected.
     *                       May not be negative.
     * @param maxNetworkWaitTimeMs The maximum amount of time to wait on a response from the tensor flow server on each
     *                             request. 0 means forever. May not be negative.

     */
    // TODO: finish contract
    ApiSynthesisStrategyRemoteTensorFlowAsts(EvidenceExtractor evidenceExtractor, Synthesizer synthesizer,
                                             String tensorFlowHost, int tensorFlowPort, int maxNetworkWaitTimeMs)
    {
        super(evidenceExtractor, synthesizer);

        _logger.debug("entering");

        if(tensorFlowHost == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("tensorFlowHost");
        }

        if(tensorFlowHost.trim().equals(""))
        {
            _logger.debug("exiting");
            throw new IllegalArgumentException("tensorFlowHost must be nonempty and contain non-whitespace characters");
        }

        if(tensorFlowPort < 0)
        {
            _logger.debug("exiting");
            throw new IllegalArgumentException("tensorFlowPort may not be negative");
        }

        if(maxNetworkWaitTimeMs < 0)
        {
            _logger.debug("exiting");
            throw new IllegalArgumentException("tensorFlowPort may not be negative");
        }

        _tensorFlowHost = tensorFlowHost;
	    _logger.trace("_tensorFlowHost:" + _tensorFlowHost);
        _tensorFlowPort = tensorFlowPort;
        _maxNetworkWaitTimeMs = maxNetworkWaitTimeMs;
        _logger.debug("exiting");
    }


    /**
     * Reads the first 4 bytes of inputStream and interprets them as a 32-bit big endian signed integer 'length'.
     * Reads the next 'length' bytes from inputStream and returns them as a UTF-8 encoded string.
     *
     * @param inputStream the source of bytes
     * @return the string form of the n+4 bytes
     * @throws IOException if there is a problem reading from the stream.
     */
    private static String receiveString(InputStream inputStream) throws IOException
    {
        _logger.debug("entering");
        byte[] responseBytes;
        {
            DataInputStream dis = new DataInputStream(inputStream);
            int numResponseBytes = dis.readInt();
            responseBytes = new byte[numResponseBytes];
            dis.readFully(responseBytes);
        }

        _logger.debug("exiting");
        return new String(responseBytes, StandardCharsets.UTF_8);

    }

    /**
     * Gets the byte representation of string in UTF-8 encoding, sends the length of the byte encoding across
     * outputStream via writeInt(...), and then sends the byte representation via write(byte[]).
     *
     * @param string the string to send
     * @param outputStream the destination of string
     * @throws IOException if there is a problem sending the string
     */
    private static void sendString(String string, DataOutput outputStream) throws IOException
    {
        _logger.debug("entering");
        byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        outputStream.writeInt(stringBytes.length);
        outputStream.write(stringBytes);
        _logger.debug("exiting");
    }


    @Override
    String generateAsts(String evidence, int maxProgramCount, Integer sampleCount) throws SynthesiseException
    {
         /*
         * Contact the remote Python server and provide evidence to be fed to Tensor Flow to generate solution
         * ASTs.
         */
        String astsJson;
        try(Socket pyServerSocket = new Socket(_tensorFlowHost, _tensorFlowPort))
        {
            pyServerSocket.setSoTimeout(_maxNetworkWaitTimeMs); // only wait this long for response then throw exception

            JSONObject requestObj = new JSONObject();
            requestObj.put("evidence", evidence);
            requestObj.put("max ast count", maxProgramCount);

            if(sampleCount != null)
                requestObj.put("sample count", sampleCount);

            sendString(requestObj.toString(2), new DataOutputStream(pyServerSocket.getOutputStream()));

            astsJson = receiveString(pyServerSocket.getInputStream());
            _logger.trace("astsJson:" + astsJson);
        }
        catch (IOException e)
        {
            _logger.debug("exiting");
            throw new SynthesiseException(e);
        }

        return astsJson;
    }
}
