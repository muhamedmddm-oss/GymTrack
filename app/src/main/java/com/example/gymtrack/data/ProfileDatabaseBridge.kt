package com.example.gymtrack.data

import android.content.Context
import android.util.Log
import com.example.gymtrack.MemberProfile
import kotlinx.coroutines.CancellationException

/**
 * Connects Person 1's onboarding (`MemberProfile`, kept in SharedPreferences) with the shared Room
 * `user_profile` table, so Home, Statistics and the workout module can read the profile from the database.
 * Database problems are logged and never crash the onboarding flow.
 */
suspend fun saveProfileToDatabase(context: Context, profile: MemberProfile) {
    try {
        GymTrackData.profiles(context).saveProfile(
            name = profile.name,
            heightCm = profile.height.toDoubleOrNull(),
            weightKg = profile.weight.toDoubleOrNull(),
            trainingDaysPerWeek = profile.days.size,
            age = profile.age.toIntOrNull()
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("ProfileBridge", "Could not save the profile to the database", e)
    }
}

/** For users who saved their profile before the database existed: copy it into Room once. */
suspend fun syncProfileToDatabaseIfMissing(context: Context, profile: MemberProfile) {
    try {
        if (GymTrackData.profiles(context).getProfile() == null) {
            saveProfileToDatabase(context, profile)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("ProfileBridge", "Could not sync the profile to the database", e)
    }
}
