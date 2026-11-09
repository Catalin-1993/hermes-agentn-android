package com.mobilefork.hermesagent.ui.chat

import androidx.core.content.FileProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The camera writes into a file of ours through the file provider; that folder must be declared. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CameraFileProviderTest {
    @Test
    fun theCameraFolderIsServedByTheFileProvider() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(File(context.cacheDir, "hermes-camera").apply { mkdirs() }, "camera-1.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        assertTrue(uri.toString().startsWith("content://${context.packageName}.files/"))
        assertTrue(uri.toString().endsWith("camera-1.jpg"))
    }
}
