package com.getvisitapp.google_fit.healthConnect.data

import com.getvisitapp.google_fit.HealthConnectListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal data class GraphKey(val type: String, val frequency: String = "", val timestamp: Long = 0)

/** Shares pending reads, never completed values. Each caller awaits its own return. */
internal class PendingGraphRequests(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = 30_000,
) {
    private val pending = mutableMapOf<GraphKey, Deferred<String>>()

    suspend fun read(key: GraphKey, query: suspend () -> String): String {
        val operation = synchronized(pending) {
            pending[key]?.takeUnless { it.isCompleted } ?: scope.async(start = CoroutineStart.LAZY) {
                // A sibling query cannot hold callers past the deadline while
                // a provider that ignores cancellation finishes unwinding.
                val worker = scope.async { query() }
                try {
                    withTimeout(timeoutMillis) { worker.await() }
                } finally {
                    worker.cancel()
                }
            }.also { operation ->
                pending[key] = operation
                operation.invokeOnCompletion {
                    synchronized(pending) {
                        if (pending[key] === operation) pending.remove(key)
                    }
                }
            }
        }
        operation.start()
        return operation.await()
    }

    fun cancel() {
        val operations = synchronized(pending) {
            pending.values.toList().also { pending.clear() }
        }
        operations.forEach { it.cancel() }
    }
}

/** Preserves the legacy listener contract without swallowing query failures. */
internal suspend fun deliverGraphResult(listener: HealthConnectListener, read: suspend () -> String) {
    try {
        val data = read()
        listener.loadVisitWebViewGraphData(data)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        listener.logHealthConnectError(error)
    }
}
