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

package com.thinkberg.moxo.s3;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;

import java.io.InputStream;

/**
 * This interface describes methods to access S3 Objects whether files or folders.
 *
 * @author Matthias L. Jugel
 */
public interface S3VfsObject {
  /**
   * The base name of the S3 Object.
   *
   * @return the base name without path
   */
  public String getName();

  /**
   * The absolute path of the S3 Object.
   *
   * @return the absolute path
   */
  public String getPath();

  /**
   * The type of the S3 Object. May be file, folder or imaginary.
   *
   * @return the file type
   * @see org.apache.commons.vfs.FileType#FILE
   * @see org.apache.commons.vfs.FileType#FOLDER
   * @see org.apache.commons.vfs.FileType#IMAGINARY
   */
  public FileType getType();

  /**
   * Get the last modified time. The value is undefined if this is an imaginary file.
   *
   * @return the last modified time in milliseconds
   */
  public long getLastModified();

  /**
   * Get the content length. The value may be 0 for imaginary files.
   *
   * @return the content length in bytes
   */
  public long getContentLength();

  /**
   * Get the actual content MIME type. May be null for imaginary files.
   *
   * @return the MIME type
   */
  public String getContentType();

  /**
   * Get the input stream to read data from the object or null if this file is imaginary
   *
   * @return the input stream
   * @throws FileSystemException if the S3 object cannot be accessed
   */
  public InputStream getInputStream() throws FileSystemException;

  /**
   * List all children names relative to this S3 Object. It is assumed that this is
   * only called on folders. Non-Folders may return results but there is no defined
   * behaviour in such a case.
   *
   * @return the list of children names (without path)
   * @throws FileSystemException if the object cannot be accessed
   */
  public String[] getChildren() throws FileSystemException;
}
