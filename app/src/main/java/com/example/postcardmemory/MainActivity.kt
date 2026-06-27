package com.example.postcardmemory

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PostCardMemoryApp() }
    }
}

@Composable
private fun PostCardMemoryApp() {
    val context = LocalContext.current
    val memories = remember { mutableStateListOf<Uri>() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingCameraUri?.takeIf { success }?.let { source ->
            context.contentResolver.openInputStream(source)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bitmap ->
                    savePostCardStamp(context, bitmap)?.let(memories::add)
                }
            }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            createCameraImageUri(context)?.also { uri ->
                pendingCameraUri = uri
                takePicture.launch(uri)
            }
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bitmap ->
                    savePostCardStamp(context, bitmap)?.let(memories::add)
                }
            }
        }
    }

    MaterialTheme {
        Surface(color = Cream) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Header()
                StampPreviewCard()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrutalButton("📸 카메라") { cameraPermission.launch(Manifest.permission.CAMERA) }
                    BrutalButton("🖼️ 갤러리") { pickImage.launch("image/*") }
                }
                Text(
                    text = "수집한 추억 도감 ${memories.size}장",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(memories) { uri -> MemoryTile(uri) }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Pink, RoundedCornerShape(24.dp))
            .border(4.dp, Ink, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text("POST CARD MEMORY", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("포켓몬처럼 귀여운 순간을 우표 카드로 수집해요", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StampPreviewCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Yellow),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.border(4.dp, Ink, RoundedCornerShape(28.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).padding(18.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(color = androidx.compose.ui.graphics.Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f))
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFF111111),
                    style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
                )
                repeat(8) { index ->
                    drawCircle(color = androidx.compose.ui.graphics.Color(0xFFFF7BAC), radius = 12f, center = Offset(34f + index * 58f, 22f))
                }
            }
            Text("사진이 이 우표 프레임에 딱 맞게 저장돼요", fontWeight = FontWeight.Black, color = Ink)
        }
    }
}

@Composable
private fun BrutalButton(label: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(containerColor = Mint, contentColor = Ink),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp),
        modifier = Modifier.border(3.dp, Ink, RoundedCornerShape(18.dp))
    ) { Text(label, fontWeight = FontWeight.Black) }
}

@Composable
private fun MemoryTile(uri: Uri) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .border(3.dp, Ink, RoundedCornerShape(18.dp))
            .padding(8.dp)
    ) {
        Image(
            bitmap = BitmapFactory.decodeStream(LocalContext.current.contentResolver.openInputStream(uri)).asImageBitmap(),
            contentDescription = "post card memory",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.size(8.dp))
        Text("Memory stamp", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

private fun createCameraImageUri(context: android.content.Context): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "camera_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

private fun savePostCardStamp(context: android.content.Context, source: Bitmap): Uri? {
    val width = 900
    val height = 1200
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(Color.rgb(255, 244, 214))
    paint.color = Color.WHITE
    canvas.drawRoundRect(RectF(48f, 48f, 852f, 1152f), 44f, 44f, paint)
    val crop = centerCrop(source, 720, 900)
    canvas.drawBitmap(source, crop, RectF(90f, 130f, 810f, 1030f), paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 18f
    paint.color = Color.rgb(17, 17, 17)
    canvas.drawRoundRect(RectF(48f, 48f, 852f, 1152f), 44f, 44f, paint)
    paint.strokeWidth = 8f
    paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(22f, 18f), 0f)
    canvas.drawRoundRect(RectF(76f, 76f, 824f, 1124f), 34f, 34f, paint)
    paint.pathEffect = null
    paint.style = Paint.Style.FILL
    paint.textSize = 48f
    paint.fakeBoldText = true
    canvas.drawText("POST CARD MEMORY", 118f, 1100f, paint)

    val name = "postcard_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PostCardMemory")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    context.contentResolver.openOutputStream(uri)?.use { output.compress(Bitmap.CompressFormat.JPEG, 94, it) }
    return uri
}

private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Rect {
    val targetRatio = targetWidth.toFloat() / targetHeight
    val sourceRatio = source.width.toFloat() / source.height
    return if (sourceRatio > targetRatio) {
        val newWidth = (source.height * targetRatio).toInt()
        val left = (source.width - newWidth) / 2
        Rect(left, 0, left + newWidth, source.height)
    } else {
        val newHeight = (source.width / targetRatio).toInt()
        val top = (source.height - newHeight) / 2
        Rect(0, top, source.width, top + newHeight)
    }
}

private val Cream = androidx.compose.ui.graphics.Color(0xFFFFF4D6)
private val Pink = androidx.compose.ui.graphics.Color(0xFFFF7BAC)
private val Yellow = androidx.compose.ui.graphics.Color(0xFFFFD447)
private val Mint = androidx.compose.ui.graphics.Color(0xFF7FFFD4)
private val Ink = androidx.compose.ui.graphics.Color(0xFF111111)
