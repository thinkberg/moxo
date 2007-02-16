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

package com.thinkberg.moxo.vfs;

import com.thinkberg.moxo.s3.S3VfsObject;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileObject;

import java.io.InputStream;

/**
 * A VFS wrapper for the S3 file object. All requests are proxied through to the actual
 * data object that contains an implementation of the virtual S3 Object.
 *
 * @author Matthias L. Jugel
 * @see com.thinkberg.moxo.s3.S3VfsObject
 */
public class S3FileObject extends AbstractFileObject {

  private final S3VfsObject s3Object;

  /**
   * Create a new S3 file object with the given virtual S3 object as data backend.
   *
   * @param fileName   the file name of the current file in the virtual filesystem
   * @param fileSystem the filesystem used (@see S3FileSystem)
   * @param s3Object   the actual data object
   */
  @SuppressWarnings({"WeakerAccess"})
  protected S3FileObject(FileName fileName, S3FileSystem fileSystem, S3VfsObject s3Object) {
    super(fileName, fileSystem);
    this.s3Object = s3Object;
  }

  protected FileType doGetType() throws Exception {
    return s3Object.getType();
  }

  protected String[] doListChildren() throws Exception {
    return s3Object.getChildren();
  }

  protected long doGetContentSize() throws Exception {
    return s3Object.getContentLength();
  }


  protected long doGetLastModifiedTime() throws Exception {
    return s3Object.getLastModified();
  }

  protected InputStream doGetInputStream() throws Exception {
    return s3Object.getInputStream();
  }

}
