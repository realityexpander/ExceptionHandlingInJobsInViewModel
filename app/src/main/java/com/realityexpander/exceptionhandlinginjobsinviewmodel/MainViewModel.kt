package com.realityexpander.exceptionhandlinginjobsinviewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class State(
    var isLoading: Boolean = false,
    var isSuccess: Boolean = false,
    var isError: Boolean = false,
    var errorMessage: String = "",
    var statusMessage: String = ""
)
class MainViewModel() : ViewModel() {

    // Compose
    var loginState by mutableStateOf(State(), neverEqualPolicy())
        private set

    // Kotlin Coroutine
    var loginSharedFlow = MutableSharedFlow<State>(replay = 1)
        private set

    // Kotlin Coroutine Flow
    val loginFlow = flow {
        loginSharedFlow.collect() {
            emit(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun login() {

        // Only handles exceptions in the child coroutine(s). And NOT CancelledException.
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("Login exception handler Caught $exception  - ${Thread.currentThread().name}")

            loginState = loginState.copy(
                statusMessage = "Login exception handler Caught",
                isLoading = false,
                isError = true,
                errorMessage = exception.message ?: "Unknown Error"
            )
            loginSharedFlow.tryEmit(loginState)
            //exception.printStackTrace()
        }

        viewModelScope.launch(exceptionHandler) {
//        viewModelScope.launch() {

            println("Login called - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login called", isLoading = true)
            loginSharedFlow.emit(loginState)
            yield() // allows the `emit` to update the UI

//            val job = launch(Dispatchers.IO) {
            val loginJob = async(Dispatchers.IO) {

//                throw IOException("parent coroutine IOException")
//                throw CancellationException("parent coroutine CancellationException")

                val isSuccessLogin = repositoryLogin()

//                throw IOException("parent coroutine IOException")
//                throw CancellationException("parent coroutine CancellationException")

                println("Login successful - ${Thread.currentThread().name}")
                loginState = loginState.copy(statusMessage = "Login successful", isLoading = false, isSuccess = isSuccessLogin)
                loginSharedFlow.emit(loginState)
                yield()

                isSuccessLogin
            }
            yield()  // Allows the `loginJob` coroutine a chance to start, especially if its cancelled right away with `loginJob.cancel()`

//            delay(50)
//            job.join()              // suspends until job completes
//            job.cancelAndJoin()   // cancels job and suspends until job completes
            loginJob.cancel()          // cancels job but does not suspend

            if(!loginJob.isCancelled) {
                println("Login job is not cancelled, awaiting `job` result...")
                loginJob.await()
            }

            // Allows this `viewModelScope.launch` block to run to the end, in case of `loginJob.cancel()`
            yield()

            println("login job: isCancelled=${loginJob.isCancelled}, " +
                    "value=${if(loginJob.isCompleted && !loginJob.isCancelled) loginJob.getCompleted() else "not completed or cancelled"}, " +
                    "isActive=${loginJob.isActive}, " +
                    "isCompleted=${loginJob.isCompleted}, "+
                    "completionException=${if(loginJob.isCancelled) loginJob.getCompletionExceptionOrNull()?.javaClass?.name else ""}"
            )

            if(loginJob.isCancelled) {
                println("Login cancelled - ${Thread.currentThread().name}")
                loginState = loginState.copy(
                    statusMessage = "Login cancelled",
                    isLoading = false,
                    isError = true,
                    isSuccess = false,
                    errorMessage = loginJob.getCompletionExceptionOrNull()
                        ?.javaClass
                        ?.name
                        ?.replaceBeforeLast(".", "")
                        ?: "Unknown Error"
                )
                yield()
                loginSharedFlow.emit(loginState)
            }

            // note: without `loginJob.join()` or `loginJob.await()` this is printed before the login is finished
            println("Login finished - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login finished", isLoading = false)
            loginSharedFlow.emit(loginState)
            yield()
        }

//        // Emit parallel
//        viewModelScope.launch {
//            yield()
//            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 1"))
//
//            yield()
//            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 2"))
//
//            yield()
//            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 3"))
//        }
    }

    private suspend fun repositoryLogin(): Boolean {
//        try {
        println("Login Repository called - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin called")
        loginSharedFlow.emit(loginState)
        delay(100)

        println("login running...")
        loginState = loginState.copy(statusMessage = "RepositoryLogin running...")
        loginSharedFlow.emit(loginState)
        delay(100)

//            throw IOException("Login failed - IOException")
//            throw CancellationException("Login failed - cancelled")

        var isSuccessLogin = true

        println("Login logged in - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin Logged in", isSuccess = isSuccessLogin)
        loginSharedFlow.emit(loginState)
        yield()

        return isSuccessLogin // isSuccess

//        } catch(e: CancellationException) {
//            println("Login cancelled")
//            throw e
//        } catch(e: Exception) {
//            println("Login failed")
//        }
    }

}