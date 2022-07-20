/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {
    val nights = database.getAllNights()
    val nightString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    private val tonight = MutableLiveData<SleepNight?>()
    private val _showSnackbar = MutableLiveData<Boolean>()
    val showSnackbar: LiveData<Boolean> = _showSnackbar

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }

    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    fun onClear() {
        _showSnackbar.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                clear()
            }
        }
    }

    private suspend fun clear() {
        database.clear()
    }

    fun doneShowingSnackbar() {
        _showSnackbar.value = false
    }

    fun onStartTracking() {
        viewModelScope.launch {
            val newNight = SleepNight()
            withContext(Dispatchers.IO) {
                insert(newNight)
            }
            tonight.value = getTonightFromDatabase()

        }
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
    }

    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                update(oldNight)
            }
        }
    }

    private suspend fun update(oldNight: SleepNight) {
        database.update(oldNight)
    }

}

