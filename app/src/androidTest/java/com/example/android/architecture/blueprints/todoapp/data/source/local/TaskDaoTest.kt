/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

// Create test class
class TaskDaoTest {
    private lateinit var database: ToDoDatabase

    @Before
    fun initDb() {
        // Add a test database
        // create database in memory instead of storage, which is much faster since it doesn't persist
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            ToDoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    // Add a test
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insertTaskAndGetTasks() = runTest {

        // Create task object
        val task = LocalTask(
            title = "title",
            description = "description",
            id = "id",
            isCompleted = false,
        )
        // Upsert task into our test database
        database.taskDao().upsert(task)

        // Get first object in database
        val tasks = database.taskDao().observeAll().first()

        // Test that the two actions above worked successfully
//        assertEquals(0, tasks.size) // purposefully failing test, to test the test :)
        assertEquals(1, tasks.size) // checks that upsert() works (1 task in database)
        assertEquals(task, tasks[0]) // checks that observeAll() works (task in database matches the one we inserted)

    }
}