package com.twocall.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twocall.chat.ui.components.AnimatedGlassBackground
import com.twocall.chat.ui.components.GlassButton
import com.twocall.chat.ui.components.GlassCard
import com.twocall.chat.ui.theme.*
import com.twocall.chat.ui.viewmodel.PairingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPairCodeScreen(
    viewModel: PairingViewModel,
    onBackClick: () -> Unit,
    onPairedSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isPairedSuccessfully) {
        if (uiState.isPairedSuccessfully) {
            onPairedSuccess()
        }
    }

    AnimatedGlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Enter Partner Code", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonPurple.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "Enter 6-digit Code",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Type the 6-digit pairing code shown on your partner's phone screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = DarkOnSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Digit boxes
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        repeat(6) { index ->
                            val char = if (index < codeInput.length) codeInput[index].toString() else ""
                            val isActive = index == codeInput.length
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x33FFFFFF))
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) NeonPurple else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    ),
                                    color = NeonPurple
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                codeInput = it
                                if (it.length == 6) {
                                    viewModel.joinPair(it)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("Type 6 digits here", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error!!,
                            color = DarkError,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassButton(
                    onClick = { viewModel.joinPair(codeInput) },
                    enabled = codeInput.length == 6 && !uiState.isLoading,
                    gradientColors = listOf(NeonPurple, NeonIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Connect Private Pair",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
