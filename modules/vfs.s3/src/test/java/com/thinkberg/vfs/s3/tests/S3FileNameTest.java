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
import com.thinkberg.vfs.s3.S3FileNameParser;
import com.thinkberg.vfs.s3.S3TestCase;
import junit.framework.Assert;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;

import java.net.URI;

/**
 * @author Matthias L. Jugel
 */
public class S3FileNameTest extends S3TestCase {
  public void testGetBucketFromUri() throws FileSystemException {
    String uriString = ROOT + "/junk.txt";
    String bucketId = URI.create(uriString).getHost();
    FileName fileName = S3FileNameParser.getInstance().parseUri(null, null, uriString);
    Assert.assertEquals(bucketId, ((S3FileName) fileName).getRootFile());
  }

  public void testGetRootFolderFromUri() throws FileSystemException {
    String path = "/myfolder";
    String uri = ROOT + path;
    FileName fileName = S3FileNameParser.getInstance().parseUri(null, null, uri);
    assertEquals(path, fileName.getPath());
  }
}
