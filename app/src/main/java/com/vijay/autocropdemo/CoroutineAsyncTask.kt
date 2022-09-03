package com.vijay.autocropdemo

import kotlinx.coroutines.*

enum class Status {
    Pending,
    Running,
    Finished
}

abstract class CoroutineAsyncTask<Params, Progress, Result>(
    private val taskName: String
) {

    private lateinit var coroutineScope: CoroutineScope
    companion object {
        private var coroutineDispatcher: CoroutineDispatcher? = null
    }

    abstract fun doInBackground(vararg params: Params?): Result
    open fun onPreExecute() {}
    open fun onPostExecute(result: Result?) {}
    open fun onProgressUpdate(vararg values: Progress?) {}
    open fun onCancelled(result: Result?) {}
    protected var isCancelled = false
    var status = Status.Pending
    var preJob: Job? = null
    var backgroundJob: Deferred<Result>? = null


    fun execute(vararg params: Params?) {
        execute(Dispatchers.Default, *params)
    }

    private fun execute(dispatcher: CoroutineDispatcher, vararg params: Params?) {
        coroutineScope = CoroutineScope(dispatcher)
        if (status != Status.Pending) {
            when (status) {
                Status.Running -> {

                }
                Status.Finished -> {

                }
                else -> {}
            }
        }

        status = Status.Running

        coroutineScope.launch(Dispatchers.Main) {
            preJob = launch(Dispatchers.Main) {
                onPreExecute()
                backgroundJob = async(dispatcher) {
                    doInBackground(*params)
                }
            }
            preJob!!.join()
            if (!isCancelled) {
                withContext(dispatcher) {
                    onPostExecute(backgroundJob!!.await())
                    status = Status.Finished
                }
            }
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean) {
        if (preJob == null || backgroundJob == null) {
            return
        }

        if (mayInterruptIfRunning || (!preJob!!.isActive && !backgroundJob!!.isActive)) {
            isCancelled = true
            status = Status.Finished
            if (backgroundJob!!.isCompleted) {
                coroutineScope.launch(Dispatchers.Main) {
                    onCancelled(backgroundJob!!.await())
                }
            }
            preJob?.cancel(CancellationException())
            backgroundJob?.cancel(CancellationException())
        }

    }

    fun publishProgress(vararg progress: Progress?) {
        coroutineScope.launch(Dispatchers.Main) {
            if (!isCancelled) {
                onProgressUpdate(*progress)
            }
        }
    }


}