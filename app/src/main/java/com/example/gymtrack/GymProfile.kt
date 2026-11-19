
package com.example.gymtrack

import android.content.Context
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.gymtrack.data.saveProfileToDatabase
import com.example.gymtrack.data.syncProfileToDatabaseIfMissing

private val Background = Color(0xFF080B09)
private val CardColor = Color(0xFF171E19)
private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFFAAAAAA)
private val Orange = Color(0xFFFF9800)
private val Blue = Color(0xFF2196F3)
private val LightBlue = Color(0xFF81D4FA)

data class MemberProfile(
    val name: String,
    val age: String,
    val weight: String,
    val height: String,
    val days: List<String>
)

private fun saveProfile(context: Context, profile: MemberProfile) {
    val prefs = context.getSharedPreferences(
        "GymMemberData",
        Context.MODE_PRIVATE
    )

    prefs.edit()
        .putString("name", profile.name)
        .putString("age", profile.age)
        .putString("weight", profile.weight)
        .putString("height", profile.height)
        .putString("days", profile.days.joinToString(","))
        .putBoolean("profile_saved", true)
        .apply()
}

private fun loadProfile(context: Context): MemberProfile? {
    val prefs = context.getSharedPreferences(
        "GymMemberData",
        Context.MODE_PRIVATE
    )

    if (!prefs.getBoolean("profile_saved", false)) {
        return null
    }

    return MemberProfile(
        name = prefs.getString("name", "") ?: "",
        age = prefs.getString("age", "") ?: "",
        weight = prefs.getString("weight", "") ?: "",
        height = prefs.getString("height", "") ?: "",
        days = (prefs.getString("days", "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
    )
}

@Composable
fun GymSetupApp(onFinished: () -> Unit = {}) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf("splash") }
    var onboardingPage by remember { mutableStateOf(0) }
    var savedProfile by remember { mutableStateOf<MemberProfile?>(null) }

    LaunchedEffect(Unit) {
        delay(1500)

        savedProfile = loadProfile(context)

        val existing = savedProfile
        if (existing != null) {
            // Returning user: make sure the profile is also in the database, then go straight to the app.
            syncProfileToDatabaseIfMissing(context, existing)
            onFinished()
        } else {
            screen = "onboarding"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {

        when (screen) {

            "splash" -> SplashScreen()

            "onboarding" -> OnboardingScreen(
                page = onboardingPage,
                onNext = {
                    if (onboardingPage < 2) {
                        onboardingPage++
                    } else {
                        screen = "welcome"
                    }
                },
                onSkip = {
                    screen = "welcome"
                }
            )

            "welcome" -> WelcomeScreen(
                onStart = {
                    screen = "profile"
                }
            )

            "profile" -> ProfileSetupScreen(
                onSave = { profile ->
                    saveProfile(context, profile)
                    scope.launch { saveProfileToDatabase(context, profile) }
                    savedProfile = profile
                    screen = "done"
                }
            )

            "done" -> savedProfile?.let { profile ->
                ProfileDoneScreen(profile, onContinue = onFinished)
            }
        }
    }
}

// ---------------- SPLASH SCREEN ----------------

@Composable
fun SplashScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "🏋️",
            fontSize = 75.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "IRON HOUSE",
            color = Orange,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Text(
            text = "GYM & FITNESS",
            color = White,
            fontSize = 15.sp,
            letterSpacing = 5.sp
        )

        Spacer(modifier = Modifier.height(35.dp))

        CircularProgressIndicator(
            color = Orange,
            strokeWidth = 3.dp,
            modifier = Modifier.size(30.dp)
        )
    }
}

// ---------------- ONBOARDING ----------------

