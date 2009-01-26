/*
 * Copyright 2009 Matthias L. Jugel.
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

package com.thinkberg.webdav.vfs;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class VFSBackend {
  private static VFSBackend instance;

  private final FileObject fileSystemRoot;

  public static void initialize(String rootUri, FileSystemOptions options) throws FileSystemException {
    if (null == instance) {
      instance = new VFSBackend(rootUri, options);
    }
  }

  private VFSBackend(String rootUri, FileSystemOptions options) throws FileSystemException {
    fileSystemRoot = VFS.getManager().resolveFile(rootUri, options);
  }

  public static FileObject resolveFile(String path) throws FileSystemException {
    if (null == instance) {
      throw new IllegalStateException("VFS backend not initialized");
    }
    return instance.fileSystemRoot.resolveFile(path);
  }
}
