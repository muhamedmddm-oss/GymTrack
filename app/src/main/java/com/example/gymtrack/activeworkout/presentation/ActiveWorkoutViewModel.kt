package com.example.gymtrack.activeworkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrack.activeworkout.timer.WorkoutTimer
import com.example.gymtrack.data.dao.SessionExerciseDraft
import com.example.gymtrack.data.dao.SessionExerciseWithSets
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.repository.ExerciseRepository
import com.example.gymtrack.data.repository.WorkoutSessionRepository
import com.example.gymtrack.workout.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val MAX_SETS_PER_EXERCISE = 10
const val MAX_EXERCISE_NAME_LENGTH = 40

enum class TimerStatus { NOT_STARTED, RUNNING, PAUSED }

/** One set as shown on screen. reps / weight are Strings because they are text-field contents. */
data class SetUi(
    val id: Long,
    val setNumber: Int,
    val reps: String,
    val weightKg: String,
    val isCompleted: Boolean,
    val completedAt: Long? = null
)

data class ExerciseUi(val id: Long, val name: String, val sets: List<SetUi>) {
    val completedSets: Int get() = sets.count { it.isCompleted }
    val isCompleted: Boolean get() = sets.isNotEmpty() && completedSets == sets.size
}

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val dayName: String = "",
    val planName: String = "",
    val plannedExerciseCount: Int = 0,
    val sessionId: Long? = null,
    val timerStatus: TimerStatus = TimerStatus.NOT_STARTED,
    val elapsedSeconds: Long = 0,
    val exercises: List<ExerciseUi> = emptyList(),
    val isFinishing: Boolean = false,
    /** One-shot text for the snackbar; the screen calls onMessageShown() afterwards. */
    val message: String? = null,
    /** Set when the workout was saved: the screen navigates to the Workout Summary with this id. */
    val finishedSessionId: Long? = null,
    val leaveScreen: Boolean = false
) {
    val hasStarted: Boolean get() = sessionId != null
    val totalExercises: Int get() = exercises.size
    val completedExercises: Int get() = exercises.count { it.isCompleted }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    /** Overall progress = completed sets / all sets (0f when there are no sets). */
    val progress: Float get() = if (totalSets == 0) 0f else completedSets.toFloat() / totalSets
}

/**
 * State holder of the Active Workout screen.
 *
 * TIMER: the timer lives HERE (not in a composable), so recompositions and rotation can't restart it.
 * It is a [WorkoutTimer] (timestamps, not a counter) and a ticker coroutine only publishes the
 * current elapsed seconds to the UI once per second.
 *
 * ROOM: every change is written to the database immediately (write-through) through a single ordered
 * queue, so nothing is lost if the app is closed and writes can never overtake each other.
 */
