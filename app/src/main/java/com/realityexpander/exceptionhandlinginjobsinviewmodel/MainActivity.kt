package com.realityexpander.exceptionhandlinginjobsinviewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ScrollingView
import com.realityexpander.exceptionhandlinginjobsinviewmodel.ui.theme.ExceptionHandlingInJobsInViewModelTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExceptionHandlingInJobsInViewModelTheme {

                val viewModel = MainViewModel()
                val logMutableState = remember { mutableStateListOf<String>() }
                val logSharedFlow = remember { mutableStateListOf<String>() }
                val logStateFlow = remember { mutableStateListOf<String>() }
                val logFlow = remember { mutableStateListOf<String>() }
                val logChannel = remember { mutableStateListOf<String>() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val resultState = viewModel.loginState
                    val resultSharedFlow = viewModel.loginSharedFlow.collectAsState(State())
                    val resultStateFlow = viewModel.loginStateFlow.collectAsState()

                    LaunchedEffect(true) {
                        delay(2000) // Uncomment to allow app to start and user put app in background to see difference in behavior
                        viewModel.login()
                    }

                    // MutableState
                    LaunchedEffect(resultState) {
                        logMutableState += resultState.statusMessage +": "+
                                resultState.errorMessage +
                                ", logIn=${resultState.isLoggedIn}" +
                                ", err=${resultState.isError}"
                    }

                    // MutableSharedFlow
                    LaunchedEffect(resultSharedFlow.value) {
                        logSharedFlow += resultSharedFlow.value.statusMessage + ": "+
                                resultSharedFlow.value.errorMessage +
                                ", logIn=${resultSharedFlow.value.isLoggedIn}" +
                                ", err=${resultSharedFlow.value.isError}"
                    }

                    // MutableStateFlow
                    LaunchedEffect(resultStateFlow.value) {
                        logStateFlow += resultStateFlow.value.statusMessage + ": "+
                                resultStateFlow.value.errorMessage +
                                ", logIn=${resultStateFlow.value.isLoggedIn}" +
                                ", err=${resultStateFlow.value.isError}"
                    }

                    // Flow
                    LaunchedEffect(true) {
                        viewModel.loginFlow.collectLatest {
                            logFlow += it.statusMessage +": "+
                                    it.errorMessage +
                                    ", logIn=${it.isLoggedIn}" +
                                    ", err=${it.isError}"
                        }
                    }

                    // Channel
                    LaunchedEffect(true) {
                        viewModel.loginChannel.receiveAsFlow().collectLatest {
                            logChannel += it.statusMessage +": "+
                                    it.errorMessage +
                                    ", logIn=${it.isLoggedIn}" +
                                    ", err=${it.isError}"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ResultView(resultState.toString())
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Compose State:", logMutableState)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("SharedFlow:", logSharedFlow)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("StateFlow:", logStateFlow)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Flow:", logFlow)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Channel:", logChannel)

                    }

                }
            }
        }
    }
}

@Composable
fun ResultView(value: String) {
    Text(text = "Final Result: $value")
}

@Composable
fun LogView(title: String, log: List<String>) {
    Column {
        Text(text = title)
        log.forEach {
            Text(text = "• $it")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ExceptionHandlingInJobsInViewModelTheme {
        ResultView("Android")
    }
}