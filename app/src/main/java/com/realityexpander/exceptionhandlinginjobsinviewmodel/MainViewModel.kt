package com.realityexpander.exceptionhandlinginjobsinviewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

data class State(
    var isLoading: Boolean = false,
    var isLoggedIn: Boolean = false,
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

                // Throw exception before job is started
//                throw IOException("parent coroutine IOException")
//                throw CancellationException("parent coroutine CancellationException")

                // Run Job either way:
                //   1) exceptions are handled by the parent coroutine:
                //val isLoginSuccess = repositoryLoginThrowExceptionToParent()
                //   2) exceptions are handled in the child function using try/catch:
                val isLoginSuccess = repositoryLoginWithTryCatch()

                // Throw exception after job is started
//                throw IOException("parent coroutine IOException")
//                throw CancellationException("parent coroutine CancellationException")

                println("Login Job completed - ${Thread.currentThread().name}")
                loginState = loginState.copy(statusMessage = "Login Job completed", isLoading = false, isLoggedIn = isLoginSuccess)
                loginSharedFlow.emit(loginState)
                yield()

                isLoginSuccess
            }
            yield()  // Allows the `loginJob` coroutine a chance to start, especially if its cancelled right away with `loginJob.cancel()`

//            delay(50)
//            job.join()                 // suspends until job completes
//            job.cancelAndJoin()        // cancels job and suspends until job completes
//            loginJob.cancel()          // cancels job but does not suspend

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
                    isLoggedIn = false,
                    errorMessage = loginJob.getCompletionExceptionOrNull()
                        ?.javaClass
                        ?.name
                        ?.replaceBeforeLast(".", "")
                        ?: "Unknown Error"
                )
                yield()
                loginSharedFlow.emit(loginState)
            }

            // note: without `loginJob.join()` or `loginJob.await()` this is printed before the login is finished!
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

    // Exceptions are handled in the parent coroutine.
    // Returns true if login successful, or throws exception to be handled by parent coroutine.
    // Exceptions must be handled in the parent coroutine, otherwise the app will crash (unless handled with a exceptionHander).
    // CancellationExceptions simply stop the coroutine. They are not thrown to the parent coroutine.
    private suspend fun repositoryLoginThrowExceptionToParent(): Boolean {
        println("Login Repository called - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin called")
        loginSharedFlow.emit(loginState)
        delay(100)

        println("login running...")
        loginState = loginState.copy(statusMessage = "RepositoryLogin running...")
        loginSharedFlow.emit(loginState)
        delay(100)

//            throw IOException("Login failed - IOException")  // thrown to parent
//            throw CancellationException("Login failed - cancelled") // NOT thrown to parent. It simply stops the coroutine.

        val isSuccessLogin = true

        println("Login logged in - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin Logged in", isLoggedIn = isSuccessLogin)
        loginSharedFlow.emit(loginState)
        yield()

        return isSuccessLogin // isSuccess
    }

    // Handles exceptions in the child function using try/catch.
    // Returns true if successful login, or catches exception and returns false.
    // Allows finer control over the exception handling, and allows the parent coroutine to continue.
    private suspend fun repositoryLoginWithTryCatch(): Boolean {
        var isLoginSuccess = false

        return try {
            println("Login Repository called - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "RepositoryLogin called")
            loginSharedFlow.emit(loginState)
            delay(100)

            println("login running...")
            loginState = loginState.copy(statusMessage = "RepositoryLogin running...")
            loginSharedFlow.emit(loginState)
            delay(100)

                throw IOException("Login failed - IOException") // caught by try/catch. Parent coroutine continues.
//                throw CancellationException("Login failed - cancelled")  // caught by try/catch. Parent coroutine continues.

            isLoginSuccess = true

            println("Login logged in - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "RepositoryLogin Logged in", isLoggedIn = isLoginSuccess)
            loginSharedFlow.emit(loginState)
            yield()

            isLoginSuccess // isSuccess

        } catch(e: CancellationException) {  // Must be specifically caught! Otherwise, it just stops the parent coroutine.
            println("Login cancelled")
            loginState = loginState.copy(statusMessage = "RepositoryLogin cancelled", isLoggedIn = false)
            loginSharedFlow.emit(loginState)
            yield()

            //throw e  // rethrow exception to be handled by parent coroutine
            false
        } catch(e: Exception) {
            println("Login failed")
            loginState = loginState.copy(statusMessage = "RepositoryLogin failed", isLoggedIn = false)
            loginSharedFlow.emit(loginState)
            yield()

            false
        }
    }

}