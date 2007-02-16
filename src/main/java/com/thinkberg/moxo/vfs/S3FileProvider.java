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

import com.thinkberg.moxo.s3.S3Connector;
import com.thinkberg.moxo.s3.S3VfsRoot;
import org.apache.commons.vfs.Capability;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.AbstractOriginatingFileProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Matthias L. Jugel
 */
public class S3FileProvider extends AbstractOriginatingFileProvider {

  public final static Collection capabilities = Collections.unmodifiableCollection(Arrays.asList(
/*
          Capability.CREATE,
          Capability.DELETE,
          Capability.RENAME,
*/
Capability.GET_TYPE,
Capability.GET_LAST_MODIFIED,
/*
          Capability.SET_LAST_MODIFIED_FILE,
          Capability.SET_LAST_MODIFIED_FOLDER,
*/
Capability.LIST_CHILDREN,
Capability.READ_CONTENT,
Capability.URI/*,

          Capability.WRITE_CONTENT,
          Capability.APPEND_CONTENT,
          Capability.RANDOM_ACCESS_READ,
          Capability.RANDOM_ACCESS_WRITE
*/
  ));


  public S3FileProvider() {
    super();
    setFileNameParser(S3FileNameParser.getInstance());
  }

  protected FileSystem doCreateFileSystem(FileName fileName, FileSystemOptions fileSystemOptions) throws FileSystemException {
    S3FileName s3FileName = (S3FileName) fileName;
    String s3BucketId = s3FileName.getRootFile();
    S3VfsRoot s3VfsRoot = S3Connector.getInstance().getRoot(s3BucketId);
    return new S3FileSystem(s3FileName, fileSystemOptions, s3VfsRoot);
  }

  public Collection getCapabilities() {
    return capabilities;
  }
}
