package com.cutm.nt14.ui.ratelimits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.daos.RateLimitDao
import com.cutm.nt14.data.local.entities.RateLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateLimitViewModel @Inject constructor(
    private val dao: RateLimitDao,
    private val sessionManager: com.cutm.nt14.data.local.SessionManager
) : ViewModel() {

    val userRole = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cutm.nt14.domain.model.UserRole.VIEWER)

    val rules = dao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateRule(rule: RateLimit) {
        viewModelScope.launch {
            dao.insertRule(rule)
        }
    }
}
