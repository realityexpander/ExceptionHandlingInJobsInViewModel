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
                val log = remember { mutableStateListOf<String>() }
                val log2 = remember { mutableStateListOf<String>() }
                val log3 = remember { mutableStateListOf<String>() }

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
                        log += resultState.statusMessage +" : "+
                                resultState.errorMessage +
                                ", success=${resultState.isSuccess}"
                    }

                    // MutableSharedFlow
                    LaunchedEffect(resultSharedFlow.value) {
                        log2 += resultSharedFlow.value.statusMessage + " : "+
                                resultSharedFlow.value.errorMessage +
                                ", success=${resultSharedFlow.value.isSuccess}"
                    }

                    // Flow
                    LaunchedEffect(true) {
                        viewModel.loginFlow.collectLatest {
                            log3 += it.statusMessage +" : "+
                                    it.errorMessage +
                                    ", success=${it.isSuccess}"
                        }
                    }

                    Column {
                        ResultView(resultState.toString())
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("State:",  log)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("SharedFlow:", log2)
                        Spacer(modifier = Modifier.height(16.dp))

                        LogView("Flow:", log3)
                    }

                }
            }
        }
    }
}

@Composable
fun ResultView(value: String) {
    Text(text = "Result: $value")
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