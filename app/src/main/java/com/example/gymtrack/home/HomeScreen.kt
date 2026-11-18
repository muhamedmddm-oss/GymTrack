
package com.example.gymtrack.home



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.example.gymtrack.data.GymTrackData
import com.example.gymtrack.data.entity.UserProfileEntity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Person

private val Dark = Color(0xFF101010)
private val CardColor = Color(0xFF202020)
private val Orange = Color(0xFFFF8A00)
private val White = Color.White
private val Gray = Color(0xFFAAAAAA)
private val Green = Color(0xFF4CAF50)

// Home Screen

@Composable
fun HomeScreen() {

    val appContext = LocalContext.current.applicationContext

    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(GymTrackData.profiles(appContext))
            }
        }
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        containerColor = Dark,

        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

    ) { paddingValues ->

        when (selectedTab) {

            0 -> HomeTab(
                profile = profile,
                modifier = Modifier.padding(paddingValues)
            )

            1 -> PlaceholderTab(
                title = "Workouts",
                subtitle = "Manage your workout plans",
                modifier = Modifier.padding(paddingValues)
            )

            2 -> PlaceholderTab(
                title = "Progress",
                subtitle = "Track your fitness progress",
                modifier = Modifier.padding(paddingValues)
            )

            3 -> PlaceholderTab(
                title = "Profile",
                subtitle = "Your personal information",
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// Bottom Navigation Bar

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Workouts", Icons.Default.FitnessCenter, 1),
        Triple("Progress", Icons.Default.ShowChart, 2),
        Triple("Profile", Icons.Default.Person, 3)
    )

    NavigationBar(
        containerColor = CardColor,
        contentColor = Orange
    ) {

        items.forEach { item ->

            NavigationBarItem(
                selected = selectedTab == item.third,

                onClick = {
                    onTabSelected(item.third)
                },

                icon = {
                    Icon(
                        imageVector = item.second,
                        contentDescription = item.first
                    )
                },

                label = {
                    Text(item.first)
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Orange,
                    selectedTextColor = Orange,
                    unselectedIconColor = Gray,
                    unselectedTextColor = Gray,
                    indicatorColor = Color(0xFF383027)
                )
            )
        }
    }
}

// Home Tab

@Composable
fun HomeTab(
    profile: UserProfileEntity?,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Dark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = if (profile != null)
                "Welcome Back, ${firstName(profile.name)}"
            else
                "Welcome Back",
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

        if (profile != null) {

            Spacer(modifier = Modifier.height(28.dp))

            SectionTitle(title = "Your Profile")

            Spacer(modifier = Modifier.height(12.dp))

            ProfileStats(profile)
        }

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

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Section Title

@Composable
fun SectionTitle(title: String) {

    Text(
        text = title,
        color = White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
}

// Statistics Card

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

// Workout Card

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

// Last Workout Card

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

// Profile Statistics

@Composable
fun ProfileStats(profile: UserProfileEntity) {

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            StatCard(
                title = "Weight",
                value = profile.weightKg?.let {
                    "${formatNumber(it)} kg"
                } ?: "—",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Height",
                value = profile.heightCm?.let {
                    "${formatNumber(it)} cm"
                } ?: "—",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            StatCard(
                title = "Age",
                value = profile.age?.let {
                    "$it years"
                } ?: "—",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Days / Week",
                value = profile.trainingDaysPerWeek.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Temporary Pages

@Composable
fun PlaceholderTab(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Dark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = title,
            color = Orange,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            color = Gray,
            fontSize = 15.sp
        )
    }
}

// Helper Functions

private fun firstName(name: String): String =
    name.trim().substringBefore(' ')

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0)
        value.toInt().toString()
    else
        value.toString()
