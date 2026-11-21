package ai.localcompanion.app.models

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads one pack, file by file:
 * HTTP Range resume into a `.part` file → SHA-256 verify → atomic rename →
 * extract if the file is a .tar.bz2 archive → READY marker.
 * Progress is the byte fraction across all files of the pack.
 */
class PackDownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packId = inputData.getString(PackManager.KEY_PACK_ID)
            ?: return@withContext Result.failure()
        val pack = ModelPacks.byId(packId) ?: return@withContext Result.failure()
        val dir = PackManager.packDir(applicationContext, pack.id).apply { mkdirs() }
        val total = pack.totalBytes.coerceAtLeast(1)
        var doneBytes = 0L

        try {
            setForeground(foregroundInfo(pack, 0f))
            for (file in pack.files) {
                val finalFile = File(dir, file.fileName)
                if (finalFile.exists() && hashOf(finalFile) == file.sha256) {
                    doneBytes += file.sizeBytes
                    report(pack, doneBytes.toFloat() / total)
                    continue
                }
                val part = File(dir, file.fileName + ".part")
                downloadWithResume(file, part) { written ->
                    reportThrottled(pack, (doneBytes + written).toFloat() / total)
                }
                if (hashOf(part) != file.sha256) {
                    part.delete()
                    return@withContext Result.failure()
                }
                part.renameTo(finalFile)
                doneBytes += file.sizeBytes
                report(pack, doneBytes.toFloat() / total)

                if (file.archive) {
                    extractTarBz2(finalFile, dir)
                    finalFile.delete()
                }
            }
            File(dir, PackManager.READY_MARKER).writeText("ok")
            report(pack, 1f)
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private var lastReported = -1f
    private fun reportThrottled(pack: ModelPack, fraction: Float) {
        if (fraction - lastReported >= 0.01f || fraction >= 1f) report(pack, fraction)
    }

    private fun report(pack: ModelPack, fraction: Float) {
        lastReported = fraction
        setProgressAsync(workDataOf(PackManager.KEY_PROGRESS to fraction))
        setForegroundAsync(foregroundInfo(pack, fraction))
    }

    private fun downloadWithResume(file: PackFile, part: File, onBytes: (Long) -> Unit) {
        var existing = if (part.exists()) part.length() else 0L
        if (existing > file.sizeBytes) {
            part.delete()
            existing = 0L
        }
        val connection = (URL(file.url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        val code = connection.responseCode
        val append = code == HttpURLConnection.HTTP_PARTIAL
        if (!append && code != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP $code for ${file.fileName}")
        }
        if (!append) existing = 0L
        var written = existing
        connection.inputStream.use { input ->
            FileOutputStream(part, append).use { output ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    written += read
                    onBytes(written)
                }
            }
        }
        if (written != file.sizeBytes) {
            throw IOException("Short download for ${file.fileName}: $written of ${file.sizeBytes}")
        }
    }

    private fun hashOf(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarBz2(archive: File, destDir: File) {
        val destCanonical = destDir.canonicalPath
        TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val out = File(destDir, entry.name)
                require(out.canonicalPath.startsWith(destCanonical)) {
                    "Archive entry escapes target dir: ${entry.name}"
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output -> tar.copyTo(output) }
                }
                entry = tar.nextEntry
            }
        }
    }

    private fun foregroundInfo(pack: ModelPack, fraction: Float): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading ${pack.title}")
            .setContentText("${(fraction * 100).toInt()}% · ~${pack.approxSizeMb} MB")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, (fraction * 100).toInt(), false)
            .build()
        return ForegroundInfo(pack.id.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "model_downloads"
    }
}
