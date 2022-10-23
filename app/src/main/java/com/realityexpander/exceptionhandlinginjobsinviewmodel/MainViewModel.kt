package com.realityexpander.exceptionhandlinginjobsinviewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class State(
    var isLoading: Boolean = false,
    var isLoggedIn: Boolean = false,
    var isError: Boolean = false,
    var errorMessage: String = "",
    var statusMessage: String = "(initial state)"
)
class MainViewModel() : ViewModel() {

    // Compose State - - updates not sent when app is in background
    var loginState by mutableStateOf(State(), neverEqualPolicy())
        private set

    // Kotlin Coroutines Shared Flow - updates not sent when app is in background
    var loginSharedFlow = MutableSharedFlow<State>(replay = 1)
        private set

    // Kotlin Coroutines State Flow - updates not sent when app is in background
    private var _loginStateFlow = MutableStateFlow<State>(State())
    var loginStateFlow: StateFlow<State> = _loginStateFlow.asStateFlow()

    // Kotlin Coroutine Flow - updates are sent when app is in background
    val loginFlow = flow {
        loginSharedFlow.collect() {
            emit(it)

            // Uncomment to test when app in background the difference between `Compose State`/`SharedFlow` and `Flow`/`Channel`
            // Also uncomment to see behavior difference with Channel and Flow for interrupted emissions.
//            delay(1500)
        }
    }

    // Kotlin Channel - updates are sent when app is in background
    val loginChannel = Channel<State>()

    // UTILS //////////////////////////////////////////////////////////////////

    // Emit state on all methods in current coroutine
    private suspend fun emitStateAllMethodsCurrentCoroutine(state: State) {
        loginSharedFlow.emit(state)
        _loginStateFlow.value = state
        loginChannel.send(state)
    }

    // Emit state on all methods in new coroutine.
    // Note: When this is used, the `Flow` "delay" above is not interrupted.
    // All collectors will receive the state.
    // ** RECOMMENDED METHOD **
    private fun emitStateAllMethodsNewCoroutine(state: State) {
        viewModelScope.launch {
            loginSharedFlow.emit(state)
            _loginStateFlow.value = state
            loginChannel.send(state)
        }
    }

    // Emit state on all methods without launching new coroutine
    // Note: because this is not launched in a new coroutine,
    //   the `Flow` and Channel do not always receive the exception from exceptionHandler.
    private fun emitStateAllMethodsWithoutCoroutine(state: State) {
        loginSharedFlow.tryEmit(state)
        _loginStateFlow.value = state
        loginChannel.trySend(state)
    }

    // LOGIN //////////////////////////////////////////////////////////////////

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun login() {

        // Only handles exceptions in the child coroutine(s). And *NOT* CancelledException.
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("Login exception handler Caught $exception  - ${Thread.currentThread().name}")

            loginState = loginState.copy(
                statusMessage = "Login exception handler Caught",
                isLoading = false,
                isError = true,
                errorMessage = exception.message ?: "Unknown Error"
            )
//            emitStateAllMethodsWithoutCoroutine(loginState) // Note: Collectors do not always receive the exception emission.
            emitStateAllMethodsNewCoroutine(loginState) // Collectors will always receive the exception emission.

            //exception.printStackTrace()
        }

        // • Try different launch scenarios (with and without exceptionHandler) for Parent Coroutine
        viewModelScope.launch(exceptionHandler) {
//        viewModelScope.launch() {

            println("Login called - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login called", isLoading = true)
            emitStateAllMethodsCurrentCoroutine(loginState)
            yield() // allows the `emit` time to update the UI

            // • Try different child coroutine call scenarios (launch, async, withContext) for Child Coroutine
//            val loginJob = launch(Dispatchers.IO) {
            val loginJob = async(Dispatchers.IO) {

                // •• Try different cancellation/call scenarios:

                // • Throw exception before job is started:
//                throw IOException("loginJob coroutine IOException") // Will not run `repositoryLogin`, and is caught by the exceptionHandler and cancels the Parent.
//                throw CancellationException("loginJob coroutine CancellationException") // Will not run `repositoryLogin`, and Parent will run to completion(!).


                // • Try different loginJob repository call scenarios.
                //   1) exceptions are handled by the parent coroutine:
//                val isLoginSuccess = repositoryLoginThrowExceptionToParent()

                //   2) exceptions are handled in the child function using try/catch:
                val isLoginSuccess = repositoryLoginWithTryCatch()


                // • Throw exception after loginJob is started:
//                throw IOException("loginJob coroutine IOException") // repositoryLoginXXX will keep running to completion(!) and parent will catch the exception.
//                throw CancellationException("loginJob coroutine CancellationException")  // repositoryLoginXXX will keep running to completion(!) then parent coroutine will be cancelled.


                println("Login Job completed - ${Thread.currentThread().name}")
                loginState = loginState.copy(statusMessage = "Login Job completed", isLoading = false, isLoggedIn = isLoginSuccess)
                emitStateAllMethodsCurrentCoroutine(loginState)
                yield()

                isLoginSuccess
            }
            yield()  // Allows the `loginJob` coroutine a chance to staIrt, especially if its cancelled right away with `loginJob.cancel()`

            // • Try different cancellation scenarios:
            delay(150)                      // Delay to allow the `loginJob` coroutine to start, and cancel it in middle of processing.
//            loginJob.join()                 // suspends until loginJob completes
//            loginJob.cancelAndJoin()        // cancels loginJob and suspends until loginJob completes
            loginJob.cancel()          // cancels loginJob but does not suspend

            if(!loginJob.isCancelled) {
                println("loginJob is not cancelled, awaiting `job` result...")
                loginJob.await()
                println("loginJob result: ${loginJob.getCompleted()}")
            }

            // This `yield()` allows this `viewModelScope.launch` block to run to the end, in case of `loginJob.cancel()`
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
//                loginSharedFlow.emit(loginState)
//                _loginStateFlow.value = loginState
//                loginChannel.send(loginState)
                emitStateAllMethodsCurrentCoroutine(loginState)
                yield()
            }

