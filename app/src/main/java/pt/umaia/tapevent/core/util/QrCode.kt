package pt.umaia.tapevent.core.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Generates QR code images (ZXing). Used by the ticket and, later, by the organizer check-in. */
object QrCode {
    /** Text stored in a ticket QR code. The organizer scanner reads it back in Phase 2. */
    fun ticketContent(registrationId: String) = "tapevent:$registrationId"

    fun generate(content: String, sizePx: Int = 600): ImageBitmap {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        val pixels = IntArray(sizePx * sizePx) { index ->
            val x = index % sizePx
            val y = index / sizePx
            if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
