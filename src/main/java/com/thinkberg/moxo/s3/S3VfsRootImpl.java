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
import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;

/**
 * Virtual Root of an S3 file system using the Jets3t library. The root
 * stores the service and bucket to create file objects found in the
 * bucket.
 *
 * @author Matthias L. Jugel
 */
public class S3VfsRootImpl implements S3VfsRoot {
  private final S3Service service;
  private final S3Bucket bucket;

  /**
   * Create a new bucket file system root.
   *
   * @param service the S3 service to use
   * @param bucket  the S3 bucket this root is bound to
   */
  public S3VfsRootImpl(S3Service service, S3Bucket bucket) {
    this.service = service;
    this.bucket = bucket;
  }

  /**
   * Create a new S3VfsObject using the given path. The object may not exist
   * in the bucket and will then return an object that has an imaginary file type.
   *
   * @param path the absolute path to the file
   * @return the virtual S3 object representing the file
   * @throws FileSystemException if the file cannot be accessed
   * @see com.thinkberg.moxo.s3.S3VfsObject
   */
  public S3VfsObject getObject(String path) throws FileSystemException {
    return new S3VfsObjectImpl(service, bucket, path);
  }
}
