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

import junit.framework.TestCase;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.jets3t.service.Jets3tProperties;

import java.io.InputStream;

import com.thinkberg.moxo.S3TestCase;

/**
 * @author Matthias L. Jugel
 */
public class S3WrapperTest extends S3TestCase {
  public void testCreateConnection() throws FileSystemException {
    assertNotNull(S3Connector.getInstance());
  }

  public void testGetS3BucketRoot() throws FileSystemException {
    assertNotNull(S3Connector.getInstance().getRoot(BUCKETID));
  }

  public void testGetS3BucketRootMissing() throws FileSystemException {
    try {
      assertNull(S3Connector.getInstance().getRoot(BUCKETID+".NOTEXISTING"));
    } catch (FileSystemException e) {
      assertNotNull(e);
    }
  }

  public void testGetRootFolder() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject slashObject = root.getObject("/");
    assertEquals("/", slashObject.getName());
  }

  public void testRootFolderTypeIsCorrect() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject slashObject = root.getObject("/");
    assertEquals(FileType.FOLDER, slashObject.getType());
  }

  public void testRootFolderListing() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject slashObject = root.getObject("/");
    assertEquals(1, slashObject.getChildren().length);
  }

  public void testFolderTypeIsCorrect() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject folderObject = root.getObject("/Sites");
    assertEquals(FileType.FOLDER, folderObject.getType());
  }

  public void testFileTypeIsCorrect() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject fileObject = root.getObject("/Sites/Sites/index.html");
    assertEquals(FileType.FILE, fileObject.getType());
  }

  public void testFolderSize() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject folderObject = root.getObject("/Sites/Sites/images");
    assertEquals(3, folderObject.getChildren().length);
  }

  public void testGetFile() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject fileObject = root.getObject("/Sites/Sites/images/macosxlogo.gif");
    assertNotNull(fileObject.getInputStream());
  }

  public void testGetMissingFileIsImaginary() throws FileSystemException {
    S3VfsRoot root = S3Connector.getInstance().getRoot(BUCKETID);
    S3VfsObject fileObject = root.getObject("/notexisting.txt");
    assertEquals(FileType.IMAGINARY, fileObject.getType());
  }
}
