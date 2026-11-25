package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.CustomFirebaseConfig
import com.example.model.DeskConfig
import com.example.model.MemberAccessStatus
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TeamMember
import com.example.model.TradeStats
import com.example.model.TradingSignal
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Connects dynamically at runtime to a user-pasted FirebaseConfig:
 * - If `databaseURL` is present: real-time Firebase Realtime Database via OkHttp SSE + REST,
 *   using `limitToLast=50` on `$key` to minimize free-tier Spark bandwidth.
 * - Else if `projectId` + `apiKey` + `appId` are present: secondary named FirebaseApp -> Firestore `(default)`.
 */
class CustomFirebaseRepository(private val appContext: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Required for long-lived SSE streams
        .build()

    private val restClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- REALTIME DATABASE (SSE + REST) ---

    fun observeRtdbSignals(
        databaseUrl: String,
        limit: Int = 35,
        onConnected: () -> Unit,
        onDisconnected: (String) -> Unit
    ): Flow<List<TradingSignal>> = flow {
        val baseUrl = databaseUrl.trim().trimEnd('/')
        val signalsMap = linkedMapOf<String, TradingSignal>()
        var useLimitQuery = true

        while (currentCoroutineContext().isActive) {
            try {
                val streamUrl = if (useLimitQuery) {
                    "$baseUrl/signals.json?orderBy=%22%24key%22&limitToLast=$limit"
                } else {
                    "$baseUrl/signals.json"
                }

                val request = Request.Builder()
                    .url(streamUrl)
                    .header("Accept", "text/event-stream")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 400 && useLimitQuery) {
                        useLimitQuery = false
                        throw IllegalStateException("Retrying RTDB stream without limit query")
                    }
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string()?.take(200) ?: ""
                        throw IllegalStateException("RTDB HTTP ${response.code}: $errBody")
                    }
                    onConnected()
                    val body = response.body ?: throw IllegalStateException("Empty RTDB stream")
                    val reader = BufferedReader(InputStreamReader(body.byteStream()))

                    var eventName = ""
                    val dataBuilder = StringBuilder()

                    while (currentCoroutineContext().isActive) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) {
                            if (eventName.isNotEmpty() && dataBuilder.isNotEmpty()) {
                                val rawData = dataBuilder.toString().trim()
                                when (eventName) {
                                    "put", "patch" -> {
                                        applyRtdbSseEvent(eventName, rawData, signalsMap)
                                        emit(signalsMap.values.sortedByDescending { it.effectiveTimestampMillis() })
                                    }
                                    "cancel", "auth_revoked" -> {
                                        throw IllegalStateException("RTDB stream $eventName: $rawData")
                                    }
                                }
                            }
                            eventName = ""
                            dataBuilder.clear()
                        } else if (line.startsWith("event:")) {
                            eventName = line.removePrefix("event:").trim()
                        } else if (line.startsWith("data:")) {
                            if (dataBuilder.isNotEmpty()) dataBuilder.append('\n')
                            dataBuilder.append(line.removePrefix("data:").trim())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CustomFirebaseRepo", "RTDB SSE error: ${e.message}", e)
                onDisconnected(e.localizedMessage ?: "Realtime Database connection failed")
                delay(3500)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchRtdbStats(databaseUrl: String): TradeStats? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val request = Request.Builder().url("$baseUrl/stats.json").get().build()
            restClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val raw = response.body?.string() ?: return@withContext null
                if (raw == "null" || raw.isBlank()) return@withContext null
                val obj = JSONObject(raw)
                TradeStats(
                    todayWins = obj.optLong("todayWins", 0L),
                    todayLosses = obj.optLong("todayLosses", 0L),
                    todayNetPnl = obj.optDouble("todayNetPnl", 0.0),
                    yesterdayWins = obj.optLong("yesterdayWins", 0L),
                    yesterdayLosses = obj.optLong("yesterdayLosses", 0L),
                    yesterdayNetPnl = obj.optDouble("yesterdayNetPnl", 0.0),
                    winRate = obj.optDouble("winRate", 0.0),
                    activeCount = obj.optLong("activeCount", 0L)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateRtdbSignalStatus(
        databaseUrl: String,
        signal: TradingSignal,
        newStatus: SignalStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            require(signal.id.isNotBlank()) { "Signal ID missing" }
            val newPnl = signal.calculatePnlForStatus(newStatus)
            val payload = JSONObject().apply {
                put("status", newStatus.name)
                put("pnlPercent", newPnl)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/signals/${signal.id}.json")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()

            restClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IllegalStateException("RTDB update failed: $err"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllRtdbSignals(databaseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val request = Request.Builder()
                .url("$baseUrl/signals.json")
                .delete()
                .build()
            restClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IllegalStateException("RTDB clear failed: $err"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTestSignalToRtdb(databaseUrl: String): Result<TradingSignal> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val sample = BuiltInFirestoreRepository.buildSampleTestSignal()
            val json = sample.toRtdbJson().toString()
            val request = Request.Builder()
                .url("$baseUrl/signals.json")
                .post(json.toRequestBody(jsonMediaType))
                .build()
            restClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IllegalStateException("RTDB write failed: $err"))
                }
                val respObj = JSONObject(response.body?.string() ?: "{}")
                val generatedId = respObj.optString("name", "")
                Result.success(sample.copy(id = generatedId))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- TEAM MEMBERS & DESK CONFIG (RTDB, LOW-BANDWIDTH SPARK SAFE) ---

    suspend fun syncMemberAndDeskConfig(
        databaseUrl: String,
        deviceId: String,
        memberName: String,
        isOwnerDevice: Boolean,
        appVersion: String = "1.0"
    ): Result<Pair<MemberAccessStatus, DeskConfig>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val deskConfig = fetchDeskConfigInternal(baseUrl)

            if (deviceId.isBlank()) {
                return@withContext Result.success(MemberAccessStatus.ACTIVE to deskConfig)
            }

            val memberReq = Request.Builder().url("$baseUrl/team_members/$deviceId.json").get().build()
            val existingObj = restClient.newCall(memberReq).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("Member sync HTTP ${res.code}")
                }
                val raw = res.body?.string() ?: "null"
                if (raw == "null" || raw.isBlank()) null else JSONObject(raw)
            }

            val now = System.currentTimeMillis()
            val resolvedStatus: MemberAccessStatus
            if (existingObj == null) {
                resolvedStatus = when {
                    isOwnerDevice -> MemberAccessStatus.ACTIVE
                    deskConfig.requireApproval -> MemberAccessStatus.PENDING
                    else -> MemberAccessStatus.ACTIVE
                }
                val createPayload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("name", memberName.trim().ifBlank { "Trader" }.take(32))
                    put("status", resolvedStatus.name)
                    put("joinedAt", now)
                    put("lastSeenAt", now)
                    put("appVersion", appVersion)
                }.toString()
                val putReq = Request.Builder()
                    .url("$baseUrl/team_members/$deviceId.json")
                    .put(createPayload.toRequestBody(jsonMediaType))
                    .build()
                restClient.newCall(putReq).execute().close()
            } else {
                val remoteStatus = MemberAccessStatus.fromRaw(existingObj.optString("status", "ACTIVE"))
                resolvedStatus = if (isOwnerDevice) MemberAccessStatus.ACTIVE else remoteStatus
                val lastSeen = existingObj.optLong("lastSeenAt", 0L)
                val storedName = existingObj.optString("name", "")
                val cleanName = memberName.trim().ifBlank { "Trader" }.take(32)
                val needsHeartbeat = (now - lastSeen) >= 10 * 60_000L
                val needsNameUpdate = cleanName.isNotBlank() && cleanName != "Trader" && cleanName != storedName
                val needsOwnerRestore = isOwnerDevice && remoteStatus != MemberAccessStatus.ACTIVE

                if (needsHeartbeat || needsNameUpdate || needsOwnerRestore) {
                    val patchObj = JSONObject().apply {
                        if (needsHeartbeat) put("lastSeenAt", now)
                        if (needsNameUpdate) put("name", cleanName)
                        if (needsOwnerRestore) put("status", MemberAccessStatus.ACTIVE.name)
                    }.toString()
                    val patchReq = Request.Builder()
                        .url("$baseUrl/team_members/$deviceId.json")
                        .patch(patchObj.toRequestBody(jsonMediaType))
                        .build()
                    restClient.newCall(patchReq).execute().close()
                }
            }
            Result.success(resolvedStatus to deskConfig)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchDeskConfigInternal(baseUrl: String): DeskConfig {
        return try {
            val req = Request.Builder().url("$baseUrl/desk_config.json").get().build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return DeskConfig()
                val raw = res.body?.string() ?: return DeskConfig()
                if (raw == "null" || raw.isBlank()) return DeskConfig()
                val obj = JSONObject(raw)
                DeskConfig(
                    announcement = obj.optString("announcement", "").trim(),
                    requireApproval = obj.optBoolean("requireApproval", false),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            }
        } catch (_: Exception) {
            DeskConfig()
        }
    }

    suspend fun fetchAllTeamMembers(databaseUrl: String): Result<List<TeamMember>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val req = Request.Builder().url("$baseUrl/team_members.json").get().build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${res.code}"))
                }
                val raw = res.body?.string() ?: "null"
                if (raw == "null" || raw.isBlank()) return@withContext Result.success(emptyList())
                val root = JSONObject(raw)
                val list = mutableListOf<TeamMember>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = root.optJSONObject(key) ?: continue
                    list.add(
                        TeamMember(
                            deviceId = item.optString("deviceId", key),
                            name = item.optString("name", "Trader"),
                            status = item.optString("status", MemberAccessStatus.ACTIVE.name),
                            joinedAt = item.optLong("joinedAt", 0L),
                            lastSeenAt = item.optLong("lastSeenAt", 0L),
                            appVersion = item.optString("appVersion", "1.0")
                        )
                    )
                }
                Result.success(list.sortedByDescending { it.lastSeenAt })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberStatus(
        databaseUrl: String,
        deviceId: String,
        newStatus: MemberAccessStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("status", newStatus.name)
            }.toString()
            val req = Request.Builder()
                .url("$baseUrl/team_members/$deviceId.json")
                .patch(payload.toRequestBody(jsonMediaType))
                .build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Update member HTTP ${res.code}"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTeamMember(
        databaseUrl: String,
        deviceId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val req = Request.Builder()
                .url("$baseUrl/team_members/$deviceId.json")
                .delete()
                .build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Remove member HTTP ${res.code}"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeskConfig(
        databaseUrl: String,
        config: DeskConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("announcement", config.announcement.trim().take(240))
                put("requireApproval", config.requireApproval)
                put("updatedAt", System.currentTimeMillis())
            }.toString()
            val req = Request.Builder()
                .url("$baseUrl/desk_config.json")
                .put(payload.toRequestBody(jsonMediaType))
                .build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Desk config HTTP ${res.code}"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeStaleOpenSignals(
        databaseUrl: String,
        signals: List<TradingSignal>,
        maxAgeMillis: Long = 24L * 3600_000L
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val cutoff = System.currentTimeMillis() - maxAgeMillis
            val stale = signals.filter {
                it.id.isNotBlank() && it.parsedStatus.isOpen && it.effectiveTimestampMillis() < cutoff
            }
            if (stale.isEmpty()) return@withContext Result.success(0)

            val multiPatch = JSONObject()
            stale.forEach { sig ->
                multiPatch.put("${sig.id}/status", SignalStatus.CLOSED.name)
                if (sig.parsedStatus == SignalStatus.TP1_HIT && sig.pnlPercent == 0.0) {
                    multiPatch.put("${sig.id}/pnlPercent", sig.calculatePnlForStatus(SignalStatus.TP1_HIT))
                }
            }
            val req = Request.Builder()
                .url("$baseUrl/signals.json")
                .patch(multiPatch.toString().toRequestBody(jsonMediaType))
                .build()
            restClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Close stale HTTP ${res.code}"))
                }
            }
            Result.success(stale.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pruneOldSignals(
        databaseUrl: String,
        keepLatestCount: Int = 35
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = databaseUrl.trim().trimEnd('/')
            val shallowReq = Request.Builder().url("$baseUrl/signals.json?shallow=true").get().build()
            val allKeys = restClient.newCall(shallowReq).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Prune keys HTTP ${res.code}"))
                }
                val raw = res.body?.string() ?: "null"
                if (raw == "null" || raw.isBlank()) return@withContext Result.success(0)
                val obj = JSONObject(raw)
                val list = mutableListOf<String>()
                val it = obj.keys()
                while (it.hasNext()) list.add(it.next())
                list.sorted()
            }

            if (allKeys.size <= keepLatestCount) return@withContext Result.success(0)
            val toDelete = allKeys.dropLast(keepLatestCount)
            val multiDeletePatch = JSONObject()
            toDelete.forEach { key ->
                multiDeletePatch.put(key, JSONObject.NULL)
            }
            val patchReq = Request.Builder()
                .url("$baseUrl/signals.json")
                .patch(multiDeletePatch.toString().toRequestBody(jsonMediaType))
                .build()
            restClient.newCall(patchReq).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Prune delete HTTP ${res.code}"))
                }
            }
            Result.success(toDelete.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyRtdbSseEvent(
        eventName: String,
        rawJson: String,
        signalsMap: MutableMap<String, TradingSignal>
    ) {
        val envelope = JSONObject(rawJson)
        val path = envelope.optString("path", "/")
        val data = envelope.opt("data")

        if (path == "/") {
            if (eventName == "put") {
                signalsMap.clear()
            }
            if (data is JSONObject) {
                val keys = data.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val itemObj = data.optJSONObject(key)
                    if (itemObj != null) {
                        signalsMap[key] = parseSignalFromJson(key, itemObj)
                    } else if (data.isNull(key)) {
                        signalsMap.remove(key)
                    }
                }
            }
        } else {
            val segments = path.trimStart('/').split('/')
            val signalId = segments.firstOrNull().orEmpty()
            if (signalId.isBlank()) return

            if (segments.size == 1) {
                if (data == null || data == JSONObject.NULL) {
                    signalsMap.remove(signalId)
                } else if (data is JSONObject) {
                    val existing = signalsMap[signalId]
                    signalsMap[signalId] = if (eventName == "patch" && existing != null) {
                        mergeSignalJson(existing, data)
                    } else {
                        parseSignalFromJson(signalId, data)
                    }
                }
            } else if (segments.size == 2) {
                val field = segments[1]
                val existing = signalsMap[signalId] ?: return
                signalsMap[signalId] = when (field) {
                    "status" -> existing.copy(status = data?.toString() ?: existing.status)
                    "pnlPercent" -> existing.copy(pnlPercent = (data as? Number)?.toDouble() ?: existing.pnlPercent)
                    else -> existing
                }
            }
        }
    }

    private fun parseSignalFromJson(id: String, obj: JSONObject): TradingSignal {
        return TradingSignal(
            id = obj.optString("id", "").ifBlank { id },
            symbol = obj.optString("symbol", "XAUUSD"),
            timeframe = obj.optString("timeframe", "M15"),
            side = SignalSide.fromRaw(obj.optString("side", "BUY")).name,
            strategy = obj.optString("strategy", "Price Action"),
            entry = obj.optDouble("entry", 0.0),
            sl = obj.optDouble("sl", 0.0),
            tp1 = obj.optDouble("tp1", 0.0),
            tp2 = obj.optDouble("tp2", 0.0),
            tp3 = obj.optDouble("tp3", 0.0),
            rr = obj.optString("rr", "1:2.0"),
            lotSize = obj.optString("lotSize", ""),
            status = SignalStatus.fromRaw(obj.optString("status", "ACTIVE")).name,
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            pnlPercent = obj.optDouble("pnlPercent", 0.0),
            notes = obj.optString("notes", "").takeIf { it.isNotBlank() }
        )
    }

    private fun mergeSignalJson(existing: TradingSignal, patch: JSONObject): TradingSignal {
        return existing.copy(
            symbol = if (patch.has("symbol")) patch.optString("symbol", existing.symbol) else existing.symbol,
            timeframe = if (patch.has("timeframe")) patch.optString("timeframe", existing.timeframe) else existing.timeframe,
            side = if (patch.has("side")) SignalSide.fromRaw(patch.optString("side")).name else existing.side,
            strategy = if (patch.has("strategy")) patch.optString("strategy", existing.strategy) else existing.strategy,
            entry = if (patch.has("entry")) patch.optDouble("entry", existing.entry) else existing.entry,
            sl = if (patch.has("sl")) patch.optDouble("sl", existing.sl) else existing.sl,
            tp1 = if (patch.has("tp1")) patch.optDouble("tp1", existing.tp1) else existing.tp1,
            tp2 = if (patch.has("tp2")) patch.optDouble("tp2", existing.tp2) else existing.tp2,
            tp3 = if (patch.has("tp3")) patch.optDouble("tp3", existing.tp3) else existing.tp3,
            rr = if (patch.has("rr")) patch.optString("rr", existing.rr) else existing.rr,
            lotSize = if (patch.has("lotSize")) patch.optString("lotSize", existing.lotSize) else existing.lotSize,
            status = if (patch.has("status")) SignalStatus.fromRaw(patch.optString("status")).name else existing.status,
            timestamp = if (patch.has("timestamp")) patch.optLong("timestamp", existing.timestamp) else existing.timestamp,
            pnlPercent = if (patch.has("pnlPercent")) patch.optDouble("pnlPercent", existing.pnlPercent) else existing.pnlPercent
        )
    }

    // --- CUSTOM EXTERNAL FIRESTORE ---

    private fun getOrCreateCustomFirestore(config: CustomFirebaseConfig): FirebaseFirestore {
        val appName = "custom_signaldesk_${config.projectId}_${config.appId.takeLast(6)}"
        val existingApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == appName }
        val app = existingApp ?: FirebaseApp.initializeApp(
            appContext,
            FirebaseOptions.Builder()
                .setApiKey(config.apiKey)
                .setApplicationId(config.appId)
                .setProjectId(config.projectId)
                .apply {
                    if (config.databaseURL.isNotBlank()) {
                        setDatabaseUrl(config.databaseURL)
                    }
                }
                .build(),
            appName
        )
        return FirebaseFirestore.getInstance(app, "(default)")
    }

    fun observeCustomFirestoreSignals(config: CustomFirebaseConfig): Flow<List<TradingSignal>> {
        val db = getOrCreateCustomFirestore(config)
        return db.collection("signals")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TradingSignal::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.effectiveTimestampMillis() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                throw (firestoreError ?: error)
            }
    }

    suspend fun updateCustomFirestoreStatus(
        config: CustomFirebaseConfig,
        signal: TradingSignal,
        newStatus: SignalStatus
    ): Result<Unit> {
        return try {
            val db = getOrCreateCustomFirestore(config)
            require(signal.id.isNotBlank()) { "Signal ID missing" }
            val newPnl = signal.calculatePnlForStatus(newStatus)
            db.collection("signals").document(signal.id)
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "pnlPercent" to newPnl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTestSignalToCustomFirestore(config: CustomFirebaseConfig): Result<TradingSignal> {
        return try {
            val db = getOrCreateCustomFirestore(config)
            val docId = "sig_test_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val sample = BuiltInFirestoreRepository.buildSampleTestSignal()
            val payload = mapOf(
                "symbol" to sample.symbol,
                "timeframe" to sample.timeframe,
                "side" to sample.side,
                "strategy" to sample.strategy,
                "entry" to sample.entry,
                "sl" to sample.sl,
                "tp1" to sample.tp1,
                "tp2" to sample.tp2,
                "rr" to sample.rr,
                "status" to sample.status,
                "timestamp" to sample.timestamp,
                "pnlPercent" to sample.pnlPercent,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("signals").document(docId).set(payload).await()
            Result.success(sample.copy(id = docId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
