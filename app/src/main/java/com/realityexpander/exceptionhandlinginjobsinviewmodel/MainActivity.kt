package com.realityexpander.exceptionhandlinginjobsinviewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.realityexpander.exceptionhandlinginjobsinviewmodel.ui.theme.ExceptionHandlingInJobsInViewModelTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExceptionHandlingInJobsInViewModelTheme {

                val viewModel = MainViewModel()
                val logMutableState = remember { mutableStateListOf<String>() }
                val logSharedFlow = remember { mutableStateListOf<String>() }
                val logFlow = remember { mutableStateListOf<String>() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val resultState = viewModel.loginState
                    val resultSharedFlow = viewModel.loginSharedFlow.collectAsState(State())

                    LaunchedEffect(true) {
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

                    // Flow
                    LaunchedEffect(true) {
                        viewModel.loginFlow.collectLatest {
                            logFlow += it.statusMessage +": "+
                                    it.errorMessage +
                                    ", logIn=${it.isLoggedIn}" +
                                    ", err=${it.isError}"
                        }
                    }

                    Column {
                        ResultView(resultState.toString())
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Compose State:",  logMutableState)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("SharedFlow:", logSharedFlow)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Flow:", logFlow)
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