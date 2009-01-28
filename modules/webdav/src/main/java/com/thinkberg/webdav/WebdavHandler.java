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

package com.thinkberg.webdav;

import com.thinkberg.webdav.vfs.VFSBackend;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public abstract class WebdavHandler {
  private static final Log LOG = LogFactory.getLog(WebdavHandler.class);

  static final int SC_CREATED = 201;
  static final int SC_LOCKED = 423;
  static final int SC_MULTI_STATUS = 207;

  protected static URL getBaseUrl(HttpServletRequest request) {
    try {
      String requestUrl = request.getRequestURL().toString();
      String requestUri = request.getRequestURI();
      String requestUrlBase = requestUrl.substring(0, requestUrl.length() - requestUri.length());
      return new URL(requestUrlBase);
    } catch (MalformedURLException e) {
      // ignore ...
    }
    return null;
  }

  public abstract void service(HttpServletRequest request, HttpServletResponse response) throws IOException;

  /**
   * Get the depth header value. This value defines how operations
   * like propfind, move, copy etc. handle collections. A depth value
   * of 0 will only return the current collection, 1 will return
   * children too and infinity will recursively operate.
   *
   * @param request the servlet request
   * @return the depth value as 0, 1 or Integer.MAX_VALUE;
   */
  int getDepth(HttpServletRequest request) {
    String depth = request.getHeader("Depth");
    int depthValue;

    if (null == depth || "infinity".equalsIgnoreCase(depth)) {
      depthValue = Integer.MAX_VALUE;
    } else {
      depthValue = Integer.parseInt(depth);
    }

    LOG.debug(String.format("request header: Depth: %s", (depthValue == Integer.MAX_VALUE ? "infinity" : depthValue)));
    return depthValue;
  }

  /**
   * Get the overwrite header value, whether to overwrite destination
   * objects or collections or not.
   *
   * @param request the servlet request
   * @return true or false
   */
  boolean getOverwrite(HttpServletRequest request) {
    String overwrite = request.getHeader("Overwrite");
    boolean overwriteValue = overwrite == null || "T".equals(overwrite);

    LOG.debug(String.format("request header: Overwrite: %s", overwriteValue));
    return overwriteValue;
  }

  /**
   * Get the destination object or collection. The destination header contains
   * a URL to the destination which is returned as a file object.
   *
   * @param request the servlet request
   * @return the file object of the destination
   * @throws FileSystemException   if the file system cannot create a file object
   * @throws MalformedURLException if the url is misformatted
   */
  FileObject getDestination(HttpServletRequest request) throws FileSystemException, MalformedURLException {
    String targetUrlStr = request.getHeader("Destination");
    FileObject targetObject = null;
    if (null != targetUrlStr) {
      URL target = new URL(targetUrlStr);
      targetObject = VFSBackend.resolveFile(target.getPath());
      LOG.debug(String.format("request header: Destination: %s", targetObject.getName().getPath()));
    }

    return targetObject;
  }

  /**
   * Get the if header.
   *
   * @param request the request
   * @return the value if the If: header.
   */
  String getIf(HttpServletRequest request) {
    String getIfHeader = request.getHeader("If");

    if (null != getIfHeader) {
      LOG.debug(String.format("request header: If: '%s'", getIfHeader));
    }
    return getIfHeader;
  }

  /**
   * Get and parse the timeout header value.
   *
   * @param request the request
   * @return the timeout
   */
  long getTimeout(HttpServletRequest request) {
    String timeout = request.getHeader("Timeout");
    if (null != timeout) {
      String[] timeoutValues = timeout.split(",[ ]*");
      LOG.debug(String.format("request header: Timeout: %s", Arrays.asList(timeoutValues).toString()));
      if ("infinity".equalsIgnoreCase(timeoutValues[0])) {
        return -1;
      } else {
        return Integer.parseInt(timeoutValues[0].replaceAll("Second-", ""));
      }
    }
    return -1;
  }

  void logXml(Node element) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      XMLWriter xmlWriter = new XMLWriter(bos, OutputFormat.createPrettyPrint());
      xmlWriter.write(element);
      LogFactory.getLog(this.getClass()).debug(bos.toString());
    } catch (IOException e) {
      LogFactory.getLog(this.getClass()).error(e.getMessage());
    }
  }
}
