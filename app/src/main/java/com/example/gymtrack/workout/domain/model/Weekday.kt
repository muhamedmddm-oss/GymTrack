package com.example.gymtrack.workout.domain.model

/**
 * Days of the week, ordered the way the app displays them (week starts on Saturday).
 * The enum [name] (e.g. "SATURDAY") is what is stored in the database.
 */
enum class Weekday(val displayName: String, val shortName: String) {
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun"),
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri");

    companion object {
        /** Returns the weekday for a stored name, or null if it is unknown. */
        fun fromName(name: String): Weekday? = entries.firstOrNull { it.name == name }
    }
}
