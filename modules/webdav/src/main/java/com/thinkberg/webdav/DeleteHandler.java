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

import com.thinkberg.webdav.lock.LockException;
import com.thinkberg.webdav.lock.LockManager;
import com.thinkberg.webdav.vfs.VFSBackend;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class DeleteHandler extends WebdavHandler {
  private static final Log LOG = LogFactory.getLog(DeleteHandler.class);

  private final static FileSelector ALL_FILES_SELECTOR = new FileSelector() {
    public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
      return true;
    }

    public boolean traverseDescendents(FileSelectInfo fileSelectInfo) throws Exception {
      return true;
    }
  };

  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    FileObject object = VFSBackend.resolveFile(request.getPathInfo());

    try {
      String fragment = new URI(request.getRequestURI()).getFragment();
      if (fragment != null) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    } catch (URISyntaxException e) {
      throw new IOException(e.getMessage());
    }

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
      int deletedObjects = object.delete(ALL_FILES_SELECTOR);
      LOG.debug("deleted " + deletedObjects + " objects");
      if (deletedObjects > 0) {
        response.setStatus(HttpServletResponse.SC_OK);
      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
