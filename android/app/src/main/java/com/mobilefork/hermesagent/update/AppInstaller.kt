package com.mobilefork.hermesagent.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

enum class InstallStart {
    /** The system installer is open and asks the user to confirm. */
    Started,

    /** Android needs the user to allow installs from this app first. */
    NeedsPermission,

    /** No installer could be opened. */
    Unavailable,
}

/** Hands a downloaded APK to the system installer. The installer, not this app, checks the signature. */
object AppInstaller {
    fun install(context: Context, apk: File): InstallStart {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(settings)
                InstallStart.NeedsPermission
            } catch (_: ActivityNotFoundException) {
                InstallStart.Unavailable
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(view)
            InstallStart.Started
        } catch (_: ActivityNotFoundException) {
            InstallStart.Unavailable
        }
    }
}
