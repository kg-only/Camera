package com.example.camera.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.MotionEventCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.camera.R
import com.example.camera.databinding.FragmentShowBinding
import kotlin.math.min
import kotlin.math.sqrt

class ShowFragment : Fragment() {
    private var _showFragmentBinding: FragmentShowBinding? = null
    private val showFragmentBinding get() = _showFragmentBinding!!

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _showFragmentBinding = FragmentShowBinding.inflate(inflater, container, false)
        val showFragmentArgs = ShowFragmentArgs.fromBundle(requireArguments())

        val path = showFragmentArgs.path
        val type = showFragmentArgs.type

        val imageView = showFragmentBinding.showPicture
        val layout = showFragmentBinding.showLayout

        var bitmap: Bitmap? = null
        if (type == "jpg") {
            bitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(bitmap)
            imageView.setFitCenter()
            imageView.setOnTouchListener { view, event ->
                onTouch(view, event)
                true
            }
        } else if (type == "mp4") {
            bitmap =
                ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)!!
            imageView.setImageBitmap(bitmap)
            imageView.setFitCenter()
            val videoStart = Button(requireContext())
            videoStart.setBackgroundResource(R.drawable.video_start)
            layout.addView(videoStart)
            videoStart.setOnClickListener {
                val bundle = VideoFragmentArgs.Builder(path).build().toBundle()

                Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                    R.id.action_showFragment_to_videoFragment, bundle
                )
            }
        }

        return showFragmentBinding.root
    }

    private var distance = 1.0
    private var startPointF = PointF()
    private var startMatrix = Matrix()
    private var endMatrix = Matrix()
    private var MODE = 0
    private fun onTouch(v: View, event: MotionEvent) {
        val action = MotionEventCompat.getActionMasked(event)
        val view = v as ImageView
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                startMatrix.set(view.imageMatrix)
                startPointF.set(event.x, event.y)
                MODE = 1
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                startMatrix.set(endMatrix)
                distance = getDistance(event)
                MODE = 2
            }
            MotionEvent.ACTION_MOVE -> {
                if (MODE == 2) {
                    val scale = (getDistance(event) / distance).toFloat()
                    endMatrix.set(startMatrix)
                    endMatrix.postScale(scale, scale, startPointF.x, startPointF.y)
                } else if (MODE == 1) {
                    endMatrix.set(startMatrix)
                    endMatrix.postTranslate(event.x - startPointF.x, event.y - startPointF.y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> MODE = 0
        }
        view.imageMatrix = endMatrix
    }

    private fun getDistance(event: MotionEvent): Double {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(((x * x + y * y).toDouble()))
    }

    private fun ImageView.setFitCenter() {
        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight
        val width = requireActivity().windowManager.defaultDisplay.width
        val height = requireActivity().windowManager.defaultDisplay.height
        val widthScale = width.toFloat() / imageWidth.toFloat()
        val heightScale = height.toFloat() / imageHeight.toFloat()
        val minScale = min(widthScale, heightScale)
        val targetWidth = minScale * imageWidth
        val targetHeight = minScale * imageHeight
        val matrix = imageMatrix
        matrix.setScale(minScale, minScale)
        matrix.postTranslate((width - targetWidth) / 2, (height - targetHeight) / 2)
        imageMatrix = matrix
        endMatrix = matrix
    }

    override fun onDestroy() {
        super.onDestroy()
        _showFragmentBinding = null
    }
}


