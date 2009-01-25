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

import junit.framework.TestCase;
import org.jets3t.service.Jets3tProperties;

import java.util.Random;

/**
 * @author Matthias L. Jugel
 */
public class S3TestCase extends TestCase {
  protected static final String ROOT;

  static {
    String propertiesFileName = System.getProperty("moxo.properties", "moxo.properties");
    Jets3tProperties properties = Jets3tProperties.getInstance(propertiesFileName);
    System.out.println("ignoring original vfs.url settings for test: " + properties.getStringProperty("vfs.uri", "ram:/"));
    ROOT = "s3://MOXOTEST" + String.format("%X", new Random(System.currentTimeMillis()).nextLong()) + "/";
    System.out.println("using " + ROOT);
  }
}
