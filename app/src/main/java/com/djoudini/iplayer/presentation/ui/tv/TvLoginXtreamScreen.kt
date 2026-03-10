package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.domain.model.Resource
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.components.FocusableTextField
import com.djoudini.iplayer.presentation.components.ProgressRing
import com.djoudini.iplayer.presentation.viewmodel.OnboardingViewModel

@Composable
fun TvLoginXtreamScreen(
    onLoginSuccess: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success<*>) {
            onLoginSuccess((loginState as Resource.Success<Long>).data)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.TopCenter)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableCard(
                    onClick = onBack,
                    modifier = Modifier.size(56.dp),
                    focusScale = 1.1f
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = stringResource(R.string.xtream_codes_login),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Input fields
            FocusableTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.playlist_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FocusableTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = stringResource(R.string.server_url),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FocusableTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.username),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FocusableTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (loginState is Resource.Error) {
                Text(
                    text = (loginState as Resource.Error).message ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Progress or Button - always at the bottom
            if (syncProgress.isActive) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(progress = syncProgress.progress, size = 32.dp, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = syncProgress.phase,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!syncProgress.isIndeterminate) {
                        LinearProgressIndicator(
                            progress = { syncProgress.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                val isLoading = loginState is Resource.Loading
                
                FocusableCard(
                    onClick = {
                        if (!isLoading && name.isNotBlank() && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                            viewModel.loginXtream(name, serverUrl, username, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    focusScale = 1.05f
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = stringResource(R.string.connect_and_sync),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            
            // Extra space at bottom for scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
