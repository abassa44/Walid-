package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.StudyAppointment
import com.example.data.StudyRepository
import com.example.data.StudySession
import com.example.data.StudyTask
import com.example.data.network.GeminiContent
import com.example.data.network.GeminiPart
import com.example.data.network.GeminiRequest
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository

    val tasks: StateFlow<List<StudyTask>>
    val appointments: StateFlow<List<StudyAppointment>>
    val sessions: StateFlow<List<StudySession>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(database.studyDao())

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        appointments = repository.allAppointments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        sessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadCustomSounds()
    }

    // --- Focus Sound State & Custom Audios ---
    val builtInSounds = listOf(
        "none" to "بدون صوت",
        "white_noise" to "ضوضاء بيضاء 🤍",
        "brown_noise" to "ضوضاء بنية هادئة 🟫",
        "rain" to "هطول المطر المهدئ 🌧️",
        "zen_bell" to "رنين التأمل والتركيز 🔔"
    )

    private val _customSounds = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Pair(filePath, displayName)
    val customSounds = _customSounds.asStateFlow()

    private val _selectedSoundId = MutableStateFlow("none")
    val selectedSoundId = _selectedSoundId.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    private var audioTrack: android.media.AudioTrack? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var soundJob: kotlinx.coroutines.Job? = null
    private var phase = 0L

    fun loadCustomSounds() {
        val destDir = java.io.File(getApplication<Application>().filesDir, "custom_sounds")
        if (destDir.exists() && destDir.isDirectory) {
            val files = destDir.listFiles()
            if (files != null) {
                _customSounds.value = files.map { file ->
                    file.absolutePath to file.name
                }
            }
        }
    }

    fun selectSound(soundId: String) {
        _selectedSoundId.value = soundId
        syncSoundPlayback()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        syncSoundPlayback()
    }

    fun addCustomSound(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                var name = "صوت_مخصص_${System.currentTimeMillis()}.mp3"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            val displayName = it.getString(displayNameIndex)
                            if (!displayName.isNullOrEmpty()) {
                                name = displayName
                            }
                        }
                    }
                }

                val destDir = java.io.File(context.filesDir, "custom_sounds")
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                val destFile = java.io.File(destDir, name)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Refresh list
                loadCustomSounds()
                // Auto select newly added custom sound
                _selectedSoundId.value = destFile.absolutePath
                syncSoundPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCustomSound(filePath: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                }
                if (_selectedSoundId.value == filePath) {
                    _selectedSoundId.value = "none"
                }
                loadCustomSounds()
                syncSoundPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncSoundPlayback() {
        stopAudioPlayback()

        val soundId = _selectedSoundId.value
        val isTimerRunning = _isTimerRunning.value
        val isBreakState = _isBreak.value
        val isSoundPlayable = _isSoundEnabled.value

        if (!isTimerRunning || isBreakState || !isSoundPlayable || soundId == "none") {
            return
        }

        if (soundId in listOf("white_noise", "brown_noise", "rain", "zen_bell")) {
            playSynthesizedSound(soundId)
        } else {
            playCustomFileSound(soundId)
        }
    }

    private fun playSynthesizedSound(soundType: String) {
        soundJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val sampleRate = 44100
            val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            try {
                val track = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val bufferSize = 4410
                val buffer = ShortArray(bufferSize)
                val rand = java.util.Random()
                var lastValue = 0f
                phase = 0L

                while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                    when (soundType) {
                        "white_noise" -> {
                            for (i in 0 until bufferSize) {
                                buffer[i] = (rand.nextInt(65536) - 32768).toShort()
                            }
                        }
                        "brown_noise" -> {
                            for (i in 0 until bufferSize) {
                                val white = rand.nextFloat() * 2f - 1f
                                lastValue = (lastValue + 0.02f * white) / 1.02f
                                var current = lastValue * 3.5f
                                if (current > 1.0f) current = 1.0f
                                if (current < -1.0f) current = -1.0f
                                buffer[i] = (current * 32767).toInt().toShort()
                            }
                        }
                        "rain" -> {
                            for (i in 0 until bufferSize) {
                                val white = rand.nextFloat() * 2f - 1f
                                lastValue = (lastValue + 0.12f * white) / 1.12f
                                var current = lastValue * 1.8f
                                if (rand.nextFloat() < 0.003f) {
                                    current += rand.nextFloat() * 0.5f - 0.25f
                                }
                                if (current > 1.0f) current = 1.0f
                                if (current < -1.0f) current = -1.0f
                                buffer[i] = (current * 32767).toInt().toShort()
                            }
                        }
                        "zen_bell" -> {
                            val frequency = 180.0
                            val pulseSpeed = 0.15
                            for (i in 0 until bufferSize) {
                                val t = phase.toDouble() / sampleRate
                                val amp = (Math.sin(2.0 * Math.PI * pulseSpeed * t) + 1.0) / 2.0
                                val sine = Math.sin(2.0 * Math.PI * frequency * t)
                                buffer[i] = (sine * amp * 15000.0).toInt().toShort()
                                phase++
                            }
                        }
                    }
                    track.write(buffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playCustomFileSound(filePath: String) {
        soundJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    val mp = android.media.MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudioPlayback() {
        soundJob?.cancel()
        soundJob = null

        try {
            audioTrack?.apply {
                if (playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
    }

    // --- Pomodoro Study Timer State ---
    private val _timerSecondsRemaining = MutableStateFlow(0)
    val timerSecondsRemaining = _timerSecondsRemaining.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(0)
    val timerTotalSeconds = _timerTotalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _isBreak = MutableStateFlow(false)
    val isBreak = _isBreak.asStateFlow()

    private val _activeSubject = MutableStateFlow("")
    val activeSubject = _activeSubject.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(subject: String, durationMinutes: Int) {
        timerJob?.cancel()
        _activeSubject.value = subject
        _isBreak.value = false
        _timerTotalSeconds.value = durationMinutes * 60
        _timerSecondsRemaining.value = durationMinutes * 60
        _isTimerRunning.value = true

        syncSoundPlayback()
        runTimerLoop()
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        syncSoundPlayback()
    }

    fun resumeTimer() {
        if (_timerSecondsRemaining.value > 0 && !_isTimerRunning.value) {
            _isTimerRunning.value = true
            syncSoundPlayback()
            runTimerLoop()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSecondsRemaining.value = 0
        _timerTotalSeconds.value = 0
        _isBreak.value = false
        _activeSubject.value = ""
        syncSoundPlayback()
    }

    private fun runTimerLoop() {
        timerJob = viewModelScope.launch {
            while (_timerSecondsRemaining.value > 0) {
                delay(1000)
                if (_isTimerRunning.value) {
                    _timerSecondsRemaining.value -= 1
                } else {
                    break
                }
            }

            if (_timerSecondsRemaining.value == 0 && _isTimerRunning.value) {
                // Timer finished successfully!
                _isTimerRunning.value = false
                if (!_isBreak.value) {
                    // It was a Focus Session -> Log to database
                    val loggedSession = StudySession(
                        subject = _activeSubject.value.ifEmpty { "مذاكرة عامة" },
                        date = System.currentTimeMillis(),
                        durationMinutes = _timerTotalSeconds.value / 60,
                        completedMinutes = _timerTotalSeconds.value / 60,
                        isCompleted = true,
                        focusScore = 5,
                        notes = "تم إكمال جلسة المذاكرة بنجاح!"
                    )
                    insertSession(loggedSession)

                    // Switch to short break (5 minutes)
                    _isBreak.value = true
                    _timerTotalSeconds.value = 5 * 60
                    _timerSecondsRemaining.value = 5 * 60
                    _isTimerRunning.value = true
                    syncSoundPlayback()
                    runTimerLoop()
                } else {
                    // Finished Break
                    _isBreak.value = false
                    _activeSubject.value = ""
                    syncSoundPlayback()
                }
            }
        }
    }

    // --- AI Smart Advice & Notification Section ---
    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _aiNotifications = MutableStateFlow<List<String>>(emptyList())
    val aiNotifications = _aiNotifications.asStateFlow()

    fun fetchSmartScheduleAndAdvice() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAdvice.value = null

            val currentTasks = tasks.value
            val currentAppts = appointments.value

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Fallback smart tip if key is missing or not configured
                delay(1000)
                _aiAdvice.value = getLocalFallbackAdvice(currentTasks, currentAppts)
                _aiNotifications.value = getLocalFallbackNotifications(currentTasks)
                _isAiLoading.value = false
                return@launch
            }

            // Build an informative Arabic prompt
            val tasksDescription = if (currentTasks.isEmpty()) {
                "لا توجد مهام دراسية حالية."
            } else {
                currentTasks.joinToString("\n") { "- مهمة: ${it.title} في مادة ${it.subject} (الأولوية: ${it.priority})" }
            }

            val apptsDescription = if (currentAppts.isEmpty()) {
                "لا توجد مواعيد مقررة."
            } else {
                currentAppts.joinToString("\n") { "- موعد: ${it.title} في مادة ${it.subject} (النوع/ملاحظة: ${it.notes})" }
            }

            val prompt = """
                أنت مستشار ومجدول دراسي ذكي. بناءً على قائمة المهام والمواعيد والامتحانات التالية للطالب، قم بـ:
                1. تقديم جدول زمني مقترح للمذاكرة وتقسيم ذكي للوقت.
                2. تقديم نصيحة دراسية مخصصة لرفع التركيز بناءً على الأولويات ومكافحة التسويف.
                3. تقديم 3 تنبيهات/إشعارات ذكية مخصصة (كل تنبيه عبارة عن جملة قصيرة مشجعة تذكره بمهمة قادمة).

                المهام الدراسية الحالية:
                $tasksDescription

                المواعيد والامتحانات المقررة:
                $apptsDescription

                ملاحظة: يرجى كتابة الرد باللغة العربية بأسلوب راقٍ، مباشر، ومحفز للتحصيل الدراسي العالي. استخدم لغة واضحة ونقاطاً منسقة.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = "أنت مساعد دراسي وأكاديمي ذكي مصمم لتمكين الطلاب من النجاح وتنظيم وقتهم ببراعة ونظام."))
                )
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrEmpty()) {
                    _aiAdvice.value = responseText
                    // Parse notices from response (or split recommendations into bullet alerts safely)
                    val parsedAlerts = responseText.lines()
                        .filter { it.contains("تنبيه") || it.contains("إشعار") || (it.trim().startsWith("-") && it.length > 20) }
                        .take(3)
                        .map { it.replace(Regex("^[-*•\\s\\d.]*"), "").trim() }
                    
                    if (parsedAlerts.isNotEmpty()) {
                        _aiNotifications.value = parsedAlerts
                    } else {
                        // fallback notifications if Gemini didn't format them cleanly
                        _aiNotifications.value = listOf(
                            "حان وقت التركيز! أنجز مهمة '${currentTasks.firstOrNull()?.title ?: "دراستك"}' لتشعر بالإنجاز.",
                            "تذكير ذكي: لا تؤجل عمل اليوم للغد. خطتك الدراسية تنتظرك!",
                            "تنفس بعمق لمده دقيقتين واستعد لجلسة بومودورو جديدة ملهمة."
                        )
                    }
                } else {
                    _aiAdvice.value = "لم نتمكن من الحصول على جدولة ذكية حالياً. يرجى مراجعة المهام يدوياً."
                }
            } catch (e: Exception) {
                _aiAdvice.value = "حدث خطأ أثناء الاتصال بالمستشار الذكي: ${e.message}\n\nإليك خطة اقتراح محلية:\n" + getLocalFallbackAdvice(currentTasks, currentAppts)
                _aiNotifications.value = getLocalFallbackNotifications(currentTasks)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun getLocalFallbackAdvice(currentTasks: List<StudyTask>, currentAppts: List<StudyAppointment>): String {
        return """
            📚 **توصيات المذاكرة والجدول المقترح (تلقائي محلي):**
            
            1. **البداية بالأولوية العالية**: ركز اليوم على المهام ذات الأولوية العالية لتخفيف الضغط الدراسي عنك.
            2. **جلسات تركيز بومودورو**: خصص 25 دقيقة للمذاكرة تليها 5 دقائق راحة لرفع الاستيعاب بنسبة تصل لـ 40%.
            3. **التوازن الدراسي اليومي**: ننصح بجدولة 3 جلسات بومودورو للمواد العلمية، وجلستين للمراجعة العامة.
            
            ${if (currentTasks.isNotEmpty()) "⚠️ متبقي لديك قيد الإنجاز: ${currentTasks.size} مهام واجبة." else "✅ أحسنت! ليس لديك أي مهام معلقة."}
            ${if (currentAppts.isNotEmpty()) "🗓️ تذكر مواعيدك القادمة: ${currentAppts.take(2).joinToString(", ") { it.title }}." else ""}
        """.trimIndent()
    }

    private fun getLocalFallbackNotifications(currentTasks: List<StudyTask>): List<String> {
        val firstTaskText = currentTasks.firstOrNull()?.let { "مراعاة مهمة '${it.title}'" } ?: "دراستك اليومية"
        return listOf(
            "⏳ تذكير ذكي: خصص نصف ساعة الآن للبدء في $firstTaskText.",
            "💡 نصيحة تركيز: دراستك اليوم تبني مستقبلك غداً، واصل الكفاح بحب!",
            "🔔 راحة سريعة: انتهت جلسة مذاكرة بومودورو، انهض وتحرك لتجديد طاقتك."
        )
    }

    // --- Database Operations ---
    fun insertTask(task: StudyTask) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: StudyTask) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: StudyTask) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun insertAppointment(appointment: StudyAppointment) = viewModelScope.launch {
        repository.insertAppointment(appointment)
    }

    fun updateAppointment(appointment: StudyAppointment) = viewModelScope.launch {
        repository.updateAppointment(appointment)
    }

    fun deleteAppointment(appointment: StudyAppointment) = viewModelScope.launch {
        repository.deleteAppointment(appointment)
    }

    fun insertSession(session: StudySession) = viewModelScope.launch {
        repository.insertSession(session)
    }

    fun updateSession(session: StudySession) = viewModelScope.launch {
        repository.updateSession(session)
    }

    fun deleteSession(session: StudySession) = viewModelScope.launch {
        repository.deleteSession(session)
    }
}
