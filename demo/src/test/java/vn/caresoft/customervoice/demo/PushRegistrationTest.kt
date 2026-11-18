package vn.caresoft.customervoice.demo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrationTest {
 @Test fun resumeReconcilesAfterTransientRegistrationFailure()=runTest {
  var attempts=0; val registered=mutableListOf<String>()
  val push=PushRegistration(this,{"session"},{"token"}) {
   attempts++; if(attempts==1) throw java.io.IOException("offline")
   registered.add(it)
  }
  push.reconcile(); runCurrent()
  assertTrue(registered.isEmpty())
  push.reconcile(); runCurrent()
  assertEquals(listOf("token"),registered)
 }
 @Test fun lateFirebaseResultCannotRegisterIntoReplacementSession()=runTest {
  var session="one"; val fetched=CompletableDeferred<String>(); val registered=mutableListOf<String>()
  val push=PushRegistration(this,{session},{fetched.await()}) { registered.add(it) }
  push.reconcile(); runCurrent(); session="two"; fetched.complete("old-token"); runCurrent()
  assertTrue(registered.isEmpty())
 }
}
