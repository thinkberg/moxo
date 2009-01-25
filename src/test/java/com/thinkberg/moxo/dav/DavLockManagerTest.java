package com.thinkberg.moxo.dav;

import com.thinkberg.moxo.dav.lock.Lock;
import com.thinkberg.moxo.dav.lock.LockConflictException;
import com.thinkberg.moxo.dav.lock.LockManager;

/**
 * @author Matthias L. Jugel
 */
public class DavLockManagerTest extends DavTestCase {
  private final String OWNER_STR = "testowner";

  public DavLockManagerTest() {
    super();
  }

  public void testAcquireSingleSharedFileLock() {
    Lock sharedLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    try {
      LockManager.getInstance().acquireLock(sharedLock);
    } catch (Exception e) {
      assertNull(e.getMessage(), e);
    }
  }

  public void testAcquireDoubleSharedFileLock() {
    Lock sharedLock1 = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    Lock sharedLock2 = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR + "1", 0, 3600);
    try {
      LockManager.getInstance().acquireLock(sharedLock1);
      LockManager.getInstance().acquireLock(sharedLock2);
    } catch (Exception e) {
      assertNull(e.getMessage(), e);
    }
  }

  public void testFailToAcquireExclusiveLockOverSharedLock() {
    Lock sharedLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    Lock exclusiveLock = new Lock(aFile, Lock.WRITE, Lock.EXCLUSIVE, OWNER_STR, 0, 3600);
    try {
      LockManager.getInstance().acquireLock(sharedLock);
      LockManager.getInstance().acquireLock(exclusiveLock);
      assertTrue("acquireLock() should fail", false);
    } catch (Exception e) {
      assertEquals(LockConflictException.class, e.getClass());
    }
  }

  public void testConditionUnmappedFails() throws Exception {
    final String condition = "<http://cid:8080/litmus/unmapped_url> (<opaquelocktoken:cd6798>)";
    assertFalse("condition for unmapped resource must fail",
                LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }

  public void testConditionSimpleLockToken() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + ">)";
    LockManager.getInstance().acquireLock(aLock);
    assertTrue("condition with existing lock token should not fail",
               LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }

  public void testConditionSimpleLockLokenWrong() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "x>)";
    LockManager.getInstance().acquireLock(aLock);
    try {
      LockManager.getInstance().evaluateCondition(aFile, condition);
    } catch (LockConflictException e) {
      assertFalse("condition with wrong lock token must fail on locked resource", e.getLocks().isEmpty());
    }
  }

  public void testConditionSimpleLockTokenAndETag() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "> [" + Integer.toHexString(aFile.hashCode()) + "])";
    LockManager.getInstance().acquireLock(aLock);
    assertTrue("condition with existing lock token and correct ETag should not fail",
               LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }

  public void testConditionSimpleLockTokenWrongAndETag() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "x> [" + Integer.toHexString(aFile.hashCode()) + "])";
    LockManager.getInstance().acquireLock(aLock);
    try {
      LockManager.getInstance().evaluateCondition(aFile, condition);
    } catch (LockConflictException e) {
      assertFalse("condition with non-existing lock token and correct ETag should fail",
                  e.getLocks().isEmpty());
    }
  }

  public void testConditionSimpleLockTokenAndETagWrong() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "> [" + Integer.toHexString(aFile.hashCode()) + "x])";
    LockManager.getInstance().acquireLock(aLock);
    assertFalse("condition with existing lock token and incorrect ETag should fail",
                LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }

  public void testConditionSimpleLockTokenWrongAndETagWrong() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "x> [" + Integer.toHexString(aFile.hashCode()) + "x])";
    LockManager.getInstance().acquireLock(aLock);
    assertFalse("condition with non-existing lock token and incorrect ETag should fail",
                LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }

  public void testConditionSimpleLockTokenWrongAndETagOrSimpleETag() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String eTag = Integer.toHexString(aFile.hashCode());
    final String condition = "(<" + aLock.getToken() + "x> [" + eTag + "]) ([" + eTag + "])";
    LockManager.getInstance().acquireLock(aLock);
    try {
      LockManager.getInstance().evaluateCondition(aFile, condition);
    } catch (LockConflictException e) {
      assertFalse("condition with one correct ETag in list should not fail on locked resource",
                  e.getLocks().isEmpty());
    }
  }

  public void testConditionSimpleNegatedLockTokenWrongAndETag() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String eTag = Integer.toHexString(aFile.hashCode());
    final String condition = "(Not <" + aLock.getToken() + "x> [" + eTag + "])";
    assertTrue("condition with negated wrong lock token and correct ETag should not fail on unlocked resource",
               LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }


  public void testConditionMustNotFail() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String condition = "(<" + aLock.getToken() + "x>) (Not <DAV:no-lock>)";
    assertTrue("using (Not <DAV:no-lock>) in condition list must not fail on unlocked resource",
               LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }


  public void testComplexConditionWithBogusLockToken() throws Exception {
    Lock aLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
    final String eTag = Integer.toHexString(aFile.hashCode());
    final String condition = "(<" + aLock.getToken() + "> [" + eTag + "x]) (Not <DAV:no-lock> [" + eTag + "x])";
    LockManager.getInstance().acquireLock(aLock);
    assertFalse("complex condition with bogus eTag should fail",
                LockManager.getInstance().evaluateCondition(aFile, condition).result);
  }


//  assertFalse(lockManager.evaluateCondition(aFile, "</resource1> (<urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2> [W/\"A weak ETag\"]) ([\"strong ETag\"])");
//  lockManager.evaluateCondition(aFile, "(<urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2>) (Not <DAV:no-lock>)");
//  lockManager.evaluateCondition(aFile, "(Not <urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2> <urn:uuid:58f202ac-22cf-11d1-b12d-002035b29092>)");
//  lockManager.evaluateCondition(aFile, "</specs/rfc2518.doc> ([\"4217\"])");
//  lockManager.evaluateCondition(aFile, "</specs/rfc2518.doc> (Not [\"4217\"])");
//  lockManager.evaluateCondition(aFile, "(<urn:uuid:181d4fae-7d8c-11d0-a765-00a0c91e6bf2>) (Not <DAV:no-lock>) </specs/rfc2518.doc> (Not [\"4217\"])");
//  lockManager.evaluateCondition(aFile, "(<opaquelocktoken:10a098> [10a198]) (Not <DAV:no-lock> [10a198])");

//  public void testLockConditionRequiredException() {
//    Lock sharedLock = new Lock(aFile, Lock.WRITE, Lock.SHARED, OWNER_STR, 0, 3600);
//    try {
//      LockManager.getInstance().acquireLock(sharedLock);
//      LockManager.getInstance().checkCondition(aFile, null);
//      assertTrue("checkCondition() should fail", false);
//    } catch (Exception e) {
//      assertEquals(LockConditionRequiredException.class, e.getClass());
//    }
//  }
}
