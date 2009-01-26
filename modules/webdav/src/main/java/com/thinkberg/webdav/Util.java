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

package com.thinkberg.webdav;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class Util {

  private static final SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

  public static String getDateString(long time) {
    return httpDateFormat.format(new Date(time));
  }

//  public static String getISODateString(long time) {
//    return "";    4
//  }


  public static long copyStream(final InputStream is, final OutputStream os) throws IOException {
    ReadableByteChannel rbc = Channels.newChannel(is);
    WritableByteChannel wbc = Channels.newChannel(os);

    int bytesWritten = 0;
    final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
    while (rbc.read(buffer) != -1) {
      buffer.flip();
      bytesWritten += wbc.write(buffer);
      buffer.compact();
    }
    buffer.flip();
    while (buffer.hasRemaining()) {
      bytesWritten += wbc.write(buffer);
    }

    rbc.close();
    wbc.close();

    return bytesWritten;
  }

  public static String getETag(FileObject object) {
    String fileName = object.getName().getPath();
    String lastModified = "";
    try {
      lastModified = String.valueOf(object.getContent().getLastModifiedTime());
    } catch (FileSystemException e) {
      // ignore error here
    }

    return DigestUtils.shaHex(fileName + lastModified);
  }
}
