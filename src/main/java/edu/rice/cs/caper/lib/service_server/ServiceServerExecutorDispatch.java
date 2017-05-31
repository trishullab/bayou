package edu.rice.cs.caper.lib.service_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A ServiceServer that listens for requests over a socket and processes those requests in parallel using an
 * ExecutorService.
 */
public abstract class ServiceServerExecutorDispatch implements ServiceServer<IOException>
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ServiceServerExecutorDispatch.class.getName());

    /**
     * The UTF-8 charset.
     */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Provides threads to process requests.
     *
     * ACCESS MUST BE PROTECTED BY TAKING THE _requestProcessingThreadPoolLock LOCK.
     */
    private final ExecutorService _requestProcessingThreadPool;

    /**
     * Protects multithreaded access to _requestProcessingThreadPool.
     */
    // n.b. motivation: stopAsync() must be thread safe
    private final java.lang.Object _requestProcessingThreadPoolLock = new java.lang.Object();

    /**
     * The socket used to listen for incoming client requests.
     */
    // n.b. will be touched by both constructing thread and _listenThread.
    // not explicitly thread safe in documentation, but it appears the only way to unblock .accept() is to call .close()
    // from another thread. But since .accept() blocks, can't take a lock prior to .accept() or would
    // risk starvation attempting to unblock .accept().  I guess since Oracle says to call .close() from another
    // thread they at least made that part thread safe.
    // https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html#close()
    private final ServerSocket _serverSocket;

    /**
     * Whether requests should be listened for on _serverSocket.
     */
    private volatile boolean _listenForNewRequestConnections = true;

    /**
     * The thread that monitors _serverSocket for new connections.
     */
    private final Thread _listenThread = new Thread(this::start);

    /**
     * Protects concurrent access to _startedOnce.
     */
    private final Object _startedOnceLock = new Object();

    /**
     * Indicates if startAsync has ever been called.
     *
     * N.B. ALL ACCESS TO THIS FIELD MUST B SYNCHRONIZED ON _startedOnceLock
     */
    private boolean _startedOnce = false;


    /**
     * Creates a new server prepared to listen to the given port using the given code completion strategy.
     *
     * Note: listening for incoming messages will not occur on the given port until startAsync() is invoked.
     *
     *
     * @param listenPort the port on which to listen for Code Completion Request messages.
     * @param requestProcessingThreadPool thread pool to draw threads from to process a client requests.
     */
    public ServiceServerExecutorDispatch(int listenPort, ExecutorService requestProcessingThreadPool) throws IOException
    {
        _logger.debug("entering");
        _serverSocket = new ServerSocket(listenPort);

        synchronized (_requestProcessingThreadPoolLock) // probably not technically needed because final, but
        {                                               // let's be consistent in our use of requestProcessingThreadPool
            _requestProcessingThreadPool = requestProcessingThreadPool;
        }
    }

    /**
     * Begins listening for requests. Invoking more than once has no effect. This method returns at the conclusion
     * of starting the server. It does not block waiting for the server to stop.
     *
     * This method is thread safe.
     */
    @SuppressWarnings("WeakerAccess")
    public void startAsync()
    {
        _logger.debug("entering");

        synchronized (_startedOnceLock)
        {
            if (!_startedOnce)
                _listenThread.start();

            _startedOnce = true;
        }

        _logger.debug("exiting");
    }


    /**
     * Begins listening for Code Completion Request messages on _serverSocket if _listenForNewRequestConnections is true
     * and responds to each with a Code Completion Response in a thread provided by _executor.
     *
     * If _listenForNewRequestConnections is false, just returns.
     */
    private void start()
    {
        _logger.debug("entering");

        while(_listenForNewRequestConnections)
        {
            /*
             * Await a new client connection.
             */
            Socket clientSocket;
            UUID requestId;
            String clientAddress;
            try
            {
                clientSocket = _serverSocket.accept();

                if(!_listenForNewRequestConnections) // _listenForNewRequestConnections may have been set to false
                    continue;                          // while we were blocked in accept.

                requestId = UUID.randomUUID();
                clientAddress = clientSocket.getInetAddress().toString();

            }
            catch (Throwable e)
            {
                if(!_listenForNewRequestConnections && e instanceof SocketException)
                    return; // assume the result of us calling _serverSocket.close() during teardown.

                for(Throwable i = e; i != null; i = i.getCause())
                    _logger.error("Error accepting connection.", i);

                continue;
            }

            /*
             * Delegate processing a client connection to a different thread.
             */
            try
            {
                _logger.info(requestId + ": Accepted connection from " + clientAddress);

                synchronized (_requestProcessingThreadPoolLock)
                {
                    _requestProcessingThreadPool.execute(() ->
                    {
                        _logger.info(requestId + ": Processing request in thread " + getCurrentThreadName());
                        try
                        {
                            processSocket(clientSocket, requestId);
                        }
                        catch (Throwable e)
                        {
                            for (Throwable i = e; i != null; i = i.getCause())
                                _logger.error(requestId + ": Error processing request.", i);
                        }
                        finally
                        {
                            try
                            {
                                clientSocket.close();
                                _logger.info(requestId + ": Closed connection with " + clientAddress);
                            }
                            catch (Throwable e)
                            {
                                for (Throwable i = e; i != null; i = i.getCause())
                                    _logger.error(requestId + ": Error closing connection with " + clientAddress, i);
                            }

                        }
                    });
                }
            }
            catch(RejectedExecutionException e)
            {
                if(!_listenForNewRequestConnections) // we are shutting down processing and already shutdown _executor
                    continue;

                for(Throwable i = e; i != null; i = i.getCause())
                    _logger.error("Error beginning socket processing.", i);
            }
            catch (Throwable e)
            {
                for(Throwable i = e; i != null; i = i.getCause())
                    _logger.error("Error beginning socket processing.", i);
            }
        }

        try
        {
            _serverSocket.close(); // not a problem if already closed by different thread, will just return.
        }
        catch (IOException e)
        {
            for(Throwable i = e; i != null; i = i.getCause())
             _logger.error("Error closing server socket.", i);
        }

        _logger.debug("exiting");
    }

    /**
     * @return if all processing of client requests is complete and no future client requests will be processed.
     */
    protected boolean isAllClientProcessingComplete()
    {
        synchronized (_requestProcessingThreadPoolLock)
        {
            return _requestProcessingThreadPool.isTerminated();
        }
    }

    /**
     * Prevents the server from accepting any new connections and attempts to shut down all threads related
     * to message processing. This method returns as soon as possible. It does not block waiting for processing
     * threads to terminate.
     *
     * @throws IOException if there is a problem stopping the thread responsible for listening for new connections.
     */
    @SuppressWarnings("WeakerAccess")
    public void stopAsync() throws IOException
    {
        _logger.debug("entering");

        _listenForNewRequestConnections = false;

        synchronized (_requestProcessingThreadPoolLock)
        {
            _requestProcessingThreadPool.shutdownNow();
        }
        _serverSocket.close();

        _logger.debug("exiting");
    }

    /**
     * Reads one Code Completion Request from the input stream of the given socket and writes one
     * Code Completion Response message to the output stream of the socket.
     *
     * @param clientSocket the socket connection to the requestor
     * @param requestId the unique identifier for the request (used for logging errors)
     * @throws IOException if a communication error occurs
     */
    private void processSocket(Socket clientSocket, UUID requestId) throws IOException
    {
        _logger.debug("entering");

        try(InputStream inStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream())
        {
            JSONObject requestMessage = parseMessageBodyToJson(inStream);

            JSONObject responseMessage;
            try
            {
                responseMessage = processClientRequest(requestId, requestMessage);
            }
            catch (Throwable e)
            {
                for(Throwable i = e; i != null; i = i.getCause())
                    _logger.error(requestId + " error in processClientRequest", i);

                responseMessage = makeUnexpectedErrorResponse(requestId);
            }
            sendResponseToClient(responseMessage, outputStream);
        }

        _logger.debug("exiting");
    }

    protected abstract JSONObject processClientRequest(UUID requestId, JSONObject requestMessage);

    protected abstract JSONObject makeUnexpectedErrorResponse(UUID requestId);

    /**
     * Reads one Code Completion Request from the input stream and returns the resulting Code Completion Response
     * message.
     *
     * @param inStream the source of the requestor's request message
     * @return the response
     * @throws IOException if there is an error reading the request
     */
    private JSONObject parseMessageBodyToJson(InputStream inStream) throws IOException
    {
        _logger.debug("entering");

        /*
         * Read first 4 bytes of inStream to determine how many bytes are in the rest of the message.
         */
        int numBytesInMessageBody;
        {
            byte[] messageHeaderBytes = new byte[4];
            for (int i = 0; i < 4; i++)
            {
                int nextByte = inStream.read();
                if (nextByte == -1)
                {
                    _logger.debug("exiting");
                    return makeEarlyEndOfStreamDetectedErrorResponse();
                }

                messageHeaderBytes[i] = (byte) nextByte;
            }
            ByteBuffer numBytesBuffer = ByteBuffer.wrap(messageHeaderBytes);
            numBytesInMessageBody = numBytesBuffer.getInt();
        }

        /*
         * Read the remaining message bytes from inStream and interpret as String
         */
        String messageBodyAsString;
        {

            byte[] messageBody = new byte[numBytesInMessageBody];

            for (int i = 0; i < numBytesInMessageBody; i++)
            {
                int nextByte = inStream.read();
                if (nextByte == -1)
                {
                    _logger.debug("exiting");
                    return makeEarlyEndOfStreamDetectedErrorResponse();
                }

                messageBody[i] = (byte) nextByte;
            }

            messageBodyAsString = new String(messageBody, "UTF-8"); // we expect JSON, and JSON is always UTF-8
        }

        /*
         * Parse message body as JSON and process the message.
         */
        _logger.debug("exiting");
        return new JSONObject(messageBodyAsString);

    }

    protected abstract JSONObject makeEarlyEndOfStreamDetectedErrorResponse();


    /**
     * Writes the given Code Completion Request to the given output stream in binary form.
     *
     * @param responseMessage the request message
     * @param outStream the destination of the message
     * @throws IOException if there is a problem writing to the stream.
     */
    private void sendResponseToClient(JSONObject responseMessage, OutputStream outStream) throws IOException
    {
        _logger.debug("entering");

        /*
         * Covert responseMessage to a UTF-8 byte array.
         */
        byte[] responseBodyBytes;
        {
            ByteBuffer responseByteBuffer = UTF_8.encode(responseMessage.toString());
            responseBodyBytes = new byte[responseByteBuffer.limit()];
            responseByteBuffer.get(responseBodyBytes);
        }

        /*
         * Send header and body to client.
         */
        outStream.write(ByteBuffer.allocate(4).putInt(responseBodyBytes.length).array()); // header
        outStream.write(responseBodyBytes); // body
        outStream.flush(); // unnecessary since we always close the socket after processing, but technically
        // this method doesn't know that

        _logger.debug("exiting");
    }

    /**
     * Returns the name of the invoking thread, or, "unnamed" if the thread has no name or a name composed only
     * of whitespace.
     * @return the name of the invoking thread.  Never returns null.
     */
    private String getCurrentThreadName()
    {
        _logger.debug("entering");

        String threadName = Thread.currentThread().getName();

        // Suppress warning in intelliJ of threadName always being non-null. Code inspection may discover that,
        // but the contract for getName() makes no such guarantee.
        //noinspection ConstantConditions
        if(threadName == null || threadName.trim().length() == 0)
            threadName  = "unnamed";

        _logger.debug("exiting");
        return threadName;
    }

    /**
     * Blocks until all threads related to message processing have terminated.
     *
     * @throws InterruptedException if the blocking is interrupted.
     */
    @SuppressWarnings("WeakerAccess")
    public void join() throws InterruptedException
    {
        _logger.debug("entering");
        _listenThread.join();
        boolean terminalted = false;
        while(!terminalted)
        {
            synchronized (_requestProcessingThreadPoolLock)
            {
                terminalted = _requestProcessingThreadPool.awaitTermination(2, TimeUnit.SECONDS);
            }
        }
        _logger.debug("exiting");
    }

}
