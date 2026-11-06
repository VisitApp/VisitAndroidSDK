package com.getvisitapp.google_fit.healthConnect.data

import com.getvisitapp.google_fit.HealthConnectListener
import com.getvisitapp.google_fit.healthConnect.activity.HealthConnectUtil
import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
import kotlin.coroutines.Continuation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class PendingGraphRequestsTest {
    private val dashboard = GraphKey("dashboard")
    private fun TestScope.newRequests(timeoutMillis: Long = 30_000) = PendingGraphRequests(
        CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job])),
        timeoutMillis,
    )

    @Test fun duplicatesShareWorkButCompletedResultsAreNotCached() = runTest {
        val requests = newRequests()
        val data = CompletableDeferred<String>()
        var reads = 0
        val callers = List(10) { async { requests.read(dashboard) { reads++; data.await() } } }
        runCurrent()
        assertEquals(1, reads)
        data.complete("today"); runCurrent()
        callers.forEach { assertEquals("today", it.await()) }
        val next = async { requests.read(dashboard) { reads++; "fresh" } }
        runCurrent()
        assertEquals("fresh", next.await())
        assertEquals(2, reads)
    }

    @Test fun independentKeysKeepReversedResultsSeparate() = runTest {
        val requests = newRequests()
        val keys = listOf(dashboard, GraphKey("steps", "day", 1), GraphKey("sleep", "week", 1),
            GraphKey("steps", "week", 1), GraphKey("steps", "day", 2))
        val data = keys.associateWith { CompletableDeferred<String>() }
        val callers = keys.associateWith { key -> async { requests.read(key) { data.getValue(key).await() } } }
        runCurrent()
        keys.reversed().forEach { key ->
            data.getValue(key).complete(key.toString()); runCurrent()
            assertEquals(key.toString(), callers.getValue(key).await())
        }
    }

    @Test fun cancellingOneCallerDoesNotCancelTheSharedQuery() = runTest {
        val requests = newRequests()
        val data = CompletableDeferred<String>()
        var reads = 0
        val a = async { requests.read(dashboard) { reads++; data.await() } }
        val b = async { requests.read(dashboard) { reads++; data.await() } }
        runCurrent(); a.cancel(); runCurrent()
        data.complete("still active"); runCurrent()
        assertEquals("still active", b.await())
        assertEquals(1, reads)
    }

    @Test fun queryErrorsReachAllWaitersAndDoNotPoisonOtherKeysOrRetries() = runTest {
        val requests = newRequests()
        val gate = CompletableDeferred<String>()
        val callers = List(2) { async { runCatching { requests.read(dashboard) { gate.await() } } } }
        val other = async { requests.read(GraphKey("steps", "day", 0)) { "other" } }
        runCurrent(); gate.completeExceptionally(IllegalStateException("provider failed")); runCurrent()
        callers.forEach { assertIs<IllegalStateException>(it.await().exceptionOrNull()) }
        assertEquals("other", other.await())
        val retry = async { requests.read(dashboard) { "retry" } }; runCurrent()
        assertEquals("retry", retry.await())
    }

    @Test fun timeoutCompletesAllCallersAndAllowsFreshWork() = runTest {
        val requests = newRequests()
        val callers = List(2) { async { runCatching { requests.read(dashboard) { delay(60_000); "late" } } } }
        runCurrent(); advanceTimeBy(30_001); runCurrent()
        callers.forEach { assertIs<TimeoutCancellationException>(it.await().exceptionOrNull()) }
        val retry = async { requests.read(dashboard) { "retry" } }; runCurrent()
        assertEquals("retry", retry.await())
    }

    @Test fun lateUncooperativeQueryCannotRemoveOrResolveReplacement() = runTest {
        val requests = newRequests(100)
        val oldData = CompletableDeferred<String>()
        val old = async { runCatching { requests.read(dashboard) { withContext(NonCancellable) { oldData.await() } } } }
        runCurrent(); advanceTimeBy(101); runCurrent()
        assertIs<TimeoutCancellationException>(old.await().exceptionOrNull())
        var reads = 0
        val newData = CompletableDeferred<String>()
        val fresh = async { requests.read(dashboard) { reads++; newData.await() } }; runCurrent()
        oldData.complete("obsolete"); runCurrent()
        val duplicate = async { requests.read(dashboard) { reads++; "incorrect" } }; runCurrent()
        assertEquals(1, reads)
        newData.complete("fresh"); runCurrent()
        assertEquals("fresh", fresh.await())
        assertEquals("fresh", duplicate.await())
    }

    @Test fun explicitCancellationSettlesAllKeysAndIgnoresOldCompletions() = runTest {
        val requests = newRequests()
        val oldData = CompletableDeferred<String>()
        val old = async { runCatching { requests.read(dashboard) { withContext(NonCancellable) { oldData.await() } } } }
        val other = async { runCatching { requests.read(GraphKey("steps", "day", 0)) { delay(60_000); "late" } } }
        runCurrent(); requests.cancel(); runCurrent()
        assertIs<CancellationException>(old.await().exceptionOrNull())
        assertIs<CancellationException>(other.await().exceptionOrNull())
        val fresh = async { requests.read(dashboard) { "fresh" } }
        oldData.complete("obsolete"); runCurrent()
        assertEquals("fresh", fresh.await())
    }

    private class Listener : HealthConnectListener {
        val values = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        override fun loadVisitWebViewGraphData(webUrl: String) { values.add(webUrl) }
        override fun logHealthConnectError(throwable: Throwable) { errors.add(throwable) }
        override fun updateHealthConnectConnectionStatus(status: HealthConnectConnectionState, text: String) = Unit
        override fun userDeniedHealthConnectPermission() = Unit
        override fun userAcceptedHealthConnectPermission() = Unit
        override fun requestPermission() = Unit
    }

    @Test fun legacyListenersStillReceiveOneResponsePerInvocation() = runTest {
        val requests = newRequests()
        val listener = Listener()
        val data = CompletableDeferred<String>()
        var reads = 0
        val callers = List(2) { async {
            deliverGraphResult(listener) { requests.read(dashboard) { reads++; data.await() } }
        } }
        runCurrent(); data.complete("window.updateFitnessPermissions(true,0,0)"); runCurrent()
        callers.forEach { it.await() }
        assertEquals(1, reads)
        assertEquals(listOf("window.updateFitnessPermissions(true,0,0)", "window.updateFitnessPermissions(true,0,0)"), listener.values)
        deliverGraphResult(listener) { error("query failed") }
        assertEquals(1, listener.errors.size)
    }

    @Test fun nativePublicApiRetainsLegacyAndAddsRequestScopedMethods() {
        val type = HealthConnectUtil::class.java
        assertEquals(Void.TYPE, type.getMethod("getVisitDashboardGraph").returnType)
        assertEquals(Void.TYPE, type.getMethod("getActivityData", String::class.java, String::class.java, Long::class.javaPrimitiveType).returnType)
        assertEquals(Any::class.java, type.getMethod("getDashboardData", Continuation::class.java).returnType)
        assertEquals(Any::class.java, type.getMethod("getGraphData", String::class.java, String::class.java, Long::class.javaPrimitiveType, Continuation::class.java).returnType)
        assertEquals(Void.TYPE, type.getMethod("cancelPendingGraphRequests").returnType)
        assertTrue(type.constructors.any { it.parameterTypes.map { p -> p.simpleName } == listOf("Context", "HealthConnectListener") })
    }
}
