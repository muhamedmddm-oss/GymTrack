package com.example.gymtrack.data

import android.content.Context
import android.util.Log
import com.example.gymtrack.MemberProfile
import kotlinx.coroutines.CancellationException


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
