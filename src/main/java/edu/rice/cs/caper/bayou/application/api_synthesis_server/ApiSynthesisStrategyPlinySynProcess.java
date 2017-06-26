//package edu.rice.pliny.programs.api_synthesis_server;
//
//import edu.rice.pliny.library.console_out_process.ConsoleOutProcessLangProcess;
//import edu.rice.pliny.library.console_out_process.ConsoleOutProcessOutputCollector;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.StandardOpenOption;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.UUID;
//
///**
// * Created by barnett on 3/9/17.
// */
//class ApiSynthesisStrategyPlinySynProcess implements ApiSynthesisStrategy
//{
//    /**
//     * Place to send logging information.
//     */
//    private static final Logger _logger = LogManager.getLogger(ApiSynthesisStrategyPlinySynProcess.class.getName());
//
//    private final boolean _deleteSynthesiseInputFileWhenDone;
//
//    private final  File _pathToSynthesizeScript;
//
//    private final long _scriptTimeLimitBeforeKillMs;
//
//    ApiSynthesisStrategyPlinySynProcess(File pathToSynthesizeScript, boolean deleteSynthesiseInputFileWhenDone,
//                                        long scriptTimeLimitBeforeKillMs)
//    {
//        _logger.debug("entering");
//        _deleteSynthesiseInputFileWhenDone = deleteSynthesiseInputFileWhenDone;
//        _pathToSynthesizeScript = pathToSynthesizeScript;
//        _scriptTimeLimitBeforeKillMs = scriptTimeLimitBeforeKillMs;
//        _logger.debug("exiting");
//    }
//
//    @Override
//    public Iterable<String> synthesise(String searchCode) throws SynthesiseException
//    {
//        _logger.debug("entering");
//        File inputFile;
//        try
//        {
//            String prefix = UUID.randomUUID().toString();
//            inputFile = File.createTempFile(prefix, ".java");
//
//            if(_deleteSynthesiseInputFileWhenDone)
//                inputFile.deleteOnExit();
//        }
//        catch (IOException e)
//        {
//            _logger.debug("exiting");
//            throw new SynthesiseException(e);
//        }
//
//        try
//        {
//            return synthesiseHelp(searchCode, inputFile);
//        }
//        finally
//        {
//            if(_deleteSynthesiseInputFileWhenDone)
//                if(!inputFile.delete()) // attempt delete of file
//                    _logger.warn("Could not delete " + inputFile.getAbsolutePath());
//
//            _logger.debug("exiting");
//        }
//    }
//
//    private Iterable<String> synthesiseHelp(String searchCode, File inputFile) throws SynthesiseException
//    {
//        _logger.debug("entering");
//
//         /*
//         * Write the given search code to the input file.
//         */
//        try
//        {
//            Files.write(inputFile.toPath(), searchCode.getBytes(), StandardOpenOption.WRITE);
//        }
//        catch (IOException e)
//        {
//            _logger.debug("exiting");
//            throw new SynthesiseException(e);
//        }
//
//        /*
//         * Launch the completion process using the input file as input using the command (as if from command line):
//         *
//         *    /install_path/sbt --error 'set showSuccess := false' "run-main edu.rice.pliny.main.CodeCompletionMain [input filename]"
//         *
//         * in accordance with https://github.com/riceplinygroup/pliny/tree/master/pliny-reasoning/code-completion
//         *
//         * Accumulate all the output from the completion process from start to unforced termination to processOutput
//         * unless _completionProcessTimeLimitBeforeKillMs time elapses or an error occurs. In either of the latter
//         * cases throw CodeCompletionException exception.
//         *
//         * If a timeout or any other error occurs, attempt to destroy process so it does not run forever.
//         */
//        String processOutput;
//        // try block ensures completionProcess.destroy() has been called no matter what the outcome.
//        String[] command = new String[] { _pathToSynthesizeScript.getAbsolutePath(), inputFile.getAbsolutePath()  };
//        try(ConsoleOutProcessLangProcess completionProcess = new ConsoleOutProcessLangProcess(command, _pathToSynthesizeScript.getParentFile()))
//        {
//            ConsoleOutProcessOutputCollector collector =
//                    new ConsoleOutProcessOutputCollector(completionProcess, _scriptTimeLimitBeforeKillMs);
//
//            try
//            {
//                // n.b. will throw TimeoutException if execution beyond _completionProcessTimeLimitBeforeKillMs
//                processOutput = collector.startAndCollect();
//            }
//            catch (Throwable e) // n.b. covers the TimeoutException case, obviously
//            {
//                _logger.debug("exiting");
//                throw new SynthesiseException(e); // process kill will attempt via enclosing try(...) block
//            }
//
//        }
//
//        String startPart = "Synthesizing...\n";
//
//        int startIndex = processOutput.indexOf(startPart);
//
//        if(startIndex == -1)
//        {
//            _logger.debug("exiting");
//            return Collections.emptyList();
//        }
//
//	    String programTerminator = "/* --- End of application --- */";
//
//	    int stopIndex = processOutput.lastIndexOf(programTerminator);
//
//	    if(stopIndex == -1)
//        {
//            _logger.debug("exiting");
//            return Collections.emptyList();
//        }
//
//        String resultsPart = processOutput.substring(startIndex + startPart.length(), stopIndex);
//
//        String[] results = resultsPart.split("/\\* --- End of application --- \\*/");
//
//        _logger.debug("exiting");
//        return Arrays.asList(results);
//    }
//}
