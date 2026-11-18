package vn.caresoft.customervoice.demo

import kotlinx.coroutines.*

/** Idempotent push registration is retried on resume, with late Firebase results fenced by session. */
internal class PushRegistration(private val scope: CoroutineScope,private val session: ()->String?,
 private val token: suspend ()->String,private val register: suspend (String)->Unit) {
 private var job: Job?=null
 private var registering: String?=null
 fun reconcile() {
  val captured=session() ?: return
  if(job?.isActive==true && registering==captured) return
  job?.cancel(); registering=captured
  job=scope.launch {
   try { val value=token(); if(session()==captured) register(value) }
   catch(e: CancellationException) { throw e }
   catch(_: Exception) { /* The next resume/session observation retries this safe PUT. */ }
  }
 }
}
