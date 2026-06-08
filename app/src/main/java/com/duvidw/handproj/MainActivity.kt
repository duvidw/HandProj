package com.duvidw.handproj

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    GateScreen()
                }
            }
        }
    }
}

data class GateUiState(
    val phoneNumber: String = "",
    val durationMinutes: String = "",
    val periodMinutes: String = "",
    val phoneError: String? = null,
    val durationError: String? = null,
    val periodError: String? = null,
    val isRunning: Boolean = false,
    val status: String = "Stopped"
)

class GateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GateUiState())
    val uiState: StateFlow<GateUiState> = _uiState

    private var runJob: Job? = null

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, phoneError = null) }
    }

    fun onDurationChange(value: String) {
        _uiState.update { it.copy(durationMinutes = value, durationError = null) }
    }

    fun onPeriodChange(value: String) {
        _uiState.update { it.copy(periodMinutes = value, periodError = null) }
    }

    fun openGate(context: Context) {
        val current = _uiState.value
        val validation = GateInputValidator.validate(
            current.phoneNumber,
            current.durationMinutes,
            current.periodMinutes
        )

        if (validation.hasError) {
            _uiState.update {
                it.copy(
                    phoneError = validation.phoneError,
                    durationError = validation.durationError,
                    periodError = validation.periodError,
                    status = "Stopped (invalid input)",
                    isRunning = false
                )
            }
            return
        }

        closeGate("Stopped")

        val durationMillis = validation.durationMinutes!!.toLong() * 60_000L
        val periodMillis = validation.periodMinutes!!.toLong() * 60_000L
        val phoneNumber = current.phoneNumber.trim()

        runJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, status = "Running") }
            val startedAt = System.currentTimeMillis()

            while (isActive) {
                if (System.currentTimeMillis() - startedAt >= durationMillis) {
                    break
                }

                if (!launchDialer(context.applicationContext, phoneNumber)) {
                    _uiState.update { it.copy(isRunning = false, status = "Stopped (unable to launch dialer)") }
                    runJob = null
                    return@launch
                }

                val elapsed = System.currentTimeMillis() - startedAt
                val timeLeft = durationMillis - elapsed
                if (timeLeft <= 0L) {
                    break
                }

                delay(min(periodMillis, timeLeft))
            }

            _uiState.update { it.copy(isRunning = false, status = "Stopped (duration complete)") }
            runJob = null
        }
    }

    fun closeGate(status: String = "Stopped") {
        runJob?.cancel()
        runJob = null
        _uiState.update { it.copy(isRunning = false, status = status) }
    }

    private fun launchDialer(context: Context, phone: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
        }.isSuccess
    }
}

data class ValidationResult(
    val phoneError: String?,
    val durationError: String?,
    val periodError: String?,
    val durationMinutes: Int?,
    val periodMinutes: Int?
) {
    val hasError: Boolean get() = phoneError != null || durationError != null || periodError != null
}

object GateInputValidator {
    private val phonePattern = Regex("^[0-9+()\\- ]+$")

    fun validate(phone: String, duration: String, period: String): ValidationResult {
        val trimmedPhone = phone.trim()
        val durationValue = duration.trim().toIntOrNull()
        val periodValue = period.trim().toIntOrNull()

        val phoneError = when {
            trimmedPhone.isEmpty() -> "Phone number is required"
            !trimmedPhone.matches(phonePattern) -> "Phone number contains invalid characters"
            else -> null
        }

        val durationError = when {
            duration.trim().isEmpty() -> "Duration is required"
            durationValue == null || durationValue <= 0 -> "Duration must be a positive number"
            else -> null
        }

        val periodError = when {
            period.trim().isEmpty() -> "Period is required"
            periodValue == null || periodValue <= 0 -> "Period must be a positive number"
            else -> null
        }

        return ValidationResult(phoneError, durationError, periodError, durationValue, periodValue)
    }
}

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChange,
            label = { Text("Cellular number") },
            singleLine = true,
            isError = state.phoneError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { state.phoneError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = state.durationMinutes,
            onValueChange = viewModel::onDurationChange,
            label = { Text("Duration:") },
            singleLine = true,
            isError = state.durationError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { state.durationError?.let { Text(it) } }
        )

        OutlinedTextField(
            value = state.periodMinutes,
            onValueChange = viewModel::onPeriodChange,
            label = { Text("Period") },
            singleLine = true,
            isError = state.periodError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { state.periodError?.let { Text(it) } }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.openGate(context) }) {
                Text("Open Gate")
            }
            Button(onClick = { viewModel.closeGate() }) {
                Text("Close Gate")
            }
        }

        Text(
            text = "Status: ${state.status}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
