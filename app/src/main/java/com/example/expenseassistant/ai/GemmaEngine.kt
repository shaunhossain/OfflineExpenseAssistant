package com.example.expenseassistant.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface EngineState {
    data object Uninitialized : EngineState
    data class PreparingModel(val progressPercent: Int) : EngineState
    data object LoadingEngine : EngineState
    data object Ready : EngineState
    data class Error(val message: String) : EngineState
}

class GemmaEngine private constructor() {

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var turnCount: Int = 0
    private val mutex = Mutex()

    /**
     * Recreates the LiteRT-LM [Conversation] with a fresh KV cache and system prompt.
     * Because Gemma 3 270M has a compact context window and every user prompt is
     * self-contained (with its own Banglish semantic bridge or financial snapshot),
     * resetting the conversation periodically or after an error prevents KV-cache
     * saturation and multi-turn attention drift.
     */
    fun resetConversation() {
        val currentEngine = engine ?: return
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous conversation during reset", e)
        }
        try {
            val convConfig = ConversationConfig(
                systemInstruction = Contents.of(PromptBuilder.buildSystemPrompt())
            )
            conversation = currentEngine.createConversation(convConfig)
            turnCount = 0
            Log.d(TAG, "LiteRT-LM Conversation KV cache reset successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate LiteRT-LM Conversation", e)
        }
    }

    suspend fun initialize(context: Context) {
        mutex.withLock {
            if (_state.value is EngineState.Ready) return

            withContext(Dispatchers.IO) {
                try {
                    _state.value = EngineState.PreparingModel(0)
                    val modelFile = ensureModelFileExists(context) { progress ->
                        _state.value = EngineState.PreparingModel(progress)
                    }

                    _state.value = EngineState.LoadingEngine
                    Log.i(TAG, "Initializing LiteRT-LM Engine with model: ${modelFile.absolutePath}")

                    val config = EngineConfig(modelPath = modelFile.absolutePath)
                    val newEngine = Engine(config)
                    newEngine.initialize()

                    val convConfig = ConversationConfig(
                        systemInstruction = Contents.of(PromptBuilder.buildSystemPrompt())
                    )
                    val newConversation = newEngine.createConversation(convConfig)

                    engine = newEngine
                    conversation = newConversation
                    _state.value = EngineState.Ready
                    Log.i(TAG, "LiteRT-LM Engine initialized successfully.")
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to initialize GemmaEngine", e)
                    _state.value = EngineState.Error(e.message ?: "Unknown initialization failure")
                }
            }
        }
    }

    internal var testResponseEmitter: ((String) -> Flow<String>)? = null

    internal fun setStateForTesting(newState: EngineState) {
        _state.value = newState
    }

    suspend fun generateResponse(prompt: String): Flow<String> {
        testResponseEmitter?.let { return it(prompt) }
        return flow {
            if (turnCount >= MAX_TURNS_PER_CONVERSATION) {
                resetConversation()
            }
            val currentConversation = conversation
            if (currentConversation == null) {
                emit("Error: Gemma engine is not ready.")
                return@flow
            }

            val fullResponse = StringBuilder()
            try {
                turnCount++
                currentConversation.sendMessageAsync(prompt).collect { message ->
                    val chunk = message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    if (chunk.isNotEmpty()) {
                        fullResponse.append(chunk)
                        emit(chunk)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during inference; resetting conversation KV cache", e)
                resetConversation()
                emit("Error: ${e.localizedMessage}")
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun ensureModelFileExists(
        context: Context,
        onProgress: (Int) -> Unit
    ): File {
        val filename = resolveModelFilename(context)
        val targetFile = File(context.filesDir, filename)
        if (targetFile.exists() && targetFile.length() > 10_000_000L) {
            onProgress(100)
            return targetFile
        }

        Log.i(TAG, "Extracting model '$filename' from assets to ${targetFile.absolutePath}...")
        context.assets.open(filename).use { input ->
            val totalBytes = input.available().takeIf { it > 0 }?.toLong() ?: (304L * 1024 * 1024)
            var copiedBytes = 0L

            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var lastReported = 0

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    val percent = ((copiedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                    if (percent != lastReported) {
                        lastReported = percent
                        onProgress(percent)
                    }
                }
                output.flush()
            }
        }

        onProgress(100)
        Log.i(TAG, "Model extraction completed: ${targetFile.length()} bytes.")
        return targetFile
    }

    fun release() {
        try {
            conversation?.close()
            conversation = null
            turnCount = 0
            engine?.close()
            engine = null
            _state.value = EngineState.Uninitialized
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GemmaEngine", e)
        }
    }

    companion object {
        private const val TAG = "GemmaEngine"
        private const val MAX_TURNS_PER_CONVERSATION = 4
        const val DEFAULT_MODEL_FILENAME = "gemma3-270m-it-q8.litertlm"
        var MODEL_FILENAME = DEFAULT_MODEL_FILENAME

        fun resolveModelFilename(context: Context): String {
            val assetsList = try { context.assets.list("")?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
            val gemma3 = assetsList.firstOrNull { it.contains("gemma3", ignoreCase = true) }
            if (gemma3 != null) {
                MODEL_FILENAME = gemma3
                return gemma3
            }
            val anyGemma = assetsList.firstOrNull { it.contains("gemma", ignoreCase = true) }
            if (anyGemma != null) {
                MODEL_FILENAME = anyGemma
                return anyGemma
            }
            val anyModel = assetsList.firstOrNull { it.endsWith(".litertlm") || it.endsWith(".task") }
            if (anyModel != null) {
                MODEL_FILENAME = anyModel
                return anyModel
            }
            MODEL_FILENAME = DEFAULT_MODEL_FILENAME
            return DEFAULT_MODEL_FILENAME
        }

        @Volatile
        private var INSTANCE: GemmaEngine? = null

        fun getInstance(): GemmaEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GemmaEngine().also { INSTANCE = it }
            }
        }
    }
}
