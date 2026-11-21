package ai.localcompanion.app.models

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File

/** Enqueues pack downloads and answers "is it on disk yet". */
object PackManager {

    const val READY_MARKER = "READY"
    const val KEY_PACK_ID = "pack_id"
    const val KEY_PROGRESS = "progress"

    fun packDir(context: Context, packId: String): File =
        File(context.filesDir, "packs/$packId")

    fun isReady(context: Context, pack: ModelPack): Boolean =
        File(packDir(context, pack.id), READY_MARKER).exists()

    fun workName(packId: String): String = "pack-$packId"

    fun enqueue(context: Context, pack: ModelPack) {
        val request = OneTimeWorkRequestBuilder<PackDownloadWorker>()
            .setInputData(workDataOf(KEY_PACK_ID to pack.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        // KEEP: tapping Download twice never restarts a running download.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(pack.id), ExistingWorkPolicy.KEEP, request)
    }
}
