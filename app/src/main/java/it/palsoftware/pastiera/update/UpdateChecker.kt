package it.palsoftware.pastiera.update

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import it.palsoftware.pastiera.R
import it.palsoftware.pastiera.SettingsManager

internal data class UpdateCheckResult(
    val successful: Boolean,
    val hasAnnouncement: Boolean = false,
    val releaseTag: String? = null,
    val displayName: String? = null,
    val releasePageUrl: String? = null,
    val downloadUrl: String? = null,
    val isNightlyUpdate: Boolean = false,
    val isPastieraStableUpdate: Boolean = false,
    val followUpAnnouncement: UpdateCheckResult? = null
)

internal fun combineStableUpdateResults(
    pastiera: UpdateCheckResult,
    successor: UpdateCheckResult
): UpdateCheckResult {
    val successful = pastiera.successful && successor.successful
    return when {
        successor.hasAnnouncement -> successor.copy(
            successful = successful,
            followUpAnnouncement = pastiera.takeIf { it.hasAnnouncement }
        )
        pastiera.hasAnnouncement -> pastiera.copy(successful = successful)
        else -> UpdateCheckResult(successful = successful)
    }
}

internal fun combineNightlyUpdateResults(
    pastiera: UpdateCheckResult,
    successor: UpdateCheckResult
): UpdateCheckResult = combineStableUpdateResults(pastiera, successor)

/** Kept for compatibility with the existing announcement UI test; production has no callers. */
fun showUpdateDialog(
    context: Context,
    releaseTag: String,
    displayName: String,
    releasePageUrl: String?,
    onClosed: () -> Unit = {}
) {
    val builder = AlertDialog.Builder(context)
        .setTitle(R.string.successor_dialog_title)
        .setMessage(context.getString(R.string.successor_dialog_message, displayName))
        .setNeutralButton(R.string.successor_dialog_later) { _, _ ->
            SettingsManager.addDismissedRelease(context, releaseTag)
            onClosed()
        }
    releasePageUrl?.let { url ->
        builder.setPositiveButton(R.string.successor_dialog_open_release) { _, _ ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onClosed()
        }
    }
    val dialog = builder.create()
    dialog.setOnCancelListener { onClosed() }
    dialog.show()
}