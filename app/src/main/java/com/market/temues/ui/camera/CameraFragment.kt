package com.market.temues.ui.camera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.market.temues.R
import com.market.temues.databinding.FragmentCameraBinding
import java.io.File

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashActivado = false
    private var uriFotoCapturada: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iniciarCamara()
        configurarZoomPreview()
        configurarZoomFoto()

        binding.btnCapturar.setOnClickListener { capturarFoto() }
        binding.btnCambiarCamara.setOnClickListener { cambiarCamara() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnCerrarCamara.setOnClickListener { findNavController().popBackStack() }
        binding.btnUsarFoto.setOnClickListener { confirmarFoto() }
        binding.btnRetomar.setOnClickListener { mostrarCamara() }
    }

    private fun configurarZoomPreview() {
        val detector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val camara = camera ?: return true
                    val estado = camara.cameraInfo.zoomState.value ?: return true
                    val nuevoRatio = (estado.zoomRatio * detector.scaleFactor)
                        .coerceIn(estado.minZoomRatio, estado.maxZoomRatio)
                    camara.cameraControl.setZoomRatio(nuevoRatio)
                    return true
                }
            })

        binding.previewView.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    private fun configurarZoomFoto() {
        var escalaActual = 1f
        val detector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    escalaActual = (escalaActual * detector.scaleFactor).coerceIn(1f, 5f)
                    binding.imgPreviewFoto.scaleX = escalaActual
                    binding.imgPreviewFoto.scaleY = escalaActual
                    return true
                }
            })

        binding.imgPreviewFoto.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    private fun iniciarCamara() {
        val futuro = ProcessCameraProvider.getInstance(requireContext())
        futuro.addListener({
            cameraProvider = futuro.get()
            bindUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

        imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(if (flashActivado) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun capturarFoto() {
        val capture = imageCapture ?: return
        binding.btnCapturar.isEnabled = false

        val archivo = File(requireContext().cacheDir, "product_images/foto_${System.currentTimeMillis()}.jpg")
        archivo.parentFile?.mkdirs()

        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(archivo).build(),
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    uriFotoCapturada = Uri.fromFile(archivo)
                    mostrarConfirmacion(uriFotoCapturada!!)
                }

                override fun onError(e: ImageCaptureException) {
                    binding.btnCapturar.isEnabled = true
                    Toast.makeText(requireContext(), "Error al capturar foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun mostrarConfirmacion(uri: Uri) {
        binding.overlayConfirmacion.isVisible = true
        binding.imgPreviewFoto.scaleX = 1f
        binding.imgPreviewFoto.scaleY = 1f
        Glide.with(this).load(uri).into(binding.imgPreviewFoto)
    }

    private fun mostrarCamara() {
        binding.overlayConfirmacion.isVisible = false
        binding.btnCapturar.isEnabled = true
        uriFotoCapturada = null
    }

    private fun confirmarFoto() {
        val uri = uriFotoCapturada ?: return
        setFragmentResult(RESULTADO_FOTO, bundleOf(CLAVE_URI to uri.toString()))
        findNavController().popBackStack()
    }

    private fun cambiarCamara() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA
        bindUseCases()
    }

    private fun toggleFlash() {
        flashActivado = !flashActivado
        imageCapture?.flashMode = if (flashActivado) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        binding.btnFlash.setImageResource(if (flashActivado) R.drawable.ic_flash else R.drawable.ic_flash_off)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        _binding = null
    }

    companion object {
        const val RESULTADO_FOTO = "resultado_foto"
        const val CLAVE_URI = "uri_foto"
    }
}
