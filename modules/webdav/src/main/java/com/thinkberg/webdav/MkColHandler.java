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
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class MkColHandler extends WebdavHandler {

  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    BufferedReader bufferedReader = request.getReader();
    String line = bufferedReader.readLine();
    if (line != null) {
      response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

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
      response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }

    if (!object.getParent().exists() || !FileType.FOLDER.equals(object.getParent().getType())) {
      response.sendError(HttpServletResponse.SC_CONFLICT);
      return;
    }

    try {
      object.createFolder();
      response.setStatus(HttpServletResponse.SC_CREATED);
    } catch (FileSystemException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
