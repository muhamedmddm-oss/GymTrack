package com.example.gymtrack.workout.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.validation.DayValidation
import com.example.gymtrack.workout.presentation.theme.WorkoutColors
import com.example.gymtrack.workout.presentation.theme.workoutTextFieldColors
import com.example.gymtrack.workout.presentation.viewmodel.DayDraft

/** Editable card for one workout day: name field + weekday selector + optional remove button. */
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
