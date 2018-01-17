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
package edu.rice.cs.caper.bayou.application.dom_driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Batch DOM Driver that can work with a list of files having the same config
 */
public class BatchDriver {

    public static void main(String args[]) throws IOException, InterruptedException {
        if (args.length != 2) {
            System.out.println("Usage: dom-driver-batch.jar file-containing-list-of-files.txt config.json");
            return;
        }

        String listFile = args[0];
        String configFile = args[1];
        BufferedReader br = new BufferedReader(new FileReader(listFile));
        String file;
        int NPROCS = Runtime.getRuntime().availableProcessors();
        System.out.println("Going to run " + NPROCS + " threads");

        while ((file = br.readLine()) != null) {
            System.out.println(file);
            String[] driverArgs = { "-f", file, "-c", configFile, "-o", file + ".json" };
            Thread thread = new Thread(() -> Driver.main(driverArgs));
            while (Thread.activeCount() >= NPROCS)
                Thread.sleep(10);
            thread.start();
        }
    }

}
