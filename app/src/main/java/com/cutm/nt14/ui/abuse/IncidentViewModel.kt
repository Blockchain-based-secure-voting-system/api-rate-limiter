package com.cutm.nt14.ui.abuse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class IncidentViewModel @Inject constructor(
    private val abuseDao: AbuseEventDao,
    private val ddosDao: DDoSIncidentDao,
    private val sessionManager: com.cutm.nt14.data.local.SessionManager
) : ViewModel() {

    val userRole = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cutm.nt14.domain.model.UserRole.VIEWER)

    val abuseEvents = abuseDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ddosIncidents = ddosDao.getAllIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