@Composable
fun OnboardingScreen(
    page: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {

    val titles = listOf(
        "Welcome to Iron House",
        "Train Smarter",
        "Track Your Progress"
    )

    val descriptions = listOf(
        "Start your fitness journey with us and build a stronger version of yourself.",
        "Create your personal fitness profile and choose the training schedule that suits you.",
        "Stay consistent, follow your routine and achieve your fitness goals."
    )

    val icons = listOf("🏋️", "💪", "📈")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "IRON HOUSE",
                color = Orange,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            TextButton(onClick = onSkip) {
                Text("Skip", color = Gray)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            ),
            shape = RoundedCornerShape(25.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = icons[page],
                    fontSize = 100.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = titles[page],
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = descriptions[page],
            color = Gray,
            fontSize = 16.sp,
            lineHeight = 25.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index == page) 27.dp else 9.dp)
                        .height(8.dp)
                        .background(
                            if (index == page) Orange else Gray,
                            RoundedCornerShape(10.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            Text(
                text = if (page == 2) "Get Started" else "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------- WELCOME SCREEN ----------------

@Composable
fun WelcomeScreen(onStart: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "🏋️",
            fontSize = 100.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "WELCOME TO",
            color = Gray,
            fontSize = 15.sp,
            letterSpacing = 4.sp
        )

        Text(
            text = "IRON HOUSE GYM",
            color = Orange,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Your journey to a stronger, healthier you starts here.",
            color = White,
            fontSize = 16.sp,
            lineHeight = 25.sp
        )

        Spacer(modifier = Modifier.height(45.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor =Orange,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text(
                text = "Create My Profile",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------- PROFILE SETUP ----------------

@Composable
fun ProfileSetupScreen(
    onSave: (MemberProfile) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val selectedDays = remember {
        mutableStateListOf<String>()
    }

    val days = listOf(
        "Saturday",
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday"
    )

    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {

        Text(
            text = "PROFILE SETUP",
            color = Orange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Let's Get To Know You",
            color = White,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your information to personalize your fitness journey.",
            color = Gray,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        SectionHeading("Personal Information")

        ProfileInput(
            label = "Full Name",
            value = name,
            onValueChange = { name = it },
            hint = "Enter your full name",
            keyboardType = KeyboardType.Text
        )

        ProfileInput(
            label = "Age",
            value = age,
            onValueChange = { age = it },
            hint = "Enter your age",
            keyboardType = KeyboardType.Number
        )

        ProfileInput(
            label = "Weight (kg)",
            value = weight,
            onValueChange = { weight = it },
            hint = "Example: 75",
            keyboardType = KeyboardType.Decimal
        )

        ProfileInput(
            label = "Height (cm)",
            value = height,
            onValueChange = { height = it },
            hint = "Example: 175",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeading("Choose Your Training Days")

        Text(
            text = "Select the days you plan to visit the gym.",
            color = Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        days.chunked(2).forEach { rowDays ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                rowDays.forEach { day ->

                    FilterChip(
                        selected = day in selectedDays,
                        onClick = {
                            if (day in selectedDays) {
                                selectedDays.remove(day)
                            } else {
                                selectedDays.add(day)
                            }
                        },
                        label = {
                            Text(
                                text = day.take(3),
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Orange,
                            selectedLabelColor = Color.Black,
                            containerColor = CardColor,
                            labelColor = White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Selected: ${selectedDays.size} training days",
            color = Orange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {

                val ageValue = age.toIntOrNull()
                val weightValue = weight.toFloatOrNull()
                val heightValue = height.toIntOrNull()

                error = when {
                    name.isBlank() -> "Please enter your name."
                    ageValue == null || ageValue !in 1..120 ->
                        "Please enter a valid age."
                    weightValue == null || weightValue <= 0f ->
                        "Please enter a valid weight."
                    heightValue == null || heightValue <= 0 ->
                        "Please enter a valid height."
                    selectedDays.isEmpty() ->
                        "Please select at least one training day."
                    else -> ""
                }

                if (error.isEmpty()) {
                    onSave(
                        MemberProfile(
                            name = name.trim(),
                            age = age,
                            weight = weight,
                            height = height,
                            days = selectedDays.toList()
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(15.dp)
        ) {

            Text(
                text = "Save My Profile",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(25.dp))
    }
}

// ---------------- INPUT COMPONENT ----------------

@Composable
fun ProfileInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Text(
            text = label,
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(hint, color = Gray)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType
            ),
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Orange,
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = Orange,
                focusedContainerColor = CardColor,
                unfocusedContainerColor = CardColor
            )
        )
    }
}

// ---------------- SECTION HEADING ----------------

@Composable
fun SectionHeading(title: String) {

    Text(
        text = title,
        color = Orange,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif
    )

    Spacer(modifier = Modifier.height(10.dp))
}

// ---------------- PROFILE COMPLETED ----------------

@Composable
fun ProfileDoneScreen(
    profile: MemberProfile,
    onContinue: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "✓",
            color = Orange,
            fontSize = 65.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "PROFILE SAVED!",
            color = Orange,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Welcome, ${profile.name}!",
            color = White,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                SectionHeading("Your Information")

                InfoLine("Name", profile.name)
                InfoLine("Age", "${profile.age} years")
                InfoLine("Weight", "${profile.weight} kg")
                InfoLine("Height", "${profile.height} cm")

                Spacer(modifier = Modifier.height(15.dp))

                SectionHeading("Training Days")

                profile.days.forEach { day ->
                    Text(
                        text = "✓  $day",
                        color = White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your fitness journey starts now!",
            color = Gray,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoLine(label: String, value: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(label, color = Gray, fontSize = 15.sp)

        Text(
            value,
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
