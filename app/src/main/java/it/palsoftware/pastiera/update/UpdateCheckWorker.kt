package it.palsoftware.pastiera.update

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Retained only so older scheduled work can be cancelled safely. */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result = Result.success()

    companion object {
        private const val UNIQUE_WORK_NAME = "pastiera_update_check"

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        /** Legacy callers can only cancel; Q25 has no configured update feed. */
        fun schedule(context: Context) = cancel(context)
    }
}
