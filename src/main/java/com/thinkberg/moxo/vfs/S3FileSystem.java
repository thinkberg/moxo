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

import com.thinkberg.moxo.s3.S3VfsRoot;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.Collection;


/**
 * @author Matthias L. Jugel
 */
public class S3FileSystem extends AbstractFileSystem implements FileSystem {
  private final S3VfsRoot s3VfsRoot;

  @SuppressWarnings({"WeakerAccess"})
  protected S3FileSystem(S3FileName fileName, FileSystemOptions fileSystemOptions, S3VfsRoot s3VfsRoot) {
    super(fileName, null, fileSystemOptions);
    this.s3VfsRoot = s3VfsRoot;
  }

  @SuppressWarnings({"unchecked"})
  protected void addCapabilities(Collection caps) {
    caps.addAll(S3FileProvider.capabilities);
  }

  protected FileObject createFile(FileName fileName) throws Exception {
    return new S3FileObject(fileName, this, s3VfsRoot.getObject(fileName.getPathDecoded()));
  }

}
