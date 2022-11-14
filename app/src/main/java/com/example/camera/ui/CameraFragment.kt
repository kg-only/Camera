package com.example.camera.ui

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.example.camera.R
import com.example.camera.databinding.FragmentCameraBinding
import com.example.camera.viewmodel.CameraViewModel

class CameraFragment : Fragment() {
    private var _cameraBinding: FragmentCameraBinding? = null
    private val cameraBinding get() = _cameraBinding!!

    private lateinit var surfaceTexture: SurfaceTexture

    private val cameraViewModel: CameraViewModel by activityViewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _cameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        val width = requireActivity().windowManager.defaultDisplay.width
        var height = width / 3 * 4
        addTextureView(width, height)
        cameraBinding.btnChangeCamera.setOnClickListener {
            it.isEnabled = false
            cameraViewModel.changeCamera(
                surfaceTexture,
                cameraBinding.textPreviewSize.text.toString()
            )
            it.postDelayed({ it.isEnabled = true }, 500)
        }

        var isCaptureVideo = false
        cameraBinding.btnCaptureImage.setOnClickListener {
            it.isEnabled = false
            if (cameraBinding.textCameraModel.text == "8") {
                cameraViewModel.takePicture()
            } else {
                isCaptureVideo = if (!isCaptureVideo) {
                    it.setBackgroundResource(R.drawable.capture_stop)
                    cameraViewModel.videoStart()
                    true
                } else {
                    it.setBackgroundResource(R.drawable.capture_start)
                    cameraViewModel.videoStop()
                    false
                }
            }
            it.postDelayed({ it.isEnabled = true }, 500)
        }

        cameraBinding.btnChangeModel.setOnClickListener {
            val textView = cameraBinding.textCameraModel
            val button = cameraBinding.btnChangeModel
            cameraViewModel.closeCamera()
            cameraBinding.preLayout.removeAllViews()
            if (textView.text == "6") {
                Toast.makeText(requireContext(), "5", Toast.LENGTH_SHORT).show()
                button.setBackgroundResource(R.drawable.model_video)
                textView.text = "1"
                cameraBinding.textPreviewSize.text = "9:16"
                height = width / 9 * 16
            } else {
                Toast.makeText(requireContext(), "ds", Toast.LENGTH_SHORT).show()
                button.setBackgroundResource(R.drawable.model_picture)
                textView.text = "2"
                cameraBinding.textPreviewSize.text = "3:4"
                height = width / 9 * 12
            }
            addTextureView(width, height)
        }

        cameraBinding.btnChangeSize.setOnClickListener {
            val textView = cameraBinding.textPreviewSize
            cameraViewModel.closeCamera()
            if (textView.text == "3:4") {
                Toast.makeText(requireContext(), "9:16", Toast.LENGTH_SHORT).show()
                textView.text = "9:16"
                height = width / 9 * 16
                cameraBinding.preLayout.removeAllViews()
            } else {
                if (cameraBinding.textCameraModel.text == "test") {
                    Toast.makeText(requireContext(), "3:4", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    Toast.makeText(requireContext(), "3:4", Toast.LENGTH_SHORT).show()
                    textView.text = "3:4"
                    height = width / 9 * 12
                    cameraBinding.preLayout.removeAllViews()
                }
            }
            addTextureView(width, height)
        }
        cameraBinding.btnGallery.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                R.id.action_imageCameraFragment_to_galleryFragment
            )
        }

        return cameraBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addTextureView(width: Int, height: Int) {
        val textureView = TextureView(requireContext())
        val params = ViewGroup.LayoutParams(width, height)
        textureView.layoutParams = params
        cameraBinding.preLayout.addView(textureView)
        textureView.surfaceTextureListener = surfaceTextureListener
        textureView.setOnTouchListener(onTouchListener)
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            mSurfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            surfaceTexture = mSurfaceTexture
            val ratio = cameraBinding.textPreviewSize.text.toString()
            cameraViewModel.initCameraInfo(surfaceTexture, ratio)
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = false
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { _, event ->
        cameraViewModel.onTouchEvent(event)
        true
    }

    override fun onResume() {
        super.onResume()
        cameraBinding.preLayout.removeAllViews()
        val width = requireActivity().windowManager.defaultDisplay.width
        val height = width / 3 * 4
        addTextureView(width, height)
        val orientationEventListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                cameraViewModel.setOrientation(orientation)
            }
        }
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        cameraViewModel.closeCamera()
    }

    override fun onDestroy() {
        cameraViewModel.releaseThread()
        _cameraBinding = null
        super.onDestroy()
    }
}
