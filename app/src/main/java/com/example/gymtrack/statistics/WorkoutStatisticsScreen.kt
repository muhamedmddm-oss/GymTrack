package com.example.gymtrack.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.data.GymTrackData
import androidx.lifecycle.viewmodel.initializer

private val StatsDark = Color(0xFF101010)
private val StatsCard = Color(0xFF202020)
private val StatsOrange = Color(0xFFFF8A00)
private val StatsMuted = Color(0xFFAAAAAA)

@Composable
fun WorkoutStatisticsRoute() {
    val context = LocalContext.current.applicationContext
    val vm: WorkoutStatisticsViewModel = viewModel(
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer { WorkoutStatisticsViewModel(GymTrackData.sessions(context)) }
        }
    )
    val pair by vm.uiState.collectAsStateWithLifecycle()
    val range by vm.selectedRange.collectAsStateWithLifecycle()
    WorkoutStatisticsScreen(
        stats = pair.first,
        isLoading = pair.second,
        selectedRange = range,
        onRangeSelected = vm::selectRange
    )
}

@Composable
private fun WorkoutStatisticsScreen(
    stats: WorkoutStatistics,
    isLoading: Boolean,
    selectedRange: StatisticsRange,
    onRangeSelected: (StatisticsRange) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(StatsDark).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your Progress", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Real results from your completed workouts", color = StatsMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            StatisticsRange.values().forEach { item ->
                FilterChip(
                    selected = selectedRange == item,
                    onClick = { onRangeSelected(item) },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatsOrange,
                        selectedLabelColor = Color.Black,
                        labelColor = Color.White
                    )
                )
            }
        }
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StatsOrange)
            }
        } else if (stats.sessions == 0) {
            Card(colors = CardDefaults.cardColors(containerColor = StatsCard), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No completed workouts yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Finish a workout to see your activity, volume and exercise progress here.", color = StatsMuted)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Sessions", stats.sessions.toString(), Modifier.weight(1f))
                MetricCard("Sets", stats.totalSets.toString(), Modifier.weight(1f))
                MetricCard("Reps", stats.totalReps.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Volume", "${stats.totalVolumeKg.toLong()} kg", Modifier.weight(1f))
                MetricCard("Duration", "${stats.totalDurationMinutes} min", Modifier.weight(1f))
                MetricCard("Avg / session", "${stats.averageDurationMinutes} min", Modifier.weight(1f))
            }
            ChartCard(title = "Workout frequency · recent weeks", values = stats.weeklySessions.map { it.toDouble() }, suffix = " sessions")
            ChartCard(title = "Training volume · recent weeks", values = stats.volumeByWeek, suffix = " kg")
            Card(colors = CardDefaults.cardColors(containerColor = StatsCard), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Most trained exercises", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    stats.topExercises.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${item.first}", color = Color.White)
                            Text("${item.second} sessions", color = StatsOrange, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (stats.bestLiftKg != null) {
                        Divider(color = Color.DarkGray)
                        Text("Heaviest recorded set: ${stats.bestLiftKg} kg", color = StatsMuted)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = StatsCard), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, color = StatsMuted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = StatsOrange, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChartCard(title: String, values: List<Double>, suffix: String) {
    Card(colors = CardDefaults.cardColors(containerColor = StatsCard), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val gap = 8.dp.toPx()
                val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(2f)
                values.forEachIndexed { index, value ->
                    val height = (value / max * size.height).toFloat()
                    drawRoundRect(
                        color = StatsOrange,
                        topLeft = androidx.compose.ui.geometry.Offset(index * (barWidth + gap), size.height - height),
                        size = androidx.compose.ui.geometry.Size(barWidth, height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                    )
                }
            }
            Text("Each bar represents one week · max ${max.toLong()}$suffix", color = StatsMuted, fontSize = 11.sp)
        }
    }
}
