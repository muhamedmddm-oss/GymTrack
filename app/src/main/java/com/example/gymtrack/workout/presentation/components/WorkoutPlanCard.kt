package com.example.gymtrack.workout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtrack.workout.domain.model.WorkoutDay
import com.example.gymtrack.workout.domain.model.WorkoutPlan
import com.example.gymtrack.workout.presentation.theme.WorkoutColors

/** A plan with its header (name, edit, delete) and one [WorkoutDayCard] per training day. */
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
