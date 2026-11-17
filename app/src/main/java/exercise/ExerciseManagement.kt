package com.example.myapplication.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101010)
private val CardColor = Color(0xFF202020)
private val Orange = Color(0xFFFF8A00)
private val White = Color(0xFFFFFFFF)
private val Gray = Color(0xFF999999)

private const val ALL_MUSCLES = "All Muscles"

private val muscles = listOf(
    "Chest",
    "Shoulders",
    "Legs",
    "Arms",
    "Back",
    "Abs"
)

private data class Exercise(
    val id: Int,
    val name: String,
    val muscle: String,
    val notes: String
)

@Composable
fun ExerciseManagementScreen() {

    val exercises = remember { mutableStateListOf<Exercise>() }

    var search by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf(ALL_MUSCLES) }

    var showForm by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var nextId by remember { mutableIntStateOf(1) }

    val filteredExercises = filterExercises(exercises, search, selectedMuscle)

    fun saveExercise(name: String, muscle: String, notes: String) {
        val oldExercise = selectedExercise

        if (oldExercise == null) {
            exercises.add(
                Exercise(
                    id = nextId++,
                    name = name,
                    muscle = muscle,
                    notes = notes
                )
            )
        } else {
            val index = exercises.indexOfFirst { it.id == oldExercise.id }

            if (index != -1) {
                exercises[index] = oldExercise.copy(
                    name = name,
                    muscle = muscle,
                    notes = notes
                )
            }
        }

        showForm = false
        selectedExercise = null
    }

    fun deleteSelectedExercise() {
        val exercise = selectedExercise

        if (exercise != null) {
            exercises.removeAll { it.id == exercise.id }
        }

        showDelete = false
        selectedExercise = null
    }

    MaterialTheme(colorScheme = appColorScheme()) {
        Scaffold(
            containerColor = Background,
            floatingActionButton = {
                AddExerciseButton(
                    onClick = {
                        selectedExercise = null
                        showForm = true
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                ScreenHeader()

                Spacer(modifier = Modifier.height(24.dp))

                SearchField(
                    search = search,
                    onSearchChange = { search = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Muscle Group",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                MuscleFilterRow(
                    selectedMuscle = selectedMuscle,
                    onMuscleSelected = { selectedMuscle = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                ExerciseCountRow(count = filteredExercises.size)

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredExercises.isEmpty()) {
                    EmptyState(
                        isFiltering = search.isNotEmpty() || selectedMuscle != ALL_MUSCLES,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ExerciseList(
                        exercises = filteredExercises,
                        onDetails = { exercise ->
                            selectedExercise = exercise
                            showDetails = true
                        },
                        onEdit = { exercise ->
                            selectedExercise = exercise
                            showForm = true
                        },
                        onDelete = { exercise ->
                            selectedExercise = exercise
                            showDelete = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showForm) {
            ExerciseFormDialog(
                exercise = selectedExercise,
                onDismiss = {
                    showForm = false
                    selectedExercise = null
                },
                onSave = { name, muscle, notes ->
                    saveExercise(name, muscle, notes)
                }
            )
        }

        val detailsExercise = selectedExercise

        if (showDetails && detailsExercise != null) {
            ExerciseDetailsDialog(
                exercise = detailsExercise,
                onClose = {
                    showDetails = false
                    selectedExercise = null
                }
            )
        }

        val deleteExercise = selectedExercise

        if (showDelete && deleteExercise != null) {
            DeleteExerciseDialog(
                exerciseName = deleteExercise.name,
                onDismiss = { showDelete = false },
                onConfirm = { deleteSelectedExercise() },
                onCancel = {
                    showDelete = false
                    selectedExercise = null
                }
            )
        }
    }
}

private fun filterExercises(
    exercises: List<Exercise>,
    search: String,
    selectedMuscle: String
): List<Exercise> {
    return exercises.filter { exercise ->
        val matchesName = exercise.name.contains(search, ignoreCase = true)
        val matchesMuscle = selectedMuscle == ALL_MUSCLES || exercise.muscle == selectedMuscle

        matchesName && matchesMuscle
    }
}

private fun appColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = Orange,
        background = Background,
        surface = CardColor,
        onPrimary = White,
        onBackground = White,
        onSurface = White
    )
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Orange,
        contentColor = White
    ) {
        Text("+", fontSize = 30.sp)
    }
}

@Composable
private fun ScreenHeader() {
    Text(
        text = "Exercise Management",
        color = White,
        fontSize = 25.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Manage your exercises and workouts",
        color = Gray,
        fontSize = 14.sp
    )
}

@Composable
private fun SearchField(
    search: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Search exercises...", color = Gray)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Orange,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = White,
            unfocusedTextColor = White,
            cursorColor = Orange
        )
    )
}

@Composable
private fun MuscleFilterRow(
    selectedMuscle: String,
    onMuscleSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf(ALL_MUSCLES) + muscles) { muscle ->
            FilterChip(
                selected = selectedMuscle == muscle,
                onClick = { onMuscleSelected(muscle) },
                label = { Text(muscle) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Orange,
                    selectedLabelColor = White,
                    labelColor = Gray
                )
            )
        }
    }
}

@Composable
private fun ExerciseCountRow(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Your Exercises",
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$count Exercises",
            color = Orange,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmptyState(
    isFiltering: Boolean,
    modifier: Modifier
) {
    val title = if (isFiltering) "No exercises found" else "No exercises yet"

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏋️",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap + to add your first exercise",
                color = Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<Exercise>,
    onDetails: (Exercise) -> Unit,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        items(
            items = exercises,
            key = { it.id }
        ) { exercise ->
            ExerciseCard(
                exercise = exercise,
                onDetails = { onDetails(exercise) },
                onEdit = { onEdit(exercise) },
                onDelete = { onDelete(exercise) }
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetails() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exercise.name,
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = exercise.muscle,
                    color = Orange,
                    fontSize = 13.sp
                )
            }

            if (exercise.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = exercise.notes,
                    color = Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExerciseCardButtons(
                onDetails = onDetails,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun ExerciseCardButtons(
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onDetails) {
            Text("Details", color = White)
        }

        TextButton(onClick = onEdit) {
            Text("Edit", color = Orange)
        }

        TextButton(onClick = onDelete) {
            Text("Delete", color = Color(0xFFFF5252))
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(
    exercise: Exercise,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardColor,
        title = {
            Text(exercise.name, color = White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Muscle Group", exercise.muscle)

                if (exercise.notes.isNotBlank()) {
                    DetailRow("Notes", exercise.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close", color = Orange)
            }
        }
    )
}

@Composable
private fun DetailRow(
    title: String,
    value: String
) {
    Column {
        Text(title, color = Gray, fontSize = 12.sp)
        Text(value, color = White, fontSize = 15.sp)
    }
}

@Composable
private fun DeleteExerciseDialog(
    exerciseName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = {
            Text("Delete Exercise", color = White)
        },
        text = {
            Text(
                "Are you sure you want to delete $exerciseName?",
                color = Gray
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Orange)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = White)
            }
        }
    )
}

@Composable
private fun ExerciseFormDialog(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember(exercise) {
        mutableStateOf(exercise?.name ?: "")
    }

    var muscle by remember(exercise) {
        mutableStateOf(exercise?.muscle ?: "Chest")
    }

    var notes by remember(exercise) {
        mutableStateOf(exercise?.notes ?: "")
    }

    val dialogTitle = if (exercise == null) "Add Exercise" else "Edit Exercise"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = {
            Text(
                text = dialogTitle,
                color = White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                MuscleDropdown(
                    muscle = muscle,
                    onMuscleSelected = { muscle = it }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = fieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), muscle, notes.trim())
                    }
                }
            ) {
                Text("Save", color = Orange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDropdown(
    muscle: String,
    onMuscleSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = muscle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Muscle Group") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    MenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ),
            colors = fieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            muscles.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onMuscleSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Orange,
    unfocusedBorderColor = Color.DarkGray,
    focusedLabelColor = Orange,
    unfocusedLabelColor = Gray,
    focusedTextColor = White,
    unfocusedTextColor = White,
    cursorColor = Orange
)