class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val dayId: Long,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timer = WorkoutTimer()
    private var tickerJob: Job? = null
    private var startInFlight = false

    // ---- ordered database writes ----
    // Own scope (not viewModelScope) so a write queued right before the user leaves is still completed.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writeQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        writeScope.launch {
            for (job in writeQueue) {
                try {
                    job()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isFinishing = false, message = "Could not save your changes. Please try again.")
                    }
                }
            }
        }
        loadWorkout()
    }

    private fun enqueueWrite(block: suspend () -> Unit) {
        writeQueue.trySend(block)
    }

    // ------------------------------------------------------------------ loading / resuming

    private fun loadWorkout() {
        viewModelScope.launch {
            try {
                val details = workoutRepository.observeDay(dayId).first()
                if (details == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = "This workout day no longer exists.") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        dayName = details.day.name,
                        planName = details.planName,
                        plannedExerciseCount = exerciseRepository.getExercises(dayId).size
                    )
                }
                // A workout that was started earlier and never finished is resumed, not duplicated.
                val unfinished = sessionRepository.getUnfinishedSessionForDay(dayId)
                if (unfinished != null) loadSession(unfinished.id) else _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = "Could not load this workout.") }
            }
        }
    }

    /** Single source of truth: rebuild timer + exercises from what Room has stored. */
    private suspend fun loadSession(sessionId: Long) {
        val saved = sessionRepository.getSessionDetails(sessionId)
        if (saved == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        timer = WorkoutTimer(saved.session.accumulatedMillis, saved.session.runningSinceMillis)
        _uiState.update {
            it.copy(
                isLoading = false,
                sessionId = sessionId,
                exercises = saved.exercises
                    .sortedWith(compareBy({ ex -> ex.exercise.orderIndex }, { ex -> ex.exercise.id }))
                    .map { ex -> ex.toUi() }
            )
        }
        publishTimer()
        if (timer.isRunning) startTicker()
    }

    // ------------------------------------------------------------------ timer

    fun onStartWorkout() {
        val state = _uiState.value
        if (state.isLoading || state.hasStarted || startInFlight) return
        startInFlight = true
        viewModelScope.launch {
            try {
                val newSessionId = sessionRepository.startSession(
                    dayId = dayId,
                    dayName = state.dayName,
                    planName = state.planName,
                    startRunning = true
                )
                // The exercises planned for this day (teammate's part) become the starting list.
                val drafts = exerciseRepository.getExercises(dayId).mapIndexed { index, planned ->
                    SessionExerciseDraft(
                        exercise = SessionExerciseEntity(
                            sessionId = newSessionId,
                            exerciseId = planned.id,
                            exerciseName = planned.name,
                            orderIndex = index
                        ),
                        sets = (1..planned.sets).map { number ->
                            SessionSetEntity(
                                sessionExerciseId = 0,
                                setNumber = number,
                                reps = planned.reps,
                                weightKg = planned.weightKg
                            )
                        }
                    )
                }
                sessionRepository.addExercisesWithSets(newSessionId, drafts)
                loadSession(newSessionId)
                showMessage("Workout started. Good luck!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("Could not start the workout. Please try again.")
            } finally {
                startInFlight = false
            }
        }
    }

    fun onPauseWorkout() = changeTimer { it.pause(clock()) }

    fun onResumeWorkout() = changeTimer { it.resume(clock()) }

    fun onResetTimer() = changeTimer { it.reset() }

    private fun changeTimer(transform: (WorkoutTimer) -> WorkoutTimer) {
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        if (state.isFinishing) return
        timer = transform(timer)
        publishTimer()
        if (timer.isRunning) startTicker() else stopTicker()
        val snapshot = timer
        enqueueWrite {
            sessionRepository.saveTimerState(sessionId, snapshot.accumulatedMillis, snapshot.runningSinceMillis)
        }
    }

    private fun publishTimer() {
        val elapsedSeconds = timer.elapsedMillis(clock()) / 1000
        _uiState.update {
            it.copy(
                elapsedSeconds = elapsedSeconds,
                timerStatus = when {
                    !it.hasStarted -> TimerStatus.NOT_STARTED
                    timer.isRunning -> TimerStatus.RUNNING
                    else -> TimerStatus.PAUSED
                }
            )
        }
    }

    /** Publishes the elapsed time once per second, aligned to the second boundary of the timer. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                publishTimer()
                delay(1_000L - timer.elapsedMillis(clock()) % 1_000L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    // ------------------------------------------------------------------ exercises

    fun onAddExercise(name: String, setCount: Int) {
        val sessionId = _uiState.value.sessionId
        if (sessionId == null) {
            showMessage("Press Start Workout before adding exercises.")
            return
        }
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.length > MAX_EXERCISE_NAME_LENGTH) {
            showMessage("Enter an exercise name (up to $MAX_EXERCISE_NAME_LENGTH characters).")
            return
        }
        if (setCount !in 1..MAX_SETS_PER_EXERCISE) {
            showMessage("Sets must be between 1 and $MAX_SETS_PER_EXERCISE.")
            return
        }
        enqueueWrite {
            val orderIndex = _uiState.value.exercises.size
            val newId = sessionRepository.addExerciseWithSets(
                sessionId = sessionId,
                exerciseId = null,
                exerciseName = cleanName,
                orderIndex = orderIndex,
                sets = (1..setCount).map { SessionSetEntity(sessionExerciseId = 0, setNumber = it, reps = 0) }
            )
            val newExercise = ExerciseUi(
                id = newId,
                name = cleanName,
                sets = sessionRepository.getSets(newId).map { it.toUi() }
            )
            _uiState.update {
                it.copy(exercises = it.exercises + newExercise, message = "$cleanName added")
            }
        }
    }

    fun onRemoveExercise(exerciseId: Long) {
        val removed = _uiState.value.exercises.firstOrNull { it.id == exerciseId } ?: return
        _uiState.update { state -> state.copy(exercises = state.exercises.filterNot { it.id == exerciseId }) }
        enqueueWrite { sessionRepository.removeExerciseFromSession(exerciseId) }
        showMessage("${removed.name} removed")
    }

    // ------------------------------------------------------------------ sets

    fun onRepsChanged(exerciseId: Long, setId: Long, text: String) =
        editSet(exerciseId, setId) { it.copy(reps = sanitizeRepsInput(text)) }

    fun onWeightChanged(exerciseId: Long, setId: Long, text: String) =
        editSet(exerciseId, setId) { it.copy(weightKg = sanitizeWeightInput(text)) }

    fun onSetCompletedChanged(exerciseId: Long, setId: Long, completed: Boolean) {
        val current = findSet(exerciseId, setId) ?: return
        if (completed && parseReps(current.reps) == 0) {
            showMessage("Enter the reps before marking the set as done.")
            return
        }
        editSet(exerciseId, setId) {
            it.copy(isCompleted = completed, completedAt = if (completed) clock() else null)
        }
    }

    fun onAddSet(exerciseId: Long) {
        enqueueWrite {
            val exercise = _uiState.value.exercises.firstOrNull { it.id == exerciseId } ?: return@enqueueWrite
            if (exercise.sets.size >= MAX_SETS_PER_EXERCISE) {
                showMessage("An exercise can have at most $MAX_SETS_PER_EXERCISE sets.")
                return@enqueueWrite
            }
            // The new set starts with the previous set's numbers: a quick way to log identical sets.
            val previous = exercise.sets.lastOrNull()
            val number = exercise.sets.size + 1
            val newId = sessionRepository.logSet(
                sessionExerciseId = exerciseId,
                setNumber = number,
                reps = parseReps(previous?.reps.orEmpty()),
                weightKg = parseWeight(previous?.weightKg.orEmpty()),
                completed = false
            )
            val newSet = SetUi(newId, number, previous?.reps.orEmpty(), previous?.weightKg.orEmpty(), false)
            _uiState.update { state ->
                state.copy(exercises = state.exercises.map { if (it.id == exerciseId) it.copy(sets = it.sets + newSet) else it })
            }
        }
    }

    fun onDeleteSet(exerciseId: Long, setId: Long) {
        val exercise = _uiState.value.exercises.firstOrNull { it.id == exerciseId } ?: return
        if (exercise.sets.size <= 1) {
            showMessage("An exercise needs at least one set. Remove the exercise instead.")
            return
        }
        // Remaining sets are renumbered 1..n so the numbering has no gaps.
        val remaining = exercise.sets.filter { it.id != setId }.mapIndexed { index, set -> set.copy(setNumber = index + 1) }
        _uiState.update { state ->
            state.copy(exercises = state.exercises.map { if (it.id == exerciseId) it.copy(sets = remaining) else it })
        }
        enqueueWrite {
            sessionRepository.deleteSet(setId)
            sessionRepository.updateSets(remaining.map { it.toEntity(exerciseId) })
        }
    }

    private fun findSet(exerciseId: Long, setId: Long): SetUi? =
        _uiState.value.exercises.firstOrNull { it.id == exerciseId }?.sets?.firstOrNull { it.id == setId }

    /** Updates one set in the UI state immediately and writes it to Room. */
    private fun editSet(exerciseId: Long, setId: Long, transform: (SetUi) -> SetUi) {
        val updated = findSet(exerciseId, setId)?.let(transform) ?: return
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id != exerciseId) exercise
                    else exercise.copy(sets = exercise.sets.map { if (it.id == setId) updated else it })
                }
            )
        }
        enqueueWrite { sessionRepository.updateSets(listOf(updated.toEntity(exerciseId))) }
    }

    // ------------------------------------------------------------------ finishing

    fun onFinishWorkout() {
        val state = _uiState.value
        val sessionId = state.sessionId
        if (sessionId == null) {
            showMessage("Start the workout before finishing it.")
            return
        }
        if (state.isFinishing) return
        if (state.exercises.isEmpty()) {
            showMessage("Add at least one exercise before finishing.")
            return
        }
        // 1) stop the timer right now and freeze the final duration
        timer = timer.pause(clock())
        stopTicker()
        publishTimer()
        val finalMillis = timer.elapsedMillis(clock())
        _uiState.update { it.copy(isFinishing = true) }
        // 2) queued AFTER every pending set edit, so everything is saved before the session is closed
        enqueueWrite {
            sessionRepository.finishSessionWithDuration(sessionId, finalMillis)
            _uiState.update { it.copy(isFinishing = false, finishedSessionId = sessionId) }
        }
    }

    fun onDiscardWorkout() {
        stopTicker()
        val sessionId = _uiState.value.sessionId
        if (sessionId == null) {
            _uiState.update { it.copy(leaveScreen = true) }
            return
        }
        enqueueWrite {
            sessionRepository.deleteSession(sessionId)
            _uiState.update { it.copy(leaveScreen = true) }
        }
    }

    // ------------------------------------------------------------------ messages

    private fun showMessage(text: String) = _uiState.update { it.copy(message = text) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    override fun onCleared() {
        // Let the writer finish what is already queued, then stop.
        writeQueue.close()
        super.onCleared()
    }
}

// ---- mapping Room entities <-> UI models ----

private fun SessionExerciseWithSets.toUi() = ExerciseUi(
    id = exercise.id,
    name = exercise.exerciseName,
    sets = sets.sortedBy { it.setNumber }.map { it.toUi() }
)

private fun SessionSetEntity.toUi() = SetUi(
    id = id,
    setNumber = setNumber,
    reps = if (reps > 0) reps.toString() else "",
    weightKg = formatWeight(weightKg),
    isCompleted = isCompleted,
    completedAt = completedAt
)

private fun SetUi.toEntity(exerciseId: Long) = SessionSetEntity(
    id = id,
    sessionExerciseId = exerciseId,
    setNumber = setNumber,
    reps = parseReps(reps),
    weightKg = parseWeight(weightKg),
    isCompleted = isCompleted,
    completedAt = completedAt
)
