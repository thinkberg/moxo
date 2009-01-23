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

import org.apache.commons.vfs.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

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

  static {
    try {
      FileObject rootFs = VFS.getManager().resolveFile(ROOT);
      rootFs.delete(ALL_FILE_SELECTOR);
    } catch (FileSystemException e) {
      // just delete, ignore the rest
    }
  }

  public void testDoCreateFileSystem() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT);
    String bucketId = URI.create(ROOT).getHost();
    assertEquals(bucketId, ((S3FileName) object.getName()).getRootFile());
  }


  public void testFileSystemIsEmpty() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT);
    assertEquals(1, object.findFiles(ALL_FILE_SELECTOR).length);
  }

  public void testRootDirectoryIsFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT);
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testCreateFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FOLDER);
    assertFalse(object.exists());
    object.createFolder();
    assertTrue(object.exists());
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testGetFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FOLDER);
    assertTrue(object.exists());
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testCreateFile() throws IOException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FILE);
    assertFalse(object.exists());
    OutputStream os = object.getContent().getOutputStream();
    os.write(0xfc);
    os.close();
    assertTrue(object.exists());
    assertEquals(FileType.FILE, object.getType());
  }

  public void testCreateEmptyFile() throws IOException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FILE + ".empty");
    assertFalse(object.exists());
    object.createFile();
    assertTrue(object.exists());
    assertEquals(FileType.FILE, object.getType());
  }

  public void testFileHasLastModifiedTimestamp() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FILE);
    object.getContent().getLastModifiedTime();
  }

  public void testGetFolderListing() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FOLDER);
    FileObject[] files = object.findFiles(new DepthFileSelector(1));
    assertEquals(3, files.length);
  }

  public void testMissingFile() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + "/nonexisting.txt");
    assertFalse(object.exists());
  }

  public void testGetLastModifiedTimeFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FOLDER);
    object.getContent().getLastModifiedTime();
  }

  public void testGetLastModifiedTimeFile() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FILE);
    object.getContent().getLastModifiedTime();
  }

  public void testDeleteFile() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FILE);
    object.delete();
    assertFalse(object.exists());
  }

  public void testDeleteFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + FOLDER);
    object.delete(ALL_FILE_SELECTOR);
    assertFalse(object.exists());
  }

  public void testCopyFolder() throws FileSystemException {
    FileObject origFolder = VFS.getManager().resolveFile(ROOT + FOLDER);
    origFolder.createFolder();

    origFolder.resolveFile("file.0").createFile();
    origFolder.resolveFile("file.1").createFile();
    origFolder.resolveFile("file.2").createFile();

    FileObject[] origFiles = origFolder.findFiles(new DepthFileSelector(1));
    assertEquals(4, origFiles.length);

    FileObject destFolder = VFS.getManager().resolveFile(ROOT + FOLDER + "_dest");
    assertFalse(destFolder.exists());
    destFolder.copyFrom(origFolder, new DepthFileSelector(1));
    assertTrue(destFolder.exists());

    FileObject[] destFiles = destFolder.findFiles(new DepthFileSelector(1));
    System.err.println(Arrays.asList(destFiles));
    assertEquals(4, destFiles.length);

    for (int i = 0; i < origFiles.length; i++) {
      assertEquals(origFiles[i].getName().getRelativeName(origFolder.getName()),
                   destFiles[i].getName().getRelativeName(destFolder.getName()));
    }

    origFolder.delete(ALL_FILE_SELECTOR);
    destFolder.delete(ALL_FILE_SELECTOR);

    assertFalse(origFolder.exists());
    assertFalse(destFolder.exists());
  }

  public void testMoveFolder() throws FileSystemException {

  }

  public void testCloseFileSystem() throws FileSystemException {
    FileSystem fs = VFS.getManager().resolveFile(ROOT).getFileSystem();
    VFS.getManager().closeFileSystem(fs);
  }

}
