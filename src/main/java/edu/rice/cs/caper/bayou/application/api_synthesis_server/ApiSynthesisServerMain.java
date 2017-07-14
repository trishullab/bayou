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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ApiSynthesisServerMain
{
    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServerMain.class.getName());

    /*
     * Register a global exception logger.
     */
    static
    {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) ->
        {
            if(_logger == null)
            {
                e.printStackTrace();
                return;
            }

            for(Throwable i = e; i != null; i = i.getCause())
                _logger.error(t, i);

        });
    }

    public static void main(String[] args)
    {
        _logger.debug("entering");

        /*
         * Construct a new api synthesis server.
         */
        try
        {
            new ApiSynthesisServerRest().start();
        }
        catch (Throwable e)
        {
            _logger.fatal("Error creating ApiSynthesisServer", e);
            _logger.debug("exiting");
            System.exit(1);
        }

        _logger.debug("exiting");
    }
}
