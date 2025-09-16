package com.finalfinalprojectmedscope

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.*

class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var currentFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private val recordingsDir: File = File(context.filesDir, "recordings").apply {
        if (!exists()) mkdirs()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(fileName: String): Recording {
        val file = File(recordingsDir, "$fileName.wav")
        currentFile = file

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            writeWavFile(file, bufferSize, sampleRate, channelConfig, audioFormat)
        }.also { it.start() }

        return Recording(name = file.name, path = file.absolutePath)
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }

    private fun writeWavFile(
        file: File,
        bufferSize: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ) {
        val data = ByteArray(bufferSize)
        FileOutputStream(file).use { fos ->
            val bos = BufferedOutputStream(fos)
            val dos = DataOutputStream(bos)

            // Placeholder for WAV header
            val header = ByteArray(44)
            dos.write(header)

            var totalAudioLen = 0L

            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    dos.write(data, 0, read)
                    totalAudioLen += read
                }
            }

            dos.flush()
            fos.channel.position(0) // go back and write header
            fos.write(createWavHeader(totalAudioLen, sampleRate, 1, 16))
        }
    }

    private fun createWavHeader(totalAudioLen: Long, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        val littleEndian = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        littleEndian.put("RIFF".toByteArray())
        littleEndian.putInt(totalDataLen.toInt())
        littleEndian.put("WAVE".toByteArray())
        littleEndian.put("fmt ".toByteArray())
        littleEndian.putInt(16)
        littleEndian.putShort(1.toShort())
        littleEndian.putShort(channels.toShort())
        littleEndian.putInt(sampleRate)
        littleEndian.putInt(byteRate)
        littleEndian.putShort((channels * bitsPerSample / 8).toShort())
        littleEndian.putShort(bitsPerSample.toShort())
        littleEndian.put("data".toByteArray())
        littleEndian.putInt(totalAudioLen.toInt())

        littleEndian.rewind()
        littleEndian.get(header)
        return header
    }

    fun playRecording(recording: Recording) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(recording.path)
            prepare()
            start()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun deleteRecording(recording: Recording) {
        val file = File(recording.path)
        if (file.exists()) file.delete()
    }

    fun getRecordings(): List<Recording> {
        return recordingsDir.listFiles()
            ?.map { file -> Recording(name = file.name, path = file.absolutePath) }
            ?.sortedByDescending { it.name }
            ?: emptyList()
    }

    fun renameRecording(recording: Recording, newName: String): Recording? {
        val oldFile = File(recording.path)
        if (!oldFile.exists()) return null

        val newFile = File(oldFile.parentFile, "$newName.wav")
        return if (oldFile.renameTo(newFile)) {
            Recording(name = newFile.name, path = newFile.absolutePath)
        } else {
            null
        }
    }

}
