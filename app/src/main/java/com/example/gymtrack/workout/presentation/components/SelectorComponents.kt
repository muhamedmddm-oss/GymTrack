package com.example.gymtrack.workout.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.presentation.theme.WorkoutColors

/** Small rounded toggle used by both selectors. */
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

/** Picks the number of training days (1..7). */
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

/**
 * Picks one weekday for a workout day. Weekdays in [takenByOthers] (already used by another day
 * of the same plan) are disabled, which prevents duplicate weekdays.
 */
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
