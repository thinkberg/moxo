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
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;

/**
 * @author Matthias L. Jugel
 */
public class S3Connector {
  private static final String APPLICATION_DESCRIPTION = "S3 VFS Connector/1.0";

  private static S3Connector instance;

  /**
   * Get an instance of the S3Connector which is initialized and authenticated to the
   * Amazon S3 Service.
   *
   * @return an S3 connector
   * @throws FileSystemException if connection or authentication fails
   */
  public static S3Connector getInstance() throws FileSystemException {
    if (null == instance) {
      instance = new S3Connector();
    }
    return instance;
  }

  private S3Service service;

  /**
   * Initialize Amazon S3.
   *
   * @throws FileSystemException if S3 can't be initialized
   */
  private S3Connector() throws FileSystemException {
    String propertiesFileName = System.getProperty("moxo.properties", "moxo.properties");
    Jets3tProperties properties = Jets3tProperties.getInstance(propertiesFileName);

    if (!properties.isLoaded()) {
      throw new FileSystemException("can't find S3 configuration: " + propertiesFileName);
    }

    AWSCredentials awsCredentials = new AWSCredentials(
            properties.getStringProperty("accesskey", null),
            properties.getStringProperty("secretkey", null));


    try {
      service = new RestS3Service(awsCredentials, APPLICATION_DESCRIPTION, null);
    } catch (S3ServiceException e) {
      throw new FileSystemException("can't initialize S3 Service", e);
    }
  }

  /**
   * Get a virtual file system root corresponding to a bucket.
   *
   * @param bucket the bucket that contains the filesystem
   * @return the S3 root
   * @throws FileSystemException if the bucket is not found
   */
  public S3VfsRoot getRoot(String bucket) throws FileSystemException {
    try {
      if (service.isBucketAccessible(bucket)) {
        return new S3VfsRootImpl(service, new S3Bucket(bucket));
      }
      throw new FileSystemException("vsf.provider.vfs/cant-access.error");
    } catch (S3ServiceException e) {
      throw new FileSystemException(e);
    }
  }
}
