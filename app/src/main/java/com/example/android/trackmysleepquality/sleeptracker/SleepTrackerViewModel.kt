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
import android.util.Log
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
    private val _nights = database.getAllNights()
    val nights: LiveData<List<SleepNight>> = _nights

    val nightsString = Transformations.map(_nights) { nights ->
        formatNights(nights, application.resources)
    }

    private val tonight = MutableLiveData<SleepNight?>()

    private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()
    val navigateToSleepQuality: LiveData<SleepNight?> = _navigateToSleepQuality

    private val _navigateToSleepDetail = MutableLiveData<Long?>()

    val navigateToSleepDetail: LiveData<Long?> = _navigateToSleepDetail

    private val _showSnackbar = MutableLiveData<Boolean>()
    val showSnackbar: LiveData<Boolean> = _showSnackbar

    // Buttons
    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }
    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }
    val clearButtonVisible = Transformations.map(nights) {
        it.isNotEmpty()
    }

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
            _navigateToSleepQuality.value = oldNight
        }
    }

    fun doneNavigatingToSleepQuality() {
        _navigateToSleepQuality.value = null
    }

    private suspend fun update(oldNight: SleepNight) {
        database.update(oldNight)
    }


    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDetail.value = id
    }

    fun doneNavigatingToSleepDetail() {
        _navigateToSleepDetail.value = null

    }

}

