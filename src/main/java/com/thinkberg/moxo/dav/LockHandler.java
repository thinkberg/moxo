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

package com.thinkberg.moxo.dav;

import com.thinkberg.moxo.dav.lock.Lock;
import com.thinkberg.moxo.dav.lock.LockConflictException;
import com.thinkberg.moxo.dav.lock.LockManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

/**
 * Handle WebDAV LOCK requests.
 *
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class LockHandler extends WebdavHandler {
  private static final Log LOG = LogFactory.getLog(LockHandler.class);

  private static final String TAG_LOCKSCOPE = "lockscope";
  private static final String TAG_LOCKTYPE = "locktype";
  private static final String TAG_OWNER = "owner";
  private static final String TAG_HREF = "href";
  private static final String TAG_PROP = "prop";
  private static final String TAG_LOCKDISCOVERY = "lockdiscovery";

  private static final String HEADER_LOCK_TOKEN = "Lock-Token";

  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    FileObject object = getVFSObject(request.getPathInfo());

    try {
      final LockManager manager = LockManager.getInstance();
      final LockManager.EvaluationResult evaluation = manager.evaluateCondition(object, getIf(request));
      if (!evaluation.result) {
        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
        return;
      } else {
        if (!evaluation.locks.isEmpty()) {
          LOG.debug(String.format("discovered locks: %s", evaluation.locks));
          sendLockAcquiredResponse(response, evaluation.locks.get(0));
          return;
        }
      }
    } catch (LockConflictException e) {
      List<Lock> locks = e.getLocks();
      for (Lock lock : locks) {
        if (Lock.EXCLUSIVE.equals(lock.getType())) {
          response.sendError(WebdavHandler.SC_LOCKED);
          return;
        }
      }
    } catch (ParseException e) {
      response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return;
    }

    try {
      SAXReader saxReader = new SAXReader();
      Document lockInfo = saxReader.read(request.getInputStream());
      //log(lockInfo);

      Element rootEl = lockInfo.getRootElement();
      String lockScope = null, lockType = null;
      Object owner = null;
      Iterator elIt = rootEl.elementIterator();
      while (elIt.hasNext()) {
        Element el = (Element) elIt.next();
        if (TAG_LOCKSCOPE.equals(el.getName())) {
          lockScope = el.selectSingleNode("*").getName();
        } else if (TAG_LOCKTYPE.equals(el.getName())) {
          lockType = el.selectSingleNode("*").getName();
        } else if (TAG_OWNER.equals(el.getName())) {
          // TODO correctly handle owner
          Node subEl = el.selectSingleNode("*");
          if (subEl != null && TAG_HREF.equals(subEl.getName())) {
            owner = new URL(el.selectSingleNode("*").getText());
          } else {
            owner = el.getText();
          }
        }
      }

      LOG.debug("LOCK(" + lockType + ", " + lockScope + ", " + owner + ")");

      Lock requestedLock = new Lock(object, lockType, lockScope, owner, getDepth(request), getTimeout(request));
      try {
        LockManager.getInstance().acquireLock(requestedLock);
        sendLockAcquiredResponse(response, requestedLock);
      } catch (LockConflictException e) {
        response.sendError(SC_LOCKED);
      } catch (IllegalArgumentException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      }
    } catch (DocumentException e) {
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private void sendLockAcquiredResponse(HttpServletResponse response, Lock lock) throws IOException {
    if (!lock.getObject().exists()) {
      response.setStatus(SC_CREATED);
    }
    response.setContentType("text/xml");
    response.setCharacterEncoding("UTF-8");
    response.setHeader(HEADER_LOCK_TOKEN, "<" + lock.getToken() + ">");

    Document propDoc = DocumentHelper.createDocument();
    Element propEl = propDoc.addElement(TAG_PROP, "DAV:");
    Element lockdiscoveryEl = propEl.addElement(TAG_LOCKDISCOVERY);

    lock.serializeToXml(lockdiscoveryEl);

    XMLWriter xmlWriter = new XMLWriter(response.getWriter());
    xmlWriter.write(propDoc);

    logXml(propDoc);
  }

  private void logXml(Node element) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      XMLWriter xmlWriter = new XMLWriter(bos, OutputFormat.createPrettyPrint());
      xmlWriter.write(element);
      LOG.debug(bos.toString());
    } catch (IOException e) {
      LOG.debug("ERROR writing XML log: " + e.getMessage());
    }
  }
}
