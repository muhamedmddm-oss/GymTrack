package com.example.gymtrack.data

import com.example.gymtrack.data.dao.UserProfileDao
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileRepositoryTest {

    private class FakeProfileDao : UserProfileDao {
        val rows = MutableStateFlow<List<UserProfileEntity>>(emptyList())
        private var next = 1L
        override suspend fun insert(profile: UserProfileEntity): Long {
            val id = next++
            rows.value = rows.value + profile.copy(id = id)
            return id
        }
        override suspend fun update(profile: UserProfileEntity) {
            rows.value = rows.value.map { if (it.id == profile.id) profile else it }
        }
        override suspend fun delete(profile: UserProfileEntity) {
            rows.value = rows.value.filterNot { it.id == profile.id }
        }
        override suspend fun getById(id: Long) = rows.value.firstOrNull { it.id == id }
        override fun observeById(id: Long): Flow<UserProfileEntity?> = rows.map { l -> l.firstOrNull { it.id == id } }
        override suspend fun getCurrent() = rows.value.minByOrNull { it.id }
        override fun observeCurrent(): Flow<UserProfileEntity?> = rows.map { l -> l.minByOrNull { it.id } }
        override fun observeAll(): Flow<List<UserProfileEntity>> = rows
        override suspend fun count() = rows.value.size
    }

    private val dao = FakeProfileDao()
    private var now = 100L
    private val repo = UserProfileRepository(dao) { now++ }

    @Test
    fun firstSave_createsProfile() = runTest {
        assertNull(repo.getProfile())
        val id = repo.saveProfile("  Mohamed ", 180.0, 80.0, 3, age = 22)
        val p = repo.getProfile()!!
        assertEquals(id, p.id)
        assertEquals("Mohamed", p.name)
        assertEquals(3, p.trainingDaysPerWeek)
        assertEquals(22, p.age)
    }

    @Test
    fun secondSave_updatesSameRow_andKeepsCreatedAt() = runTest {
        val id = repo.saveProfile("A", null, null, 3)
        val created = repo.getProfile()!!.createdAt
        repo.saveProfile("B", 170.0, 65.0, 5)
        assertEquals(1, dao.count())
        val p = repo.getProfile()!!
        assertEquals(id, p.id)
        assertEquals("B", p.name)
        assertEquals(5, p.trainingDaysPerWeek)
        assertEquals(created, p.createdAt)
        assertEquals(true, p.updatedAt > created)
    }
}
