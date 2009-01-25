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

import com.thinkberg.moxo.dav.lock.LockException;
import com.thinkberg.moxo.dav.lock.LockManager;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public abstract class CopyMoveBase extends WebdavHandler {

  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    boolean overwrite = getOverwrite(request);
    FileObject object = getVFSObject(request.getPathInfo());
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

    copyOrMove(object, targetObject, getDepth(request));
  }

  protected abstract void copyOrMove(FileObject object, FileObject target, int depth) throws FileSystemException;

}
