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

package com.thinkberg.vfs.s3;

import com.thinkberg.vfs.s3.tests.S3FileProviderTest;
import junit.framework.TestCase;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * @author Matthias L. Jugel
 */
public class S3TestCase extends TestCase {
  protected final static String BUCKETID;
  protected static String FSURI;
  protected static FileObject ROOT;
  protected static FileSystemOptions OPTIONS;


  static {
    BUCKETID = "MOXOTEST" + String.format("%X", new Random(System.currentTimeMillis()).nextLong());
    Properties userConfig = new Properties();

    InputStream propertyResource = S3FileProviderTest.class.getResourceAsStream("/s3.auth.properties");
    assertNotNull(propertyResource);

    try {
      userConfig.load(propertyResource);
      StaticUserAuthenticator userAuthenticator =
              new StaticUserAuthenticator("",
                                          userConfig.getProperty("s3.access.key", ""),
                                          userConfig.getProperty("s3.secret.key", ""));
      OPTIONS = new FileSystemOptions();
      FSURI = String.format("s3://%s/", BUCKETID);

      DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(OPTIONS, userAuthenticator);
      ROOT = VFS.getManager().resolveFile(FSURI, OPTIONS);
    } catch (IOException e) {
      assertTrue("initialization failed", false);
    }
  }
}
