package com.twocall.chat.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.twocall.chat.ChatApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ProfileEditor(app: ChatApplication) {
    var name by remember { mutableStateOf(app.keyStoreManager.getMyProfileName()) }
    var image by remember { mutableStateOf(app.keyStoreManager.getMyProfileImage()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                image = withContext(Dispatchers.IO) {
                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
                            val scale = 256f / maxOf(info.size.width, info.size.height)
                            decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        app.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                        options.inJustDecodeBounds = false
                        options.inSampleSize = 1
                        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) options.inSampleSize *= 2
                        app.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                            ?: error("Cannot read image")
                    }
                    val scale = minOf(1f, 256f / maxOf(bitmap.width, bitmap.height))
                    val resized = Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()), maxOf(1, (bitmap.height * scale).toInt()), true)
                    val bytes = ByteArrayOutputStream().use { out ->
                        resized.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        out.toByteArray()
                    }
                    if (resized !== bitmap) resized.recycle()
                    bitmap.recycle()
                    require(bytes.size <= 75000) { "Choose a smaller image" }
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
                status = null
            } catch (e: Exception) { status = "Could not load photo. Please choose another image." }
            finally { busy = false }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your profile", style = MaterialTheme.typography.titleMedium)
            ZippyAvatar(name.ifBlank { "You" }, size = 72.dp, showOnlineDot = false, imageBase64 = image)
            OutlinedTextField(value = name, onValueChange = { if (it.length <= 64) name = it },
                label = { Text("Profile name") }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth())
            Row {
                TextButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text("Choose photo") }
                if (image != null) TextButton(onClick = { image = null }, enabled = !busy) { Text("Remove photo") }
            }
            Button(enabled = !busy && name.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    try {
                        status = if (app.chatRepository.updateMyProfile(name, image)) "Profile saved and shared with your partners."
                            else "Saved on this device. Sharing will retry when connected."
                    } catch (e: Exception) { status = "Could not save profile. Please retry." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Saving…" else "Save profile") }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
