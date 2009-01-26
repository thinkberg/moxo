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

package com.thinkberg.webdav.lock;

import com.thinkberg.webdav.vfs.DepthFileSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSystemException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lock manager is responsible for exclusive and shared write locks on the
 * DAV server. It is used to acquire a lock, release a lock, discover existing
 * locks or check conditions. The lock manager is a singleton.
 *
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class LockManager {
  private static LockManager instance = null;
  private static final Log LOG = LogFactory.getLog(LockManager.class);

  // condition parser patterns and tokens
  private static final Pattern IF_PATTERN = Pattern.compile("(<[^>]+>)|(\\([^)]+\\))");
  private static final Pattern CONDITION_PATTERN = Pattern.compile("([Nn][Oo][Tt])|(<[^>]+>)|(\\[[^]]+\\])");
  private static final char TOKEN_LOWER_THAN = '<';
  private static final char TOKEN_LEFT_BRACE = '(';
  private static final char TOKEN_LEFT_BRACKET = '[';

  /**
   * Get an instance of the lock manager.
   *
   * @return the lock manager
   */
  public static LockManager getInstance() {
    if (null == instance) {
      instance = new LockManager();
    }

    return instance;
  }

  private final Map<FileObject, List<Lock>> lockMap;

  /**
   * The lock manager is a singleton and cannot be instantiated directly.
   */
  private LockManager() {
    lockMap = new HashMap<FileObject, List<Lock>>();
  }

  /**
   * Acquire a lock. This will first check for conflicts and throws exceptions if
   * there are existing locks or for some reason the lock could not be acquired.
   *
   * @param lock the lock to acquire
   * @throws LockConflictException if an existing lock has priority
   * @throws FileSystemException   if the file object and its path cannot be accessed
   */
  public void acquireLock(Lock lock) throws LockConflictException, FileSystemException {
    checkConflicts(lock);
    addLock(lock);
  }

  /**
   * Release a lock on a file object with a given lock token. Releeases the lock if
   * if one exists and if the lock token is valid for the found lock.
   *
   * @param object the file object we want to unlock
   * @param token  the lock token associated with the file object
   * @return true if the lock has been released, false if not
   */
  public boolean releaseLock(FileObject object, String token) {
    List<Lock> locks = lockMap.get(object);
    if (null != locks) {
      for (Lock lock : locks) {
        if (lock.getToken().equals(token)) {
          locks.remove(lock);
          return true;
        }
      }
      return false;
    }

    return true;
  }

  /**
   * Discover locks for a given file object. This will find locks for the object
   * itself and parent path locks with a depth that reaches the file object.
   *
   * @param object the file object to find locks for
   * @return the locks that are found for this file object
   * @throws FileSystemException if the file object or its parents cannot be accessed
   */
  public List<Lock> discoverLock(FileObject object) throws FileSystemException {
    FileObject parent = object;
    while (parent != null) {
      List<Lock> parentLocks = lockMap.get(parent);
      if (parentLocks != null && !parentLocks.isEmpty()) {
        return parentLocks;
      }
      parent = parent.getParent();
    }

    return null;
  }

  /**
   * Evaluate an 'If:' header condition.
   * The condition may be a tagged list or an untagged list. Tagged lists define the resource, the condition
   * applies to in front of the condition (ex. 1, 2, 5, 6). Conditions may be inverted by using 'Not' at the
   * beginning of the condition (ex. 3, 4, 6). The list constitutes an OR expression while the list of
   * conditions within braces () constitutes an AND expression.
   * <p/>
   * Evaluate example 2:<br/>
   * <code>
   * URI(/resource1) { (
   * is-locked-with(urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2)
   * AND matches-etag(W/"A weak ETag") )
   * OR ( matches-etag("strong ETag") ) }
   * </code>
   * <p/>
   * Examples:
   * <ol>
   * <li> &lt;http://cid:8080/litmus/unmapped_url&gt; (&lt;opaquelocktoken:cd6798&gt;)</li>
   * <li> &lt;/resource1&gt; (&lt;urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2&gt; [W/"A weak ETag"]) (["strong ETag"])</li>
   * <li> (&lt;urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2&gt;) (Not &lt;DAV:no-lock&gt;)</li>
   * <li> (Not &lt;urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2&gt; &lt;urn:uuid:58f202ac-22cf-11d1-b12d-002035b29092&gt;)</li>
   * <li> &lt;/specs/rfc2518.doc&gt; (["4217"])</li>
   * <li> &lt;/specs/rfc2518.doc&gt; (Not ["4217"])</li>
   * </ol>
   *
   * @param contextObject the contextual resource (needed when the If: condition is not tagged)
   * @param ifCondition   the string of the condition as sent by the If: header
   * @return evaluation of the condition expression
   * @throws ParseException        if the condition does not meet the syntax requirements
   * @throws LockConflictException
   * @throws FileSystemException
   */
  public EvaluationResult evaluateCondition(FileObject contextObject, String ifCondition)
          throws FileSystemException, LockConflictException, ParseException {
    List<Lock> locks = discoverLock(contextObject);
    EvaluationResult evaluation = new EvaluationResult();

    if (ifCondition == null || "".equals(ifCondition)) {
      if (locks != null) {
        throw new LockConflictException(locks);
      }
      evaluation.result = true;
      return evaluation;
    }

    Matcher matcher = IF_PATTERN.matcher(ifCondition);
    FileObject resource = contextObject;
    while (matcher.find()) {
      String token = matcher.group();
      switch (token.charAt(0)) {
        case TOKEN_LOWER_THAN:
          String resourceUri = token.substring(1, token.length() - 1);
          try {
            resource = contextObject.getFileSystem().resolveFile(new URI(resourceUri).getPath());
            locks = discoverLock(resource);
          } catch (URISyntaxException e) {
            throw new ParseException(ifCondition, matcher.start());
          }
          break;
        case TOKEN_LEFT_BRACE:
          LOG.debug(String.format("URI(%s) {", resource));
          Matcher condMatcher = CONDITION_PATTERN.matcher(token.substring(1, token.length() - 1));
          boolean expressionResult = true;
          while (condMatcher.find()) {
            String condToken = condMatcher.group();
            boolean negate = false;
            if (condToken.matches("[Nn][Oo][Tt]")) {
              negate = true;
              condMatcher.find();
              condToken = condMatcher.group();
            }
            switch (condToken.charAt(0)) {
              case TOKEN_LOWER_THAN:
                String lockToken = condToken.substring(1, condToken.length() - 1);

                boolean foundLock = false;
                if (locks != null) {
                  for (Lock lock : locks) {
                    if (lockToken.equals(lock.getToken())) {
                      evaluation.locks.add(lock);
                      foundLock = true;
                      break;
                    }
                  }
                }
                final boolean foundLockResult = negate ? !foundLock : foundLock;
                LOG.debug(String.format("  %sis-locked-with(%s) = %b",
                                        negate ? "NOT " : "", lockToken, foundLockResult));
                expressionResult = expressionResult && foundLockResult;
                break;
              case TOKEN_LEFT_BRACKET:
                String eTag = condToken.substring(1, condToken.length() - 1);
                String resourceETag = String.format("%x", resource.hashCode());
                boolean resourceTagMatches = resourceETag.equals(eTag);
                final boolean matchesEtagResult = negate ? !resourceTagMatches : resourceTagMatches;
                LOG.debug(String.format("  %smatches-etag(%s) = %b",
                                        negate ? "NOT " : "", eTag, matchesEtagResult));
                expressionResult = expressionResult && matchesEtagResult;
                break;
              default:
                throw new ParseException(String.format("syntax error in condition '%s' at %d",
                                                       ifCondition, matcher.start() + condMatcher.start()),
                                         matcher.start() + condMatcher.start());
            }
          }

          evaluation.result = evaluation.result || expressionResult;
          LOG.debug("} => " + evaluation.result);
          break;
        default:
          throw new ParseException(String.format("syntax error in condition '%s' at %d", ifCondition, matcher.start()),
                                   matcher.start());
      }
    }

    // regardless of the evaluation, if the object is locked but there is no valed lock token in the
    // conditions we must fail with a lock conflict too
    if (evaluation.result && (locks != null && !locks.isEmpty()) && evaluation.locks.isEmpty()) {
      throw new LockConflictException(locks);
    }
    return evaluation;
  }

  public class EvaluationResult {
    public List<Lock> locks = new ArrayList<Lock>();
    public boolean result = false;

    public String toString() {
      return String.format("EvaluationResult[%b,%s]", result, locks);
    }
  }

  /**
   * Add a lock to the list of shared locks of a given object.
   *
   * @param lock the lock to add
   */
  private void addLock(Lock lock) {
    FileObject object = lock.getObject();
    List<Lock> locks = lockMap.get(object);
    if (null == locks) {
      locks = new ArrayList<Lock>();
      lockMap.put(object, locks);
    }
    locks.add(lock);
  }

  /**
   * Check whether a lock conflicts with already existing locks up and down the path.
   * First we go up the path to check for parent locks that may include the file object
   * and the go down the directory tree (if depth requires it) to check locks that
   * will conflict.
   *
   * @param requestedLock the lock requested
   * @throws LockConflictException if a conflicting lock was found
   * @throws FileSystemException   if the file object or path cannot be accessed
   */
  private void checkConflicts(final Lock requestedLock) throws LockConflictException, FileSystemException {
    // find locks in the parent path
    FileObject parent = requestedLock.getObject();
    while (parent != null) {
      List<Lock> parentLocks = lockMap.get(parent);
      if (parentLocks != null && !parentLocks.isEmpty()) {
        for (Lock parentLock : parentLocks) {
          if (Lock.EXCLUSIVE.equals(requestedLock.getScope()) || Lock.EXCLUSIVE.equals(parentLock.getScope())) {
            throw new LockConflictException(parentLocks);
          }
        }
      }
      parent = parent.getParent();
    }

    // look for locks down the path (if depth requests it)
    if (requestedLock.getDepth() != 0 && requestedLock.getObject().getChildren().length > 0) {
      requestedLock.getObject().findFiles(new DepthFileSelector(1, requestedLock.getDepth()) {
        public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
          List<Lock> childLocks = lockMap.get(fileSelectInfo.getFile());
          for (Lock childLock : childLocks) {
            if (Lock.EXCLUSIVE.equals(requestedLock.getScope()) || Lock.EXCLUSIVE.equals(childLock.getScope())) {
              throw new LockConflictException(childLocks);
            }
          }
          return false;
        }
      }, false, new ArrayList());
    }
  }

}
