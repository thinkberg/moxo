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

import com.thinkberg.webdav.data.AbstractDavResource;
import com.thinkberg.webdav.data.DavResource;
import com.thinkberg.webdav.data.DavResourceFactory;
import com.thinkberg.webdav.lock.LockException;
import com.thinkberg.webdav.lock.LockManager;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle PROPPATCH requests. This currently a dummy only and will return a
 * forbidden status for any attempt to modify or remove a property.
 *
 * @author Matthias L. Jugel
 */
public class PropPatchHandler extends WebdavHandler {
  private static final Log LOG = LogFactory.getLog(PropPatchHandler.class);

  private static final String TAG_MULTISTATUS = "multistatus";
  private static final String TAG_HREF = "href";
  private static final String TAG_RESPONSE = "response";

  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    FileObject object = VFSBackend.resolveFile(request.getPathInfo());

    try {
      if (!LockManager.getInstance().evaluateCondition(object, getIf(request)).result) {
        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
        return;
      }
    } catch (LockException e) {
      response.sendError(SC_LOCKED);
      return;
    } catch (ParseException e) {
      response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return;
    }

    if (object.exists()) {
      SAXReader saxReader = new SAXReader();
      try {
        Document propDoc = saxReader.read(request.getInputStream());
        logXml(propDoc);

        Element propUpdateEl = propDoc.getRootElement();
        List<Element> requestedProperties = new ArrayList<Element>();
        for (Object elObject : propUpdateEl.elements()) {
          Element el = (Element) elObject;
          String command = el.getName();
          if (AbstractDavResource.TAG_PROP_SET.equals(command) || AbstractDavResource.TAG_PROP_REMOVE.equals(command)) {
            for (Object propElObject : el.elements()) {
              for (Object propNameElObject : ((Element) propElObject).elements()) {
                Element propNameEl = (Element) propNameElObject;
                requestedProperties.add(propNameEl);
              }
            }
          }
        }

        // respond as XML encoded multi status
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(SC_MULTI_STATUS);

        Document multiStatusResponse = getMultiStatusResponse(object, requestedProperties, getBaseUrl(request));

        logXml(multiStatusResponse);

        // write the actual response
        XMLWriter writer = new XMLWriter(response.getWriter(), OutputFormat.createCompactFormat());
        writer.write(multiStatusResponse);
        writer.flush();
        writer.close();

      } catch (DocumentException e) {
        LOG.error("invalid request: " + e.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      }
    } else {
      LOG.error(object.getName().getPath() + " NOT FOUND");
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private Document getMultiStatusResponse(FileObject object, List<Element> requestedProperties, URL baseUrl) throws FileSystemException {
    Document propDoc = DocumentHelper.createDocument();
    propDoc.setXMLEncoding("UTF-8");

    Element multiStatus = propDoc.addElement(TAG_MULTISTATUS, "DAV:");
    Element responseEl = multiStatus.addElement(TAG_RESPONSE);
    try {
      URL url = new URL(baseUrl, URLEncoder.encode(object.getName().getPath(), "UTF-8"));
      responseEl.addElement(TAG_HREF).addText(url.toExternalForm());
    } catch (Exception e) {
      LOG.error("can't set HREF tag in response", e);
    }
    DavResource resource = DavResourceFactory.getInstance().getDavResource(object);
    resource.setPropertyValues(responseEl, requestedProperties);
    return propDoc;
  }
}