            // note: without `loginJob.join()` or `loginJob.await()` this is printed before the login job is finished!
            println("Login finished - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "Login finished", isLoading = false)
//            loginSharedFlow.emit(loginState)
//            _loginStateFlow.value = loginState
//            loginChannel.send(loginState)
            emitStateAllMethodsCurrentCoroutine(loginState)
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
    // Note: Exception is bubbled up to the parent coroutine, and not handled in the child coroutine.
    private suspend fun repositoryLoginThrowExceptionToParent(): Boolean {
        println("Login Repository called - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin called")
        emitStateAllMethodsCurrentCoroutine(loginState)
        delay(100)

        println("login running...")
        loginState = loginState.copy(statusMessage = "RepositoryLogin running...")
        emitStateAllMethodsCurrentCoroutine(loginState)
        delay(100)

        // • Try throwing different exceptions
//            throw IOException("Login failed - IOException")  // thrown to parent
//            throw CancellationException("Login failed - cancelled") // NOT thrown to parent. It simply stops the coroutine.

        val isSuccessLogin = true

        println("RepositoryLogin completed - ${Thread.currentThread().name}")
        loginState = loginState.copy(statusMessage = "RepositoryLogin completed", isLoggedIn = isSuccessLogin)
        emitStateAllMethodsCurrentCoroutine(loginState)
        yield()

        return isSuccessLogin // isSuccess
    }

    // Handles exceptions in the child function using try/catch.
    // Returns true if successful login, or catches exception and returns false.
    // Allows finer control over the exception handling, and allows the parent coroutine to continue.
    // Note: Exception is not bubbled up to parent coroutine.
    private suspend fun repositoryLoginWithTryCatch(): Boolean {
        var isLoginSuccess = false

        return try {
            println("Login Repository called - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "RepositoryLogin called")
            emitStateAllMethodsCurrentCoroutine(loginState)
            delay(100)

            println("login running...")
            loginState = loginState.copy(statusMessage = "RepositoryLogin running...")
            emitStateAllMethodsCurrentCoroutine(loginState)
            delay(100)

            // • Try throwing different exceptions
//                throw IOException("Login failed - IOException") // caught by try/catch. Parent coroutine continues.
//                throw CancellationException("Login failed - cancelled")  // caught by try/catch. Parent coroutine continues.

            isLoginSuccess = true  // simulate a good username/pw login

            println("Login logged in - ${Thread.currentThread().name}")
            loginState = loginState.copy(statusMessage = "RepositoryLogin completed", isLoggedIn = isLoginSuccess)
            emitStateAllMethodsCurrentCoroutine(loginState)
            yield()

            isLoginSuccess // isSuccess

        } catch(e: CancellationException) {  // Must be specifically caught! Otherwise, it just stops the parent coroutine.
            println("Login cancelled")
            loginState = loginState.copy(statusMessage = "RepositoryLogin cancelled", isLoggedIn = false)
            emitStateAllMethodsCurrentCoroutine(loginState)
            yield()

            //throw e  // rethrow exception to be handled by parent coroutine
            false
        } catch(e: Exception) {
            println("Login failed")
            loginState = loginState.copy(statusMessage = "RepositoryLogin failed", isLoggedIn = false)
            emitStateAllMethodsCurrentCoroutine(loginState)
            yield()

            false
        }
    }

}