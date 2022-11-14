package com.example.camera.util

import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.camera.databinding.ActivityCameraBinding
import com.example.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CameraActivity : AppCompatActivity() {

    private lateinit var activityCameraBinding: ActivityCameraBinding

    private val cameraViewModel: CameraViewModel by viewModels()

    companion object {
        const val FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private lateinit var orientationEventListener: OrientationEventListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(activityCameraBinding.root)

        lifecycleScope.launch(Dispatchers.IO) {
            FileUtil.getFiles(applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        activityCameraBinding.fragmentContainer.systemUiVisibility = FLAGS_FULLSCREEN
    }
}