package com.finalfinalprojectmedscope

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import org.json.JSONObject

@Composable
fun MLResultCard(
    resultJson: String,
    onClose: () -> Unit,
    buttonColor: Color
) {
    val scrollState = rememberScrollState()

    var label: String
    var healthyPercent: Int
    var abnormalPercent: Int
    var confidence: Int

    try {
        val json = JSONObject(resultJson)

        val lbl = json.optString("label", "Unknown")
        val conf = (json.optDouble("confidence", 0.0) * 100).toInt()

        val probs = json.optJSONObject("probabilities") ?: JSONObject()
        val healthy = probs.optDouble("healthy", 0.0)
        val wheezes = probs.optDouble("wheezes", 0.0)
        val crackles = probs.optDouble("crackles", 0.0)
        val cracklesAndWheezes = probs.optDouble("crackles_and_wheezes", 0.0)

        label = lbl
        confidence = conf
        healthyPercent = (healthy * 100).toInt()
        abnormalPercent = ((wheezes + crackles + cracklesAndWheezes) * 100).toInt()

    } catch (e: Exception) {
        label = "Unknown"
        healthyPercent = 0
        abnormalPercent = 0
        confidence = 0
    }

    val isHealthy = label.equals("healthy", ignoreCase = true)
    val labelText = if (isHealthy) "✔️ Normal - Healthy Sounds" else "❌ Abnormal - Needs Attention"
    val labelColor = if (isHealthy) Color(0xFF06D00E) else Color(0xFFFF0000)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = labelText,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(labelColor, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Overall confidence
            Text("Overall Confidence: $confidence%", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = confidence / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(vertical = 6.dp),
                color = labelColor
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Gray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Detailed probabilities
            Text("Detailed Probabilities", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))

            Text("Normal: $healthyPercent%")
            LinearProgressIndicator(
                progress = healthyPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF0092FF)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Abnormal: $abnormalPercent%")
            LinearProgressIndicator(
                progress = abnormalPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF0092FF)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Gray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Recommendation
            val recommendation = if (isHealthy) {
                "Normal heart/lung sounds detected. Continue regular monitoring."
            } else {
                "Abnormal sounds detected. Please consult a doctor."
            }
            Text(recommendation, fontSize = 14.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF858F9F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = Color.White)
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var recorder: AudioRecorder
    private val client = OkHttpClient()
    private val ButtonColor = Color(0xFF0066FF)

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorder = AudioRecorder(this)

        setContent {
            MaterialTheme {
                var isRecording by remember { mutableStateOf(false) }
                var mlResult by remember { mutableStateOf<String?>(null) }

                val recordings = remember { mutableStateListOf<Recording>().apply {
                    addAll(recorder.getRecordings())
                } }

                // rename dialog state
                var renameTarget by remember { mutableStateOf<Recording?>(null) }
                var newName by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // ML Result Card
                    mlResult?.let { result ->
                        MLResultCard(
                            resultJson = result,
                            onClose = { mlResult = null },
                            buttonColor = ButtonColor
                        )
                    }

                    // Record / Stop button
                    Button(
                        onClick = {
                            if (!isRecording) {
                                val fileName = "Recording_${System.currentTimeMillis()}.wav"
                                val recording = recorder.startRecording(fileName)
                                recordings.add(recording)
                            } else {
                                recorder.stopRecording()
                            }
                            isRecording = !isRecording
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)
                    ) {
                        Text(if (isRecording) "Stop Recording" else "Start Recording")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recordings list
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(recordings) { recording ->
                            RecordingItem(
                                recording = recording,
                                onPlay = { recorder.playRecording(recording) },
                                onDelete = {
                                    recorder.deleteRecording(recording)
                                    recordings.remove(recording)
                                },
                                onAnalyze = {
                                    analyzeRecording(recording) { result ->
                                        mlResult = result
                                    }
                                },
                                onRename = {
                                    renameTarget = recording
                                    newName = recording.name.removeSuffix(".wav")
                                }
                            )
                        }
                    }
                }

                // Rename Dialog
                if (renameTarget != null) {
                    AlertDialog(
                        onDismissRequest = { renameTarget = null },
                        title = { Text("Rename Recording") },
                        text = {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("New name") }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    renameTarget?.let { rec ->
                                        val renamed = recorder.renameRecording(rec, newName)
                                        if (renamed != null) {
                                            val index = recordings.indexOf(rec)
                                            if (index != -1) recordings[index] = renamed
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Rename failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    renameTarget = null
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            Button(onClick = { renameTarget = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    /** Uploads audio file to ML endpoint and returns result via callback */
    private fun analyzeRecording(recording: Recording, callback: (String) -> Unit) {
        val file = File(recording.path)
        if (!file.exists()) return

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("audio/wav".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://h3rsh-resp2.hf.space/predict")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    callback("Analysis failed: ${e.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Analysis failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "No response"
                Log.d("MLResponse", responseBody)
                runOnUiThread { callback(responseBody) }
            }
        })
    }
}
