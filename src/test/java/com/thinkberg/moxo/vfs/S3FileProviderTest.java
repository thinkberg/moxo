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

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.jets3t.service.Jets3tProperties;
import junit.framework.TestCase;
import com.thinkberg.moxo.S3TestCase;

/**
 * @author Matthias L. Jugel
 */
public class S3FileProviderTest extends S3TestCase {
  public void testDoCreateFileSystem() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT);
    assertEquals(BUCKETID, ((S3FileName)object.getName()).getRootFile());
  }

  public void testRootDirectoryIsFolder() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT);
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testGetDirectory() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + "/Sites");
    assertEquals(FileType.FOLDER, object.getType());
  }

  public void testGetDirectoryListing() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + "/Sites/Sites/images");
    FileObject[] files = object.findFiles(new DepthFileSelector(1));
    assertEquals(4, files.length);
  }

  public void testMissingFile() throws FileSystemException {
    FileObject object = VFS.getManager().resolveFile(ROOT + "/nonexisting.txt");
    assertFalse(object.exists());
  }
}
