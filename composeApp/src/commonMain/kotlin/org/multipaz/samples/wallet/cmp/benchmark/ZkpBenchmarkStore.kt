package org.multipaz.samples.wallet.cmp.benchmark

import org.multipaz.cbor.Cbor
import org.multipaz.eventlogger.EventPresentmentDigitalCredentialsMdocApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.multipaz.eventlogger.Event
import org.multipaz.eventlogger.EventPresentment
import org.multipaz.eventlogger.EventPresentmentDigitalCredentialsOpenID4VP
import org.multipaz.eventlogger.EventPresentmentUriSchemeOpenID4VP
import org.multipaz.util.Logger
import kotlin.time.ExperimentalTime

/** Tag used for structured benchmark lines in Activity → View app logs. */
const val ZKP_BENCHMARK_LOG_TAG = "ZKP-Benchmark"

private const val MAX_SNAPSHOTS = 64
private const val CORRELATION_WINDOW_MS = 120_000L
data class MdocDeviceResponseSnapshot(
    val responseBytes: Long,
    val completedAtEpochMs: Long,
)

object ZkpBenchmarkStore {
    private val lock = Any()
    private val snapshots = ArrayDeque<ZkpBenchmarkSnapshot>()
    private val _lastProofGeneration = MutableStateFlow<ZkpBenchmarkSnapshot?>(null)
    private val _lastVpTokenPayload = MutableStateFlow<VpTokenPayloadSnapshot?>(null)
    private val _lastMdocDeviceResponse =
        MutableStateFlow<MdocDeviceResponseSnapshot?>(null)
    /** Most recently completed ZKP proof generation (updated after each presentation). */
    val lastProofGeneration: StateFlow<ZkpBenchmarkSnapshot?> = _lastProofGeneration.asStateFlow()

    /** Most recently measured VP token payload size (OpenID4VP presentations only). */
    val lastVpTokenPayload: StateFlow<VpTokenPayloadSnapshot?> = _lastVpTokenPayload.asStateFlow()
    /** Most recently measured mdoc device response size. */
    val lastMdocDeviceResponse: StateFlow<MdocDeviceResponseSnapshot?> =
        _lastMdocDeviceResponse.asStateFlow()
    fun record(snapshot: ZkpBenchmarkSnapshot) {
        synchronized(lock) {
            snapshots.addLast(snapshot)
            while (snapshots.size > MAX_SNAPSHOTS) {
                snapshots.removeFirst()
            }
        }
        _lastProofGeneration.value = snapshot
        Logger.i(ZKP_BENCHMARK_LOG_TAG, snapshot.toLogLine())
    }
    fun recordMdocDeviceResponse(
        responseBytes: Long,
        completedAtEpochMs: Long,
    ) {
        val snapshot = MdocDeviceResponseSnapshot(
            responseBytes = responseBytes,
            completedAtEpochMs = completedAtEpochMs,
        )

        _lastMdocDeviceResponse.value = snapshot

        Logger.i(
            ZKP_BENCHMARK_LOG_TAG,
            "operation=mdoc device response (presentation) " +
                "instrument=CBOR.encode(deviceResponse).size " +
                "mdoc_device_response_bytes=$responseBytes " +
                "(${formatMdocDeviceResponseSize(responseBytes)})",
        )
    }

    fun recordVpTokenPayload(payloadBytes: Long, completedAtEpochMs: Long) {
        val snapshot = VpTokenPayloadSnapshot(
            payloadBytes = payloadBytes,
            completedAtEpochMs = completedAtEpochMs,
        )
        _lastVpTokenPayload.value = snapshot
        Logger.i(
            ZKP_BENCHMARK_LOG_TAG,
            "operation=VP token payload (presentation) instrument=len(base64url.decode(vp_token)) " +
                "vp_token_payload_bytes=$payloadBytes (${formatVpTokenPayloadSize(payloadBytes)})",
        )
    }
    @OptIn(ExperimentalTime::class)
    fun snapshotsForPresentation(event: Event): List<ZkpBenchmarkSnapshot> {
        if (event !is EventPresentment) {
            return emptyList()
        }
        val eventMs = event.timestamp.toEpochMilliseconds()
        return synchronized(lock) {
            snapshots.filter { snap ->
                kotlin.math.abs(snap.completedAtEpochMs - eventMs) <= CORRELATION_WINDOW_MS
            }
        }
    }
}
@OptIn(ExperimentalTime::class)
fun logVpTokenPayloadForEvent(event: Event) {
    Logger.i(
        ZKP_BENCHMARK_LOG_TAG,
        "VP TOKEN EVENT RECEIVED: ${event::class.simpleName}"
    )
    when (event) {
        is EventPresentmentUriSchemeOpenID4VP -> {
            logVpTokenJsonStructure(event.vpToken)
        }

        is EventPresentmentDigitalCredentialsOpenID4VP -> {
            logVpTokenJsonStructure(event.vpToken)
        }

        else -> Unit
    }

    val bytes = vpTokenPayloadBytesForEvent(event)

    if (bytes == null) {
        Logger.w(
            ZKP_BENCHMARK_LOG_TAG,
            "VP TOKEN MEASUREMENT SKIPPED: unsupported event or payload parsing failed"
        )
        return
    }
    val completedAtMs = if (event is EventPresentment) {
        event.timestamp.toEpochMilliseconds()
    } else {
        kotlin.time.Clock.System.now().toEpochMilliseconds()
    }
    ZkpBenchmarkStore.recordVpTokenPayload(bytes, completedAtMs)
}

fun vpTokenPayloadBytesForEvent(event: Event): Long? = when (event) {
    is EventPresentmentUriSchemeOpenID4VP -> measureVpTokenPayloadBytes(event.vpToken)
    is EventPresentmentDigitalCredentialsOpenID4VP -> measureVpTokenPayloadBytes(event.vpToken)
    else -> null
}

fun formatMdocDeviceResponseSize(bytes: Long): String {
    return if (bytes < 1024) {
        "$bytes B"
    } else {
        "%.2f KB".format(bytes / 1024.0)
    }
}
@OptIn(ExperimentalTime::class)
fun logMdocDeviceResponseSize(event: Event) {
    if (event !is EventPresentmentDigitalCredentialsMdocApi) {
        return
    }

    val bytes = runCatching {
        Cbor.encode(event.deviceResponse).size.toLong()
    }.getOrNull()

    if (bytes == null) {
        Logger.w(
            ZKP_BENCHMARK_LOG_TAG,
            "MDOC DEVICE RESPONSE MEASUREMENT FAILED"
        )
        return
    }

    ZkpBenchmarkStore.recordMdocDeviceResponse(
        responseBytes = bytes,
        completedAtEpochMs = event.timestamp.toEpochMilliseconds(),
    )
}
