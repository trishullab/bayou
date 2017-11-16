package edu.rice.cs.caper.floodgage.application.floodgage;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.SynthesizerBayou_1_1_0;
import edu.rice.cs.caper.floodgage.application.floodgage.view.ViewConsole;
import net.openhft.compiler.CompilerUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;


public class FloodGageMain
{

    public static void main(String[] args) throws Exception
    {
        StatusLogger.getLogger().setLevel(Level.OFF);

        if(args.length != 1)
        {
            System.out.println("Usage: java -cp [trails.jar]:[floodgage.jar] [bayou-hostname]");
            return;
        }

        String bayouHostname = args[0];
        SynthesizerBayou_1_1_0 synthesizer = new SynthesizerBayou_1_1_0(bayouHostname, 8080);

        new FloodGage().run(synthesizer, new ViewConsole());

        System.out.println("");
    }

}
