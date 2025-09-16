package com.finalfinalprojectmedscope

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
private val ButtonColor = Color(0xFF0066FF) // teal color

@Composable
fun RecordingItem(
    recording: Recording,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Recording Name at the top
            Text(
                text = recording.name,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Options row below the name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)) { Text("Play") }
                Button(onClick = onAnalyze, colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)) { Text("Analyze") }
                Button(onClick = onRename, colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)) { Text("Rename") }
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)) { Text("Delete") }
            }
        }
    }
}


