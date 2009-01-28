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

package com.thinkberg.vfs.s3.tests;

import com.thinkberg.vfs.s3.S3FileName;
import com.thinkberg.vfs.s3.S3TestCase;
import com.thinkberg.vfs.s3.jets3t.Jets3tFileSystem;
import junit.framework.Assert;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.*;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Matthias L. Jugel
 */
public class S3FileProviderTest extends S3TestCase {
  private static final FileSelector ALL_FILE_SELECTOR = new FileSelector() {

    public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
      return true;
    }

    public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
      return true;
    }
  };
  private static final String FOLDER = "/directory";
  private static final String FILE = FOLDER + "/newfile.txt";
  private static final String FILE_NON_EXISTING = "/nonexisting.txt";
  private static final String ATTR_TESTKEY = "TESTKEY";
  protected static final String ATTR_TESTVALUE = "TESTVALUE";

  static {
    LogFactory.getLog(S3FileProviderTest.class).debug("initializing ...");
    try {
      ROOT.delete(ALL_FILE_SELECTOR);
    } catch (FileSystemException e) {
      // just delete, ignore the rest
    }
  }

  public void testDoCreateFileSystem() throws FileSystemException {
    FileObject object = ROOT.resolveFile("/");
    Assert.assertEquals(BUCKETID, ((S3FileName) object.getName()).getRootFile());
  }

  public void testFileSystemIsEmpty() throws FileSystemException {
    FileObject object = ROOT.resolveFile("/");
    assertEquals(1, object.findFiles(ALL_FILE_SELECTOR).length);
  }

  public void testRootDirectoryIsFolder() throws FileSystemException {
    FileObject object = ROOT.resolveFile("/");
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testCreateFolder() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    assertFalse(object.exists());
    object.createFolder();
    assertTrue(object.exists());
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testGetFolder() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    assertTrue(object.exists());
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testCreateFile() throws IOException {
    FileObject object = ROOT.resolveFile(FILE);
    assertFalse(object.exists());
    OutputStream os = object.getContent().getOutputStream();
    os.write(0xfc);
    os.close();
    assertTrue(object.exists());
    assertEquals(FileType.FILE, object.getType());
  }

  public void testCreateEmptyFile() throws IOException {
    FileObject object = ROOT.resolveFile(FILE + ".empty");
    assertFalse(object.exists());
    object.createFile();
    assertTrue(object.exists());
    assertEquals(FileType.FILE, object.getType());
  }

  public void testFileHasLastModifiedTimestamp() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FILE);
    object.getContent().getLastModifiedTime();
  }

  public void testGetFolderListing() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    assertEquals(2, object.getChildren().length);
  }

  public void testGetFolderListingIsShallow() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    FileObject subFolder = object.resolveFile("subdir");
    subFolder.createFolder();
    subFolder.resolveFile("subfile.0").createFile();
    subFolder.resolveFile("subfile.1").createFile();
    subFolder.resolveFile("subfile.2").createFile();

    assertEquals(3, object.getChildren().length);
  }

  public void testMissingFile() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FILE_NON_EXISTING);
    assertFalse(object.exists());
  }

  public void testGetLastModifiedTimeFolder() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    object.getContent().getLastModifiedTime();
  }

  public void testGetLastModifiedTimeFile() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FILE);
    object.getContent().getLastModifiedTime();
  }

  public void testDeleteFile() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FILE);
    object.delete();
    assertFalse(object.exists());
  }

  public void testDeleteFolder() throws FileSystemException {
    FileObject object = ROOT.resolveFile(FOLDER);
    object.delete(ALL_FILE_SELECTOR);
    assertFalse(object.exists());
  }

  public void testCopyFile() throws FileSystemException {
    FileObject srcObject = ROOT.resolveFile(FILE);
    srcObject.createFile();
    assertTrue("source object should exist", srcObject.exists());
    FileObject dstObject = ROOT.resolveFile(FILE + ".dst");
    assertFalse("destination should not exist", dstObject.exists());
    dstObject.copyFrom(srcObject, ALL_FILE_SELECTOR);
    assertTrue("destination should exist after copy", dstObject.exists());

    srcObject.delete();
    dstObject.delete();
  }

  public void testCopyFileWithAttribute() throws FileSystemException {
    if (!BUCKETID.startsWith("s3:")) {
      FileObject srcObject = ROOT.resolveFile(FILE);
      srcObject.createFile();
      srcObject.getContent().setAttribute(ATTR_TESTKEY, ATTR_TESTVALUE);
      assertTrue("source object should exist", srcObject.exists());
      assertEquals("source object attribute missing",
                   ATTR_TESTVALUE, srcObject.getContent().getAttribute(ATTR_TESTKEY));
      FileObject dstObject = ROOT.resolveFile(FILE + ".dst");
      assertFalse("destination should not exist", dstObject.exists());
      dstObject.copyFrom(srcObject, ALL_FILE_SELECTOR);
      assertTrue("destination should exist after copy", dstObject.exists());
      assertEquals("destination object attribute missing",
                   ATTR_TESTVALUE, dstObject.getContent().getAttribute(ATTR_TESTKEY));

      srcObject.delete();
      dstObject.delete();
    } else {
      LogFactory.getLog(S3FileProviderTest.class).info(String.format("ignoring property test for '%s'", ROOT));
    }
  }

  public void testCopyShallowFolder() throws FileSystemException {
    FileObject origFolder = ROOT.resolveFile(FOLDER);
    origFolder.createFolder();

    origFolder.resolveFile("file.0").createFile();
    origFolder.resolveFile("file.1").createFile();
    origFolder.resolveFile("file.2").createFile();

    assertEquals(3, origFolder.getChildren().length);

    FileObject destFolder = ROOT.resolveFile(FOLDER + "_dest");
    assertFalse(destFolder.exists());
    destFolder.copyFrom(origFolder, new DepthFileSelector(1));
    assertTrue(destFolder.exists());

    assertEquals(3, destFolder.getChildren().length);

    FileObject[] origFiles = origFolder.findFiles(new DepthFileSelector(1));
    FileObject[] destFiles = destFolder.findFiles(new DepthFileSelector(1));
    for (int i = 0; i < origFiles.length; i++) {
      assertEquals(origFiles[i].getName().getRelativeName(origFolder.getName()),
                   destFiles[i].getName().getRelativeName(destFolder.getName()));
    }

    origFolder.delete(ALL_FILE_SELECTOR);
    destFolder.delete(ALL_FILE_SELECTOR);

    assertFalse(origFolder.exists());
    assertFalse(destFolder.exists());
  }

  public void testMoveShallowFolder() throws FileSystemException {
    FileObject origFolder = ROOT.resolveFile(FOLDER);
    origFolder.delete(ALL_FILE_SELECTOR);
    origFolder.createFolder();

    origFolder.resolveFile("file.0").createFile();
    origFolder.resolveFile("file.1").createFile();
    origFolder.resolveFile("file.2").createFile();

    assertEquals(3, origFolder.getChildren().length);

    FileObject destFolder = ROOT.resolveFile(FOLDER + "_dest");
    destFolder.delete(ALL_FILE_SELECTOR);
    assertFalse(destFolder.exists());

    origFolder.moveTo(destFolder);
    assertFalse(origFolder.exists());
    assertTrue(destFolder.exists());

    assertEquals(3, destFolder.getChildren().length);

    destFolder.delete(ALL_FILE_SELECTOR);

    assertFalse(origFolder.exists());
    assertFalse(destFolder.exists());
  }

  public void testMoveDeepFolder() throws FileSystemException {
    FileObject origFolder = ROOT.resolveFile(FOLDER);
    origFolder.delete(ALL_FILE_SELECTOR);
    origFolder.createFolder();

    origFolder.resolveFile("file.0").createFile();
    origFolder.resolveFile("file.1").createFile();
    origFolder.resolveFile("file.2").createFile();
    origFolder.resolveFile("subfolder").createFolder();
    origFolder.resolveFile("subfolder").resolveFile("subfile.0").createFile();
    origFolder.resolveFile("subfolder").resolveFile("subfile.1").createFile();
    origFolder.resolveFile("subfolder").resolveFile("subfile.2").createFile();


    FileObject[] origFiles = origFolder.findFiles(ALL_FILE_SELECTOR);
    assertEquals(8, origFiles.length);

    FileObject destFolder = ROOT.resolveFile(FOLDER + "_dest");
    destFolder.delete(ALL_FILE_SELECTOR);
    assertFalse(destFolder.exists());

    origFolder.moveTo(destFolder);
    assertFalse(origFolder.exists());
    assertTrue(destFolder.exists());

    FileObject[] destFiles = destFolder.findFiles(ALL_FILE_SELECTOR);
    assertEquals(8, destFiles.length);

    destFolder.delete(ALL_FILE_SELECTOR);

    assertFalse(origFolder.exists());
    assertFalse(destFolder.exists());
  }

  public void testCloseFileSystem() throws FileSystemException {
    VFS.getManager().closeFileSystem(ROOT.getFileSystem());
  }

  public void testDestroyFileSystem() throws FileSystemException {
    final FileSystem fileSystem = ROOT.getFileSystem();
    assertTrue(fileSystem instanceof Jets3tFileSystem);
    ((Jets3tFileSystem) fileSystem).destroyFileSystem();
  }

  private class DepthFileSelector implements FileSelector {
    int depth = 0;

    public DepthFileSelector(int depth) {
      this.depth = depth;
    }

    public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
      return fileInfo.getDepth() <= depth;
    }

    public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
      return fileInfo.getDepth() < depth;
    }
  }
}
