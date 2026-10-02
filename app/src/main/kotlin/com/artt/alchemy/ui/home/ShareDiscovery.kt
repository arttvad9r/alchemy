package com.artt.alchemy.ui.home

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.artt.alchemy.R
import com.artt.alchemy.ui.theme.Gold
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SHARED_DIR = "shared"
private const val SHARED_FILE = "discovery.png"
private const val PICTURE_PADDING_SHARE = 0.08f
private const val SCRIM_ALPHA = 165
private const val SIGNATURE_SIZE_SHARE = 0.09f

/** The store page players are sent to from a shared discovery. */
fun storeLink(context: Context): String = "https://www.rustore.ru/catalog/app/${context.packageName}"

/** Hands the discovery card, set on the scene's background, to the system share sheet as a PNG with [text]. */
suspend fun shareDiscovery(context: Context, card: ImageBitmap, @DrawableRes backgroundRes: Int, text: String) {
    val uri = withContext(Dispatchers.IO) {
        val file = File(File(context.cacheDir, SHARED_DIR).apply { mkdirs() }, SHARED_FILE)
        val picture = discoveryPicture(context, card, backgroundRes)
        file.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The chooser shows the picture as a preview only when it can read it through the clip.
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, null))
}

/** The card over the darkened scene, signed with the game's name so the picture says where it came from on its own. */
private fun discoveryPicture(context: Context, card: ImageBitmap, @DrawableRes backgroundRes: Int): Bitmap {
    // The captured card may live on the GPU; a software canvas can only draw a copy.
    val content = card.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    val padding = (content.width * PICTURE_PADDING_SHARE).toInt()
    val signature = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.underdog)
        textSize = content.width * SIGNATURE_SIZE_SHARE
        color = Gold.toArgb()
        textAlign = Paint.Align.CENTER
    }
    val signatureHeight = (signature.fontSpacing + padding).toInt()
    val picture = createBitmap(content.width + padding * 2, content.height + padding * 2 + signatureHeight)
    val canvas = Canvas(picture)
    BitmapFactory.decodeResource(context.resources, backgroundRes)?.let { background ->
        // Centre-crop, like the scene behind the game.
        val scale = max(picture.width.toFloat() / background.width, picture.height.toFloat() / background.height)
        val cropWidth = (picture.width / scale).toInt()
        val cropHeight = (picture.height / scale).toInt()
        val left = (background.width - cropWidth) / 2
        val top = (background.height - cropHeight) / 2
        canvas.drawBitmap(background, Rect(left, top, left + cropWidth, top + cropHeight), Rect(0, 0, picture.width, picture.height), null)
    }
    canvas.drawColor(Color.argb(SCRIM_ALPHA, 0, 0, 0))
    canvas.drawBitmap(content, padding.toFloat(), padding.toFloat(), null)
    canvas.drawText(context.getString(R.string.app_name), picture.width / 2f, picture.height - padding - signature.descent(), signature)
    return picture
}
