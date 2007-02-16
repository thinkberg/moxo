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
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.utils.Mimetypes;

import java.io.InputStream;

/**
 * Implementation of the virtual S3 file system object using the Jets3t library.
 *
 * @author Matthias L. Jugel
 */
public class S3VfsObjectImpl implements S3VfsObject {
  private final S3Service service;
  private final S3Bucket bucket;
  private S3Object object;

  /**
   * Create a new S3 Object wrapper for the virtual filesystem. If an object exists its details
   * are loaded and a virtual object is created for non-existing files. If data is requested that
   * is not part of the details (HEAD) the real object is loaded on demand.
   *
   * @param service the S3 service used for retrieving the object
   * @param bucket  the bucket the object is located in
   * @param path    the full path to the S3 object (the key)
   * @throws FileSystemException if there is a problem accessing the object
   */
  @SuppressWarnings({"RedundantThrows"})
  public S3VfsObjectImpl(S3Service service, S3Bucket bucket, String path) throws FileSystemException {
    this.service = service;
    this.bucket = bucket;
    // check object and load details or create a virtual object
    String s3Path = makeS3Path(path);
    try {
      object = service.getObjectDetails(bucket, s3Path);
    } catch (S3ServiceException e) {
      object = new S3Object(bucket, s3Path);
    }
  }

  /**
   * Get the name of this object.
   *
   * @return the base name of the object
   */
  public String getName() {
    String key = object.getKey();
    if ("".equals(key)) {
      return "/";
    }

    int lastSlash = key.lastIndexOf("/");
    if (lastSlash == -1) {
      return key;
    } else {
      return key.substring(lastSlash + 1);
    }
  }

  /**
   * The file type as provided by commons-vfs.
   *
   * @return the file type
   * @see org.apache.commons.vfs.FileType#FILE
   * @see org.apache.commons.vfs.FileType#FOLDER
   * @see org.apache.commons.vfs.FileType#IMAGINARY
   */
  public FileType getType() {
    if (null == object.getContentType()) {
      return FileType.IMAGINARY;
    }

    String key = object.getKey();
    String contentType = object.getContentType();
    if ("".equals(key) || "/".equals(key) || Mimetypes.MIMETYPE_JETS3T_DIRECTORY.equals(contentType)) {
      return FileType.FOLDER;
    }

    return FileType.FILE;
  }

  /**
   * Get the full path of the object as an absolute path (including heading /).
   *
   * @return the full path
   */
  public String getPath() {
    return makeAbsolutePath(object.getKey());
  }

  /**
   * Get the last modified time of the object. The result is undefined if this object does not exist.
   *
   * @return the last modified time
   */
  public long getLastModified() {
    return object.getLastModifiedDate().getTime();
  }

  /**
   * Get the content length of the object. The result is 0 for non-existing objects.
   *
   * @return the content length
   */
  public long getContentLength() {
    return object.getContentLength();
  }

  /**
   * Get the actual content type as a MIME type. The result is undefined if this object does not exist.
   *
   * @return the mime type
   */
  public String getContentType() {
    return object.getContentType();
  }

  /**
   * Get all children names relative to this object. It is assumed that
   * this method is called for directories only. The returned names are
   * not absolute names, but rather relative base names.
   *
   * @return the list of children names
   * @throws FileSystemException if the object cannot be accessed
   */
  public String[] getChildren() throws FileSystemException {
    String path = object.getKey();
    // make sure we add a '/' slash at the end to find children
    if (!"".equals(path)) {
      path = path + "/";
    }

    try {
      S3Object[] children = service.listObjects(bucket, path, "/");
      String[] childrenNames = new String[children.length];
      for (int i = 0; i < children.length; i++) {
        if (!children[i].getKey().equals(path)) {
          // strip path from name (leave only base name)
          childrenNames[i] = children[i].getKey().replaceAll("[^/]*//*", "");
        }
      }
      return childrenNames;
    } catch (S3ServiceException e) {
      throw new FileSystemException(e);
    }
  }

  /**
   * Get the input stream to read data from the object. Returns null for non-existing objects.
   * Calling this method causes the object to be reloaded if it has not yet been fully
   * retrieved from S3.
   *
   * @return the input stream
   * @throws FileSystemException if the input stream cannot be accessed
   */
  public InputStream getInputStream() throws FileSystemException {
    try {
      InputStream is = object.getDataInputStream();
      if (null == is) {
        object = service.getObject(bucket, object.getKey());
        is = object.getDataInputStream();
      }
      return is;
    } catch (S3ServiceException e) {
      throw new FileSystemException(e);
    }
  }

  // Utility methods

  /**
   * Make an absolute path out of an S3 (Jets3t path) which does not contain
   * a slash. If the key is "SomeDirectory/afile.txt" it will become
   * "/SomeDirectory/afile.txt". All files will be handled with their full
   * key name so this cannot be confused with relative file names which are
   * handled by commons-vfs.
   *
   * @param key the objects key
   * @return the absolute path name
   */
  private String makeAbsolutePath(String key) {
    return "/" + key;
  }

  /**
   * Create an S3 path (the key) from a commons-vfs path. This simply
   * strips the slash from the beginning if it exists. We assume that the
   * path is always absolute, so a missing slash at the beginning is
   * simply ignored.
   *
   * @param path the absolute file path (with or without heading slash)
   * @return the S3 object key
   */
  private String makeS3Path(String path) {
    if ("".equals(path)) {
      return path;
    } else {
      return path.substring(1);
    }
  }
}
