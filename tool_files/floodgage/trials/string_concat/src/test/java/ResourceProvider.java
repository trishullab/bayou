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
import java.io.*;

// THIS FILE IS A PART OF THE TESTPACK MAVEN INTEGRATION AND SHOULD NOT BE MODIFIED FOR TESTPACK CREATION.

class ResourceProvider
{
    /**
     * Gets the bytes of the resource file located at src/test/resources/[name] both when the test pack is being run
     * from within maven or when the test pack is in jar form.
     *
     * @param name the name of the resource file
     * @return the byte contents of the file
     * @throws IOException if there is a problem accessing the file
     */
    static byte[] getResource(String name) throws IOException
    {
        ClassLoader loader = new ResourceProvider().getClass().getClassLoader();
        InputStream resourceStream = loader.getResourceAsStream(name);

        /*
         * Fill buffer with the contents of the resource file.
         */
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = resourceStream.read(data, 0, data.length)) != -1)
                buffer.write(data, 0, nRead);

            buffer.flush();
        }

        return buffer.toByteArray();
    }
}
