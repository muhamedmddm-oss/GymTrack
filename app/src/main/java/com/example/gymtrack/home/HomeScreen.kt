
package com.example.gymtrack.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Dark = Color(0xFF101010)
private val CardColor = Color(0xFF202020)
private val Orange = Color(0xFFFF8A00)
private val White = Color.White
private val Gray = Color(0xFFAAAAAA)
private val Green = Color(0xFF4CAF50)

@Composable
fun HomeScreen() {
    Scaffold(
        containerColor = Dark
    ) { paddingValues ->
        HomeTab(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun HomeTab(modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Dark)
            .padding(20.dp)
    ) {
        Text(
            text = "Welcome Back, Mohamed",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ready for your next workout?",
            color = Gray,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Your Activity")

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Weekly Workouts",
                value = "2/3",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Total Sessions",
                value = "12",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Workout Days")

        Spacer(modifier = Modifier.height(12.dp))

        WorkoutCard(
            title = "Push Day",
            subtitle = "Chest, Shoulders & Triceps"
        )

        Spacer(modifier = Modifier.height(10.dp))

        WorkoutCard(
            title = "Pull Day",
            subtitle = "Back & Biceps"
        )

        Spacer(modifier = Modifier.height(10.dp))

        WorkoutCard(
            title = "Leg Day",
            subtitle = "Legs & Abs"
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionTitle(title = "Last Workout")

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard(
            title = "Push Day",
            subtitle = "3 Exercises • 45 Minutes"
        )
    }
}

@Composable
fun SectionTitle(title: String) {

    Text(
        text = title,
        color = White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                color = Orange,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WorkoutCard(
    title: String,
    subtitle: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = subtitle,
                    color = Gray,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "›",
                color = Orange,
                fontSize = 28.sp
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    subtitle: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Green,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = Gray,
                fontSize = 14.sp
            )
        }
    }
}
