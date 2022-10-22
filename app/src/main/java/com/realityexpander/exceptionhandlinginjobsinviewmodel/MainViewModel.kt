package com.realityexpander.exceptionhandlinginjobsinviewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.*
import java.io.IOException

data class State(
    var isLoading: Boolean = false,
    var isSuccess: Boolean = false,
    var isError: Boolean = false,
    var errorMessage: String = "",
    var statusMessage: String = ""
)
class MainViewModel() : ViewModel() {

    var loginState by mutableStateOf(State(), neverEqualPolicy())
        private set

    var loginSharedFlow = MutableSharedFlow<State>(replay = 1)
        private set

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

        // Emit parallel
        viewModelScope.launch {
            yield()
            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 1"))

            yield()
            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 2"))

            yield()
            loginSharedFlow.emit(loginState.copy(statusMessage = "parallel emit 3"))
        }

        viewModelScope.launch(exceptionHandler) {
//        viewModelScope.launch() {

            println("Login called - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login called", isLoading = true)
            loginSharedFlow.emit(loginState)
            yield()

//            val job = launch(Dispatchers.IO) {
            val job = async(Dispatchers.IO) {
                val isSuccessLogin = repositoryLogin()

//                throw IOException("parent coroutine IOException")
//                throw CancellationException("parent coroutine CancellationException")

                println("Login successful - ${Thread.currentThread().name}")
                loginState = loginState.copy(statusMessage = "Login successful", isLoading = false, isSuccess = isSuccessLogin)
                loginSharedFlow.emit(loginState)
                yield()

                isSuccessLogin
            }

            yield()  // give the job coroutine a chance to start, ie: for `cancel()`
//            delay(50)
//            job.join()              // suspends until job completes
//            job.cancelAndJoin()   // cancels job and suspends until job completes
//            job.cancel()          // cancels job but does not suspend

            if(!job.isCancelled) job.await()
            yield()
            println("login job: isCancelled=${job.isCancelled}, " +
                    "value=${if(job.isCompleted && !job.isCancelled) job.getCompleted() else "not completed or cancelled"}, " +
                    "isActive=${job.isActive}, " +
                    "isCompleted=${job.isCompleted}, "+
                    "completionException=${if(job.isCancelled) job.getCompletionExceptionOrNull()?.javaClass?.name else ""}"
            )

            if(job.isCancelled) {
                println("Login cancelled - ${Thread.currentThread().name}")
                loginState = loginState.copy(
                    statusMessage = "Login cancelled",
                    isLoading = false,
                    isError = true,
                    isSuccess = false,
                    errorMessage = job.getCompletionExceptionOrNull()
                        ?.javaClass
                        ?.name
                        ?.replaceBeforeLast(".", "")
                        ?: "Unknown Error"
                )
                yield()
                loginSharedFlow.emit(loginState)
            }

            // note: without job.join() or job.await() this is printed before the login is finished
            println("Login finished - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login finished", isLoading = false)
            loginSharedFlow.emit(loginState)
            yield()
        }
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