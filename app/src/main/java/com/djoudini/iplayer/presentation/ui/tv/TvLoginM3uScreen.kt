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
import androidx.compose.foundation.text.KeyboardOptions
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
fun TvLoginM3uScreen(
    onLoginSuccess: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success<*>) {
            onLoginSuccess((loginState as Resource.Success<Long>).data)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.6f).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableCard(
                    onClick = onBack,
                    modifier = Modifier.size(64.dp),
                    focusScale = 1.1f
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                Text(
                    text = stringResource(R.string.m3u_playlist),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            FocusableTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.playlist_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            FocusableTextField(
                value = m3uUrl,
                onValueChange = { m3uUrl = it },
                label = stringResource(R.string.m3u_url),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (loginState is Resource.Error) {
                Text(
                    text = (loginState as Resource.Error).message ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

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
                    Spacer(modifier = Modifier.height(16.dp))
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
                        if (!isLoading && name.isNotBlank() && m3uUrl.isNotBlank()) {
                            viewModel.loginM3u(name, m3uUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    focusScale = 1.05f
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = stringResource(R.string.import_and_sync),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
