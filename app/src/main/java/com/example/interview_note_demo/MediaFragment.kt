package com.example.interview_note_demo

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException

class MediaFragment : Fragment() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var isPlaying = false

    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_media, container, false)

        btnRecord = view.findViewById(R.id.btnRecord)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnStop = view.findViewById(R.id.btnStop)

        btnRecord.setOnClickListener { toggleRecording() }
        btnPlay.setOnClickListener { togglePlayback() }
        btnStop.setOnClickListener { stopPlayback() }

        updateButtonStates()

        return view
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            checkPermissionAndRecord()
        }
    }

    private fun checkPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        // Create audio file in app's external files directory
        val audioDir = requireContext().getExternalFilesDir(null)
        val audioFile = File(audioDir, "recording_${System.currentTimeMillis()}.3gp")
        audioFilePath = audioFile.absolutePath

        try {
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                prepare()
                start()
            }
            isRecording = true
            updateButtonStates()
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            updateButtonStates()
            Toast.makeText(context, "Recording saved to:\n$audioFilePath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error stopping recording: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        if (audioFilePath == null || !File(audioFilePath!!).exists()) {
            Toast.makeText(context, "No recording found. Please record audio first.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                start()
            }
            isPlaying = true
            updateButtonStates()
            Toast.makeText(context, "Playing audio", Toast.LENGTH_SHORT).show()

            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                updateButtonStates()
                Toast.makeText(context, "Playback completed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Playback failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updateButtonStates()
        Toast.makeText(context, "Playback paused", Toast.LENGTH_SHORT).show()
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isPlaying = false
            updateButtonStates()
            Toast.makeText(context, "Playback stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error stopping playback: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun updateButtonStates() {
        if (!::btnRecord.isInitialized) return

        btnRecord.text = if (isRecording) "Stop Recording" else "Start Recording"
        btnPlay.text = if (isPlaying) "Pause" else "Play"
        btnPlay.isEnabled = audioFilePath != null && !isRecording
        btnStop.isEnabled = isPlaying
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
        if (isPlaying) {
            stopPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
        mediaRecorder = null
        mediaPlayer = null
    }
}