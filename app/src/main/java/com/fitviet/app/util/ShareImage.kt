package com.fitviet.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Gate D3 — saves [bitmap] as a PNG under `cacheDir/images/` (matches `file_paths.xml`'s
 * `<cache-path name="images" path="images/" />` entry) and returns a `content://` URI for it via
 * [FileProvider] — a raw `file://` URI can't be granted to another app's share target since
 * Android 7 (`FileUriExposedException`). Overwrites the same filename each call rather than
 * accumulating one file per share (this is a throwaway export, not something the user manages).
 */
fun saveBitmapForSharing(context: Context, bitmap: Bitmap, fileName: String): android.net.Uri {
    val imagesDir = File(context.cacheDir, "images")
    check(imagesDir.exists() || imagesDir.mkdirs()) { "Could not create $imagesDir" }
    val file = File(imagesDir, fileName)
    FileOutputStream(file).use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG compression failed" } }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Same "share via the system chooser" idiom [com.fitviet.app.ui.programs.WeeklyScheduleScreen]'s
 * `ExportButton` already uses for text — this is the image-attachment equivalent. */
fun shareImageIntent(imageUri: android.net.Uri, chooserTitle: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(sendIntent, chooserTitle)
}
