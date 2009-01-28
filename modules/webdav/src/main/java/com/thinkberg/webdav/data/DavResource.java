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

package com.thinkberg.webdav.data;

import com.thinkberg.webdav.Util;
import com.thinkberg.webdav.lock.Lock;
import com.thinkberg.webdav.lock.LockManager;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class DavResource extends AbstractDavResource {

  public DavResource(FileObject object) {
    super(object);
  }

  protected boolean setPropertyValue(Element root, Element propertyEl) {
    LogFactory.getLog(getClass()).debug(String.format("[%s].set('%s')", object.getName(), propertyEl.asXML()));

    if (!ALL_PROPERTIES.contains(propertyEl.getName())) {
      final String nameSpace = propertyEl.getNamespaceURI();
      final String attributeName = getFQName(nameSpace, propertyEl.getName());
      try {
        FileContent objectContent = object.getContent();
        final String command = propertyEl.getParent().getParent().getName();
        if (TAG_PROP_SET.equals(command)) {
          StringWriter propertyValueWriter = new StringWriter();
          propertyEl.write(propertyValueWriter);
          propertyValueWriter.close();
          objectContent.setAttribute(attributeName, propertyValueWriter.getBuffer().toString());
        } else if (TAG_PROP_REMOVE.equals(command)) {
          objectContent.setAttribute(attributeName, null);
        }
        root.addElement(propertyEl.getQName());
        return true;
      } catch (IOException e) {
        LogFactory.getLog(getClass()).error(String.format("can't store attribute property '%s' = '%s'",
                                                          attributeName,
                                                          propertyEl.asXML()), e);
      }
    }
    return false;
  }

  /**
   * Add the value for a given property to the result document. If the value
   * is missing or can not be added for some reason it will return false to
   * indicate a missing property.
   *
   * @param root         the root element for the result document fragment
   * @param propertyName the property name to query
   * @return true for successful addition and false for missing data
   */
  protected boolean getPropertyValue(Element root, String propertyName, boolean ignoreValue) {
    LogFactory.getLog(getClass()).debug(String.format("[%s].get('%s')", object.getName(), propertyName));
    if (PROP_CREATION_DATE.equals(propertyName)) {
      return addCreationDateProperty(root, ignoreValue);
    } else if (PROP_DISPLAY_NAME.equals(propertyName)) {
      return addGetDisplayNameProperty(root, ignoreValue);
    } else if (PROP_GET_CONTENT_LANGUAGE.equals(propertyName)) {
      return addGetContentLanguageProperty(root, ignoreValue);
    } else if (PROP_GET_CONTENT_LENGTH.equals(propertyName)) {
      return addGetContentLengthProperty(root, ignoreValue);
    } else if (PROP_GET_CONTENT_TYPE.equals(propertyName)) {
      return addGetContentTypeProperty(root, ignoreValue);
    } else if (PROP_GET_ETAG.equals(propertyName)) {
      return addGetETagProperty(root, ignoreValue);
    } else if (PROP_GET_LAST_MODIFIED.equals(propertyName)) {
      return addGetLastModifiedProperty(root, ignoreValue);
    } else if (PROP_LOCK_DISCOVERY.equals(propertyName)) {
      return addLockDiscoveryProperty(root, ignoreValue);
    } else if (PROP_RESOURCETYPE.equals(propertyName)) {
      return addResourceTypeProperty(root, ignoreValue);
    } else if (PROP_SOURCE.equals(propertyName)) {
      return addSourceProperty(root, ignoreValue);
    } else if (PROP_SUPPORTED_LOCK.equals(propertyName)) {
      return addSupportedLockProperty(root, ignoreValue);
    } else {
      // handle non-standard properties (keep a little separate)
      if (PROP_QUOTA.equals(propertyName)) {
        return addQuotaProperty(root, ignoreValue);
      } else if (PROP_QUOTA_USED.equals(propertyName)) {
        return addQuotaUsedProperty(root, ignoreValue);
      } else if (PROP_QUOTA_AVAILABLE_BYTES.equals(propertyName)) {
        return addQuotaAvailableBytesProperty(root, ignoreValue);
      } else if (PROP_QUOTA_USED_BYTES.equals(propertyName)) {
        return addQuotaUsedBytesProperty(root, ignoreValue);
      } else {
        try {
          Object propertyValue = object.getContent().getAttribute(propertyName);
          if (null != propertyValue) {
            if (((String) propertyValue).startsWith("<")) {
              try {
                Document property = DocumentHelper.parseText((String) propertyValue);
                if (ignoreValue) {
                  property.clearContent();
                }
                root.add(property.getRootElement().detach());
                return true;
              } catch (DocumentException e) {
                LogFactory.getLog(getClass()).error("property value unparsable", e);
                return false;
              }
            } else {
              Element el = root.addElement(propertyName);
              if (!ignoreValue) {
                el.addText((String) propertyValue);
              }
              return true;
            }

          }
        } catch (FileSystemException e) {
          LogFactory.getLog(this.getClass()).error(String.format("property '%s' is not supported", propertyName), e);
        }
      }
    }

    return false;
  }

  protected boolean addCreationDateProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addGetDisplayNameProperty(Element root, boolean ignoreValue) {
    Element el = root.addElement(PROP_DISPLAY_NAME);
    if (!ignoreValue) {
      el.addCDATA(object.getName().getBaseName());
    }
    return true;
  }

  protected boolean addGetContentLanguageProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addGetContentLengthProperty(Element root, boolean ignoreValue) {
    try {
      Element el = root.addElement(PROP_GET_CONTENT_LENGTH);
      if (!ignoreValue) {
        el.addText("" + object.getContent().getSize());
      }
      return true;
    } catch (FileSystemException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected boolean addGetContentTypeProperty(Element root, boolean ignoreValue) {
    try {
      String contentType = object.getContent().getContentInfo().getContentType();
      if (null == contentType || "".equals(contentType)) {
        return false;
      }

      Element el = root.addElement(PROP_GET_CONTENT_TYPE);
      if (!ignoreValue) {
        el.addText(contentType);
      }
      return true;
    } catch (FileSystemException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected boolean addGetETagProperty(Element root, boolean ignoreValue) {
    root.addElement(PROP_GET_ETAG, Util.getETag(object));
    return true;
  }

  protected boolean addGetLastModifiedProperty(Element root, boolean ignoreValue) {
    try {
      Element el = root.addElement(PROP_GET_LAST_MODIFIED);
      if (!ignoreValue) {
        el.addText(Util.getDateString(object.getContent().getLastModifiedTime()));
      }
      return true;
    } catch (FileSystemException e) {
      e.printStackTrace();
      return false;
    }
  }

  protected boolean addLockDiscoveryProperty(Element root, boolean ignoreValue) {
    Element lockdiscoveryEl = root.addElement(PROP_LOCK_DISCOVERY);
    try {
      List<Lock> locks = LockManager.getInstance().discoverLock(object);
      if (locks != null && !locks.isEmpty()) {
        for (Lock lock : locks) {
          if (lock != null && !ignoreValue) {
            lock.serializeToXml(lockdiscoveryEl);
          }
        }
      }
      return true;
    } catch (FileSystemException e) {
      root.remove(lockdiscoveryEl);
      e.printStackTrace();
      return false;
    }
  }

  protected boolean addResourceTypeProperty(Element root, boolean ignoreValue) {
    root.addElement(PROP_RESOURCETYPE);
    return true;
  }

  protected boolean addSourceProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addSupportedLockProperty(Element root, boolean ignoreValue) {
    Element supportedlockEl = root.addElement(PROP_SUPPORTED_LOCK);
    if (!ignoreValue) {
      Element exclLockentryEl = supportedlockEl.addElement("lockentry");
      exclLockentryEl.addElement("lockscope").addElement("exclusive");
      exclLockentryEl.addElement("locktype").addElement("write");
      Element sharedLockentryEl = supportedlockEl.addElement("lockentry");
      sharedLockentryEl.addElement("lockscope").addElement("shared");
      sharedLockentryEl.addElement("locktype").addElement("write");
    }

    return true;
  }

  protected boolean addQuotaProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addQuotaUsedProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addQuotaAvailableBytesProperty(Element root, boolean ignoreValue) {
    return false;
  }

  protected boolean addQuotaUsedBytesProperty(Element root, boolean ignoreValue) {
    return false;
  }
}
