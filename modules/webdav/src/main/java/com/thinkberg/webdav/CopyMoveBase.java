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
import java.io.IOException;
import java.text.ParseException;

/**
 * The body of all COPY or MOVE requests. As these requests are very similar, they are handles mainly
 * by this class. Only the actual execution using the underlying VFS backend is done in sub classes.
 *
 * @author Matthias L. Jugel
 * @version $Id$
 */
public abstract class CopyMoveBase extends WebdavHandler {

  /**
   * Handle a COPY or MOVE request.
   *
   * @param request  the servlet request
   * @param response the servlet response
   * @throws IOException if there is an error executing this request
   */
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    boolean overwrite = getOverwrite(request);
    FileObject object = VFSBackend.resolveFile(request.getPathInfo());
    FileObject targetObject = getDestination(request);

    try {
      final LockManager lockManager = LockManager.getInstance();
      LockManager.EvaluationResult evaluation = lockManager.evaluateCondition(targetObject, getIf(request));
      if (!evaluation.result) {
        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
        return;
      }
      if ("MOVE".equals(request.getMethod())) {
        evaluation = lockManager.evaluateCondition(object, getIf(request));
        if (!evaluation.result) {
          response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
          return;
        }
      }
    } catch (LockException e) {
      response.sendError(SC_LOCKED);
      return;
    } catch (ParseException e) {
      response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return;
    }

    if (null == targetObject) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (object.equals(targetObject)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    if (targetObject.exists()) {
      if (!overwrite) {
        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
        return;
      }
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      FileObject targetParent = targetObject.getParent();
      if (!targetParent.exists() ||
          !FileType.FOLDER.equals(targetParent.getType())) {
        response.sendError(HttpServletResponse.SC_CONFLICT);
      }
      response.setStatus(HttpServletResponse.SC_CREATED);
    }

    // delegate the actual execution to a sub class
    copyOrMove(object, targetObject, getDepth(request));
  }

  /**
   * Execute the actual filesystem operation. Must be implemented by sub classes.
   *
   * @param object the source object
   * @param target the target object
   * @param depth  a depth for copy
   * @throws FileSystemException if there is an error executing the request
   */
  protected abstract void copyOrMove(FileObject object, FileObject target, int depth) throws FileSystemException;

}
