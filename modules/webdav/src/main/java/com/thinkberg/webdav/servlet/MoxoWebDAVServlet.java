/*
 * Copyright 2007 Matthias L. Jugel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thinkberg.webdav.servlet;

import com.thinkberg.webdav.*;
import com.thinkberg.webdav.vfs.VFSBackend;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.mortbay.jetty.Response;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class MoxoWebDAVServlet extends HttpServlet {
  private static final Log LOG = LogFactory.getLog(MoxoWebDAVServlet.class);

  private final Map<String, WebdavHandler> handlers = new HashMap<String, WebdavHandler>();

  public MoxoWebDAVServlet() {
    handlers.put("COPY", new CopyHandler());
    handlers.put("DELETE", new DeleteHandler());
    handlers.put("GET", new GetHandler());
    handlers.put("HEAD", new HeadHandler());
    handlers.put("LOCK", new LockHandler());
    handlers.put("MKCOL", new MkColHandler());
    handlers.put("MOVE", new MoveHandler());
    handlers.put("OPTIONS", new OptionsHandler());
    handlers.put("POST", new PostHandler());
    handlers.put("PROPFIND", new PropFindHandler());
    handlers.put("PROPPATCH", new PropPatchHandler());
    handlers.put("PUT", new PutHandler());
    handlers.put("UNLOCK", new UnlockHandler());

  }

  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    String rootUri = servletConfig.getInitParameter("vfs.uri");
    String authDomain = servletConfig.getInitParameter("vfs.auth.domain");
    String authUser = servletConfig.getInitParameter("vfs.auth.user");
    String authPass = servletConfig.getInitParameter("vfs.auth.password");
    try {
      StaticUserAuthenticator userAuthenticator =
              new StaticUserAuthenticator(authDomain, authUser, authPass);
      FileSystemOptions options = new FileSystemOptions();
      DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, userAuthenticator);

      VFSBackend.initialize(rootUri, options);
    } catch (FileSystemException e) {
      LOG.error(String.format("can't create file system backend for '%s'", rootUri));
    }
  }

  public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//    String auth = request.getHeader("Authorization");
//    String login = "", password = "";
//
//    if (auth != null) {
//      auth = new String(Base64.decodeBase64(auth.substring(auth.indexOf(' ') + 1).getBytes()));
//      login = auth.substring(0, auth.indexOf(':'));
//      password = auth.substring(auth.indexOf(':') + 1);
//    }
//
//    AWSCredentials credentials = AWSCredentials.load(password,  ))
//    if (user == null) {
//      response.setHeader("WWW-Authenticate", "Basic realm=\"Moxo\"");
//      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//      return;
//    }


    // show we are doing the litmus test
    String litmusTest = request.getHeader("X-Litmus");
    if (null == litmusTest) {
      litmusTest = request.getHeader("X-Litmus-Second");
    }
    if (litmusTest != null) {
      LOG.info(String.format("WebDAV Litmus Test: %s", litmusTest));
    }

    String method = request.getMethod();
    LOG.debug(String.format(">> %s %s", request.getMethod(), request.getPathInfo()));
    if (handlers.containsKey(method)) {
      handlers.get(method).service(request, response);
    } else {
      response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
    Response jettyResponse = ((Response) response);
    String reason = jettyResponse.getReason();
    LOG.debug(String.format("<< %s (%d%s)", request.getMethod(), jettyResponse.getStatus(), reason != null ? ": " + reason : ""));
  }
}
