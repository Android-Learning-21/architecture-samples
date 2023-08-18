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

package com.example.android.architecture.blueprints.todoapp.data

import com.example.android.architecture.blueprints.todoapp.data.source.local.TaskDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.toExternal
import com.example.android.architecture.blueprints.todoapp.data.source.local.toLocal
import com.example.android.architecture.blueprints.todoapp.data.source.local.toNetwork
import com.example.android.architecture.blueprints.todoapp.data.source.network.TaskNetworkDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.network.toLocal
import com.example.android.architecture.blueprints.todoapp.di.ApplicationScope
import com.example.android.architecture.blueprints.todoapp.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val networkDataSource: TaskNetworkDataSource,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun observeAll(): Flow<List<Task>> {
        return localDataSource.observeAll().map { tasks ->
            tasks.toExternal()
        }
    }

    suspend fun create(title: String, description: String): String {
        val taskId = withContext(dispatcher) { createTaskId() }
        val task = Task(
            title = title,
            description = description,
            id = taskId
        )
        localDataSource.upsert(task.toLocal())
        saveTasksToNetwork()

        return taskId

        /* Why don't you use withContext to wrap upsert() or toLocal(), like you did before?
        Firstly, upsert() is provided by the Room library, which takes responsibility for ensuring a non-UI thread is used.
        Secondly, toLocal() is an in-memory copy of a single, small object. If it were CPU or
        I/O bound, or an operation on a collection of objects, then withContext should be used. */
    }

    private fun createTaskId(): String {
        return UUID.randomUUID().toString()
    }

    suspend fun complete(taskId: String) {
        localDataSource.updateCompleted(taskId, true)
        saveTasksToNetwork()
    }

    /** Replaces all the local tasks with those from the network */
    suspend fun refresh() {
        val networkTasks = networkDataSource.loadTasks()
        localDataSource.deleteAll()
        val localTasks = withContext(dispatcher) {
            networkTasks.toLocal()
        }
        localDataSource.upsertAll(localTasks)
    }

    suspend fun saveTasksToNetwork() {
        scope.launch {
            val localTasks =
                localDataSource.observeAll().first() // this will only save the first task, WTF?
            val networkTasks = withContext(dispatcher) {
                localTasks.toNetwork()
            }
            networkDataSource.saveTasks(networkTasks)
        }
    }
}