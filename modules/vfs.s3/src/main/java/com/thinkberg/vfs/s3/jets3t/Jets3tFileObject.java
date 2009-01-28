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

package com.thinkberg.vfs.s3.jets3t;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.util.MonitorOutputStream;
import org.jets3t.service.Constants;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.utils.Mimetypes;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of the virtual S3 file system object using the Jets3t library.
 *
 * @author Matthias L. Jugel
 */
public class Jets3tFileObject extends AbstractFileObject {
  private static final Log LOG = LogFactory.getLog(Jets3tFileObject.class);

  private static final String VFS_LAST_MODIFIED_TIME = "vfs-last-modified-time";

  private final S3Service service;
  private final S3Bucket bucket;

  private boolean attached = false;
  private boolean contentCached = false;

  private S3Object object;
  private File cacheFile;

  public Jets3tFileObject(FileName fileName,
                          Jets3tFileSystem fileSystem,
                          S3Service service, S3Bucket bucket) {
    super(fileName, fileSystem);
    this.service = service;
    this.bucket = bucket;
  }

  /**
   * Attach S3 Object to VFS object.
   * This method only downloads the meta-data without the actual content.
   * If the object does not exist, it will be created locally.
   *
   * @throws Exception if the S3 access fails for some reason
   */
  protected void doAttach() throws Exception {
    if (!attached) {
      try {
        object = service.getObjectDetails(bucket, getS3Key());
        if (object.getMetadata(VFS_LAST_MODIFIED_TIME) == null) {
          // it is possible the bucket has no last-modified data, use the S3 data then
          object.addMetadata(Constants.REST_METADATA_PREFIX + VFS_LAST_MODIFIED_TIME, "" + object.getLastModifiedDate().getTime());
        }
        contentCached = false;
        LOG.debug(String.format("attaching (existing) '%s'", object.getKey()));
      } catch (S3ServiceException e) {
        object = new S3Object(bucket, getS3Key());
        object.addMetadata(Constants.REST_METADATA_PREFIX + VFS_LAST_MODIFIED_TIME, "" + new Date().getTime());
        contentCached = true;
        LOG.debug(String.format("attaching (new) '%s'", object.getKey()));
      }

      attached = true;
    }
  }

  protected void doDetach() throws Exception {
    if (attached) {
      LOG.debug(String.format("detaching '%s' (cached=%b)", object.getKey(), (cacheFile != null)));
      object = null;
      if (cacheFile != null) {
        cacheFile.delete();
        cacheFile = null;
        contentCached = false;
      }
      attached = false;
    }
  }

  protected void doDelete() throws Exception {
    // do not delete the root folder
    if ("".equals(object.getKey())) {
      LOG.warn(String.format("ignored attempt to delete root folder '%s' ", bucket.getName()));
      return;
    }
    LOG.debug(String.format("deleting '%s'", object.getKey()));
    service.deleteObject(bucket, object.getKey());
    if (cacheFile != null) {
      cacheFile.delete();
      cacheFile = null;
      contentCached = false;
    }
    attached = false;
  }

  protected void doRename(FileObject targetFileObject) throws Exception {
    String bucketId = bucket.getName();
    S3Object targetObject = ((Jets3tFileObject) targetFileObject).object;

    LOG.debug(String.format("move object '%s' to '%s'", getS3Key(), targetObject.getKey()));

    // if this is a folder, then rename all children of the current folder too
    if (FileType.FOLDER.equals(getType())) {
      String path = object.getKey();
      // make sure we add a '/' slash at the end to find children
      if (!"".equals(path)) {
        path = path + "/";
      }

      try {
        S3Object[] children = service.listObjects(bucket, path, null);
        LOG.debug(children);
        String targetName = targetObject.getKey();
        for (S3Object child : children) {
          String targetChildName = child.getKey();
          targetChildName = targetName + targetChildName.substring(object.getKey().length());
          service.renameObject(bucketId, child.getKey(), new S3Object(bucket, targetChildName));
        }
      } catch (S3ServiceException e) {
        throw new FileSystemException(String.format("can't move children of '%s' to '%s'", object.getKey(), targetObject.getKey()), e);
      }
    }

    try {
      service.renameObject(bucket.getName(), object.getKey(), ((Jets3tFileObject) targetFileObject).object);
    } catch (S3ServiceException e) {
      throw new FileSystemException("can't rename  object", e);
    }
  }

  @Override
  public void copyFrom(FileObject file, FileSelector selector) throws FileSystemException {
    super.copyFrom(file, selector);
  }

  protected void doCreateFolder() throws Exception {
    if (!Mimetypes.MIMETYPE_JETS3T_DIRECTORY.equals(object.getContentType())) {
      object.setContentType(Mimetypes.MIMETYPE_JETS3T_DIRECTORY);

      LOG.debug(String.format("creating folder '%s'", object.getKey()));
      service.putObject(bucket, object);
    }
  }

  protected long doGetLastModifiedTime() throws Exception {
    String timeStamp = (String) object.getMetadata(VFS_LAST_MODIFIED_TIME);
    if (null != timeStamp) {
      return Long.parseLong(timeStamp);
    }
    return 0;
  }

  protected void doSetLastModifiedTime(final long modtime) throws Exception {
    object.addMetadata(Constants.REST_METADATA_PREFIX + VFS_LAST_MODIFIED_TIME, modtime);
    service.updateObjectMetadata(bucket.getName(), object);
  }

  protected InputStream doGetInputStream() throws Exception {
    if (!contentCached) {
      object = service.getObject(bucket, getS3Key());
      LOG.debug(String.format("caching content of '%s'", object.getKey()));

      InputStream objectInputStream = object.getDataInputStream();
      if (object.getContentLength() > 0) {
        ReadableByteChannel rbc = Channels.newChannel(objectInputStream);
        FileChannel cacheFc = getCacheFile().getChannel();
        cacheFc.transferFrom(rbc, 0, object.getContentLength());
        cacheFc.close();
        rbc.close();
      } else {
        objectInputStream.close();
      }
      contentCached = true;
    }

    return Channels.newInputStream(getCacheFile().getChannel());
  }

  protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
    return new MonitorOutputStream(Channels.newOutputStream(getCacheFile().getChannel())) {
      protected void onClose() throws IOException {
        try {
          LOG.debug(String.format("sending '%s' to storage (cached=%b)", object.getKey(), cacheFile));
          LOG.debug(object);
          if (cacheFile != null) {
            FileChannel cacheFc = getCacheFile().getChannel();
            object.setContentLength(cacheFc.size());
            object.setDataInputStream(Channels.newInputStream(cacheFc));
          }
          service.putObject(bucket, object);
        } catch (S3ServiceException e) {
          LOG.error(String.format("can't send object '%s' to storage", object), e);
        }
      }
    };
  }

  protected FileType doGetType() throws Exception {
    if (null == object.getContentType()) {
      return FileType.IMAGINARY;
    }

    String contentType = object.getContentType();
    if ("".equals(object.getKey()) || Mimetypes.MIMETYPE_JETS3T_DIRECTORY.equals(contentType)) {
      return FileType.FOLDER;
    }

    return FileType.FILE;
  }

  protected String[] doListChildren() throws FileSystemException {
    String path = object.getKey();
    // make sure we add a '/' slash at the end to find children
    if (!"".equals(path)) {
      path = path + "/";
    }

    try {
      S3Object[] children = service.listObjects(bucket, path, "/");
      LOG.debug(Arrays.asList(children));
      LOG.debug(Arrays.asList(children));
      String[] childrenNames = new String[children.length];
      for (int i = 0; i < children.length; i++) {
        if (!children[i].getKey().equals(path)) {
          // strip path from name (leave only base name)
          childrenNames[i] = children[i].getKey().replaceAll("[^/]*//*", "");
        }
      }

      return childrenNames;
    } catch (S3ServiceException e) {
      throw new FileSystemException(String.format("can't list children of '%s'", path), e);
    }
  }

  protected long doGetContentSize() throws Exception {
    return object.getContentLength();
  }

  @SuppressWarnings("unchecked")
  protected Map doGetAttributes() throws Exception {
    Map metaData = object.getModifiableMetadata();
    Map attributes = new HashMap<Object, Object>(metaData.size());
    for (Object key : metaData.keySet()) {
      if (((String) key).startsWith(Constants.REST_METADATA_PREFIX)) {
        attributes.put(((String) key).substring(Constants.REST_METADATA_PREFIX.length()), metaData.get(key));
      } else {
        attributes.put(key, metaData.get(key));
      }
    }
    LOG.debug(String.format("%s[%s]", object.getKey(), attributes));
    return attributes;
  }

  @SuppressWarnings("unchecked")
  protected void doSetAttribute(String attrName, Object value) throws Exception {
    object.addMetadata(Constants.REST_METADATA_PREFIX + attrName, value);
    service.updateObjectMetadata(bucket.getName(), object);
  }

  // Utility methods
  /**
   * Create an S3 key from a commons-vfs path. This simply
   * strips the slash from the beginning if it exists.
   *
   * @return the S3 object key
   */
  private String getS3Key() {
    String path = getName().getPath();
    if ("".equals(path)) {
      return path;
    } else {
      return path.substring(1);
    }
  }

  private RandomAccessFile getCacheFile() throws IOException, S3ServiceException {
    if (cacheFile == null) {
      cacheFile = File.createTempFile("moxo.", ".s3f");
      cacheFile.deleteOnExit();
    }
    return new RandomAccessFile(cacheFile, "rw");
  }
}
