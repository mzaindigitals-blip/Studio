package com.example.data

import android.content.Context
import com.example.R
import com.example.model.SignalSide
import com.example.model.SignalStatus
import com.example.model.TradeStats
import com.example.model.TradingSignal
import com.example.model.calculateTradeStats
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BuiltInFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeSignals(userId: String): Flow<List<TradingSignal>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user query." }
        val path = "users/$uid/signals"
        emitAll(
            db.collection("users").document(uid).collection("signals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TradingSignal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.isMeaningfulSignal() }
                        .sortedByDescending { it.effectiveTimestampMillis() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStats(userId: String): Flow<TradeStats?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user query." }
        val path = "users/$uid/stats/summary"
        emitAll(
            db.collection("users").document(uid).collection("stats").document("summary")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            TradeStats::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateSignalStatus(
        signal: TradingSignal,
        newStatus: SignalStatus
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/signals/${signal.id}"
        return try {
            val uid = requireUserId()
            require(signal.id.isNotBlank()) { "Signal ID must not be blank for update" }
            val docRef = db.collection("users").document(uid).collection("signals").document(signal.id)
            docPath = docRef.path
            val newPnl = signal.calculatePnlForStatus(newStatus)
            docRef.update(
                mapOf(
                    "status" to newStatus.name,
                    "pnlPercent" to newPnl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendTestSignal(): Result<TradingSignal> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/signals"
        return try {
            val uid = requireUserId()
            val docId = "sig_test_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val docRef = db.collection("users").document(uid).collection("signals").document(docId)
            docPath = docRef.path
            val sample = buildSampleTestSignal(uid)
            docRef.set(sample.toFirestoreCreateMap(uid)).await()
            Result.success(sample.copy(id = docId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncComputedStats(allSignals: List<TradingSignal>): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/stats/summary"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("stats").document("summary")
            docPath = docRef.path
            val stats = calculateTradeStats(allSignals)
            docRef.set(stats.toFirestoreMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        fun buildSampleTestSignal(uid: String = ""): TradingSignal {
            val templates = listOf(
                TradingSignal(
                    userId = uid,
                    symbol = "XAUUSD",
                    timeframe = "M15",
                    side = SignalSide.BUY.name,
                    strategy = "3-Module PA",
                    entry = 2648.40,
                    sl = 2640.00,
                    tp1 = 2658.40,
                    tp2 = 2670.40,
                    rr = "1:2.6",
                    status = SignalStatus.ACTIVE.name,
                    timestamp = System.currentTimeMillis(),
                    pnlPercent = 0.0
                ),
                TradingSignal(
                    userId = uid,
                    symbol = "BTCUSDT",
                    timeframe = "M5",
                    side = SignalSide.SELL.name,
                    strategy = "Bollinger Bands",
                    entry = 91850.00,
                    sl = 92400.00,
                    tp1 = 91100.00,
                    tp2 = 90350.00,
                    rr = "1:2.7",
                    status = SignalStatus.ACTIVE.name,
                    timestamp = System.currentTimeMillis(),
                    pnlPercent = 0.0
                ),
                TradingSignal(
                    userId = uid,
                    symbol = "BNBUSDT",
                    timeframe = "H1",
                    side = SignalSide.BUY.name,
                    strategy = "RSI+EMA",
                    entry = 624.50,
                    sl = 617.00,
                    tp1 = 634.50,
                    tp2 = 644.50,
                    rr = "1:2.7",
                    status = SignalStatus.ACTIVE.name,
                    timestamp = System.currentTimeMillis(),
                    pnlPercent = 0.0
                )
            )
            return templates.random()
        }
    }
}
