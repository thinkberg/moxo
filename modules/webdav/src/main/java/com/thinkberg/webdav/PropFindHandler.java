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

import com.thinkberg.webdav.data.DavResource;
import com.thinkberg.webdav.data.DavResourceFactory;
import com.thinkberg.webdav.vfs.DepthFileSelector;
import com.thinkberg.webdav.vfs.VFSBackend;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class PropFindHandler extends WebdavHandler {
  private static final Log LOG = LogFactory.getLog(PropFindHandler.class);

  // these tags are valid children elements of <propfind>
  private static final List<String> VALID_PROPFIND_TAGS = Arrays.asList(
          TAG_ALLPROP, TAG_PROPNAMES, TAG_PROP
  );

  /**
   * Handle a PROPFIND request.
   *
   * @param request  the servlet request
   * @param response the servlet response
   * @throws IOException if there is an error that cannot be handled normally
   */
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SAXReader saxReader = new SAXReader();
    try {
      Document propDoc = saxReader.read(request.getInputStream());
      logXml(propDoc);

      Element propFindEl = propDoc.getRootElement();
      for (Object propElObject : propFindEl.elements()) {
        Element propEl = (Element) propElObject;
        if (VALID_PROPFIND_TAGS.contains(propEl.getName())) {
          FileObject object = VFSBackend.resolveFile(request.getPathInfo());
          if (object.exists()) {
            // respond as XML encoded multi status
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(SC_MULTI_STATUS);

            Document multiStatusResponse =
                    getMultiStatusResponse(object,
                                           propEl,
                                           getBaseUrl(request),
                                           getDepth(request));
            logXml(multiStatusResponse);

            // write the actual response
            XMLWriter writer = new XMLWriter(response.getWriter(), OutputFormat.createCompactFormat());
            writer.write(multiStatusResponse);
            writer.flush();
            writer.close();

          } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
          }
          break;
        }
      }
    } catch (DocumentException e) {
      LOG.error("invalid request: " + e.getMessage());
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  /**
   * Create a multistatus response by requesting all properties and writing a response for each
   * the found and the non-found properties
   *
   * @param object  the context object the propfind request applies to
   * @param propEl  the &lt;prop&gt; element containing the actual properties
   * @param baseUrl the base url of this server
   * @param depth   a depth argument for how deep the find will go
   * @return an XML document that is the response
   * @throws FileSystemException if there was an error executing the propfind request
   */
  private Document getMultiStatusResponse(FileObject object, Element propEl, URL baseUrl, int depth)
          throws FileSystemException {
    Document propDoc = DocumentHelper.createDocument();
    propDoc.setXMLEncoding("UTF-8");

    Element multiStatus = propDoc.addElement(TAG_MULTISTATUS, NAMESPACE_DAV);
    FileObject[] children = object.findFiles(new DepthFileSelector(depth));
    for (FileObject child : children) {
      Element responseEl = multiStatus.addElement(TAG_RESPONSE);
      try {
        URL url = new URL(baseUrl, URLEncoder.encode(child.getName().getPath(), "UTF-8"));
        responseEl.addElement(TAG_HREF).addText(url.toExternalForm());
      } catch (Exception e) {
        LOG.error("can't set href in response", e);
      }
      DavResource resource = DavResourceFactory.getInstance().getDavResource(child);
      resource.getPropertyValues(responseEl, propEl);
    }
    return propDoc;
  }
}
