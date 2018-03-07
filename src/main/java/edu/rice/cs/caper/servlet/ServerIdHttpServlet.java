/*
Copyright 2018 Rice University

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A http servlet that always adds the response header "Backend-Server".  The default value for the header is
 * "server", but may be changed (once) via {@link #setServerId}.
 */
public class ServerIdHttpServlet extends HttpServlet
{
    /**
     * <code>null</code> if "server" should be stamped on responses, otherwise an alternate id.
     */
    private static volatile String _serverId;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String serverId = _serverId != null ? _serverId : "server";
        resp.setHeader("Backend-Server", serverId);

        super.service(req, resp);
    }

    /**
     * Sets the id that should be used as the value of the "Backend-Server" response header. May not be null.
     * @param id the server id
     * @throws IllegalStateException if ever invoked more than once.
     */
    public static void setServerId(String id)
    {
        if(id == null)
            throw new NullPointerException("id");

        if(_serverId != null)
            throw new IllegalStateException("serverId already set");

        _serverId = id;
    }
}
