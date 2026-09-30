package com.twocall.chat.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.twocall.chat.ui.theme.DarkPrimary

@Composable
fun DeliveryStatusTick(status: String, modifier: Modifier = Modifier) {
    when (status) {
        "PENDING" -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Pending",
                modifier = modifier.size(13.dp),
                tint = Color.Gray
            )
        }
        "SENT" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                modifier = modifier.size(13.dp),
                tint = Color.Gray
            )
        }
        "DELIVERED" -> {
            Row(modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = Color.LightGray
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    modifier = Modifier.size(13.dp),
                    tint = Color.LightGray
                )
            }
        }
        "READ" -> {
            Row(modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = DarkPrimary
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Read",
                    modifier = Modifier.size(13.dp),
                    tint = DarkPrimary
                )
            }
        }
    }
}
