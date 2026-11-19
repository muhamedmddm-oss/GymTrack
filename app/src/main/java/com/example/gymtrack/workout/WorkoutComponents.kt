package com.example.gymtrack.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextOverflow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(text = title, color = WorkoutColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WorkoutColors.TextPrimary
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WorkoutColors.Background)
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete",
    dismissLabel: String = "Cancel",
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkoutColors.Card,
        title = { Text(text = title, color = WorkoutColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(text = message, color = WorkoutColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) WorkoutColors.Error else WorkoutColors.Orange,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel, color = WorkoutColors.TextPrimary)
            }
        }
    )
}

@Composable
fun OrangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WorkoutColors.Orange,
            contentColor = WorkoutColors.OnOrange,
            disabledContainerColor = WorkoutColors.Orange.copy(alpha = 0.4f),
            disabledContentColor = WorkoutColors.OnOrange
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun EmptyPlansState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(WorkoutColors.Card, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = WorkoutColors.Orange,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No workout plans yet",
            color = WorkoutColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first plan and choose which days you train.",
            color = WorkoutColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = WorkoutColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = WorkoutColors.Error, fontSize = 12.sp, modifier = modifier)
}

@Composable
fun EditorDayCard(
    number: Int,
    draft: DayDraft,
    validation: DayValidation?,
    showErrors: Boolean,
    takenByOthers: Set<Weekday>,
    canRemove: Boolean,
    onNameChange: (String) -> Unit,
    onWeekdaySelect: (Weekday) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nameError = if (showErrors) validation?.nameError else null
    val weekdayError = if (showErrors) validation?.weekdayError else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Day $number",
                    color = WorkoutColors.Orange,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove day $number",
                            tint = WorkoutColors.Error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Workout day name") },
                placeholder = { Text("e.g. Push") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = workoutTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Weekday", color = WorkoutColors.TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            WeekdaySelector(
                selected = draft.weekday,
                takenByOthers = takenByOthers,
                onSelect = onWeekdaySelect
            )
            if (weekdayError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                ErrorText(weekdayError)
            }

            if (draft.id != 0L && draft.exerciseCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exerciseLabel(draft.exerciseCount) + " attached",
                    color = WorkoutColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = when {
        selected -> WorkoutColors.Orange
        else -> WorkoutColors.CardRaised
    }
    val content = when {
        selected -> WorkoutColors.OnOrange
        enabled -> WorkoutColors.TextPrimary
        else -> WorkoutColors.TextSecondary.copy(alpha = 0.4f)
    }
    Surface(
        modifier = modifier
            .height(42.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = container,
        border = if (selected) null else BorderStroke(1.dp, WorkoutColors.Outline.copy(alpha = if (enabled) 1f else 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, color = content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DayCountSelector(
    selectedCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxDays: Int = 7
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (count in 1..maxDays) {
            SelectableChip(
                label = count.toString(),
                selected = count == selectedCount,
                enabled = true,
                onClick = { onSelect(count) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun WeekdaySelector(
    selected: Weekday?,
    takenByOthers: Set<Weekday>,
    onSelect: (Weekday) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Weekday.entries.forEach { weekday ->
                SelectableChip(
                    label = weekday.shortName,
                    selected = weekday == selected,
                    enabled = weekday !in takenByOthers,
                    onClick = { onSelect(weekday) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}



object WorkoutColors {
    val Background = Color(0xFF101010)
    val Card = Color(0xFF202020)
    val CardRaised = Color(0xFF2B2B2B)
    val Orange = Color(0xFFFF8A00)
    val OnOrange = Color(0xFF101010)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFAAAAAA)
    val Outline = Color(0xFF444444)
    val Error = Color(0xFFFF6B6B)
}

@Composable
fun workoutTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WorkoutColors.TextPrimary,
    unfocusedTextColor = WorkoutColors.TextPrimary,
    errorTextColor = WorkoutColors.TextPrimary,
    focusedContainerColor = WorkoutColors.Card,
    unfocusedContainerColor = WorkoutColors.Card,
    errorContainerColor = WorkoutColors.Card,
    cursorColor = WorkoutColors.Orange,
    errorCursorColor = WorkoutColors.Error,
    focusedBorderColor = WorkoutColors.Orange,
    unfocusedBorderColor = WorkoutColors.Outline,
    errorBorderColor = WorkoutColors.Error,
    focusedLabelColor = WorkoutColors.Orange,
    unfocusedLabelColor = WorkoutColors.TextSecondary,
    errorLabelColor = WorkoutColors.Error,
    errorSupportingTextColor = WorkoutColors.Error
)


@Composable
fun WorkoutDayCard(
    number: Int,
    day: WorkoutDay,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WorkoutColors.CardRaised, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "View details", onClick = onViewDetails)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(WorkoutColors.Orange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = WorkoutColors.OnOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.name,
                color = WorkoutColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${day.weekday.displayName} • ${exerciseLabel(day.exerciseCount)}",
                color = WorkoutColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit ${day.name}", tint = WorkoutColors.TextSecondary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${day.name}", tint = WorkoutColors.Error)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View details",
            tint = WorkoutColors.Orange,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

internal fun exerciseLabel(count: Int): String = when (count) {
    0 -> "No exercises yet"
    1 -> "1 exercise"
    else -> "$count exercises"
}


@Composable
fun WorkoutPlanCard(
    plan: WorkoutPlan,
    onEditPlan: () -> Unit,
    onDeletePlan: () -> Unit,
    onViewDay: (WorkoutDay) -> Unit,
    onEditDay: (WorkoutDay) -> Unit,
    onDeleteDay: (WorkoutDay) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        color = WorkoutColors.TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (plan.days.size == 1) "1 training day per week" else "${plan.days.size} training days per week",
                        color = WorkoutColors.Orange,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onEditPlan) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${plan.name}", tint = WorkoutColors.TextPrimary)
                }
                IconButton(onClick = onDeletePlan) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${plan.name}", tint = WorkoutColors.Error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.days.forEachIndexed { index, day ->
                    WorkoutDayCard(
                        number = index + 1,
                        day = day,
                        onViewDetails = { onViewDay(day) },
                        onEdit = { onEditDay(day) },
                        onDelete = { onDeleteDay(day) }
                    )
                }
            }
        }
    }
}
