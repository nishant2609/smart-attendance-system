package com.nishant.smartattendance.feature.student

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.nishant.smartattendance.data.repository.FaceRepository
import com.nishant.smartattendance.data.repository.SessionRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.databinding.FragmentMarkAttendanceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudentMarkAttendanceFragment : Fragment() {

    private var _binding: FragmentMarkAttendanceBinding? = null
    private val binding get() = _binding!!

    private val sessionRepository = SessionRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private val faceRepository = FaceRepository()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var faceNetHelper: FaceNetHelper? = null
    private lateinit var faceDetector: FaceDetector

    private var pendingCode: String = ""
    private var isCapturing = false
    private var scanAnimator: ObjectAnimator? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startFaceScan()
        else {
            showStatus(false, "📷", "Camera Permission Needed",
                "Please allow camera access to verify your face")
            hideFaceScan()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.3f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        faceNetHelper = FaceNetHelper(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            faceNetHelper?.initialize()
        }

        binding.cardStatus.visibility = View.GONE
        binding.cardFaceScan.visibility = View.GONE

        binding.btnMarkAttendance.setOnClickListener {
            val code = binding.etClassCode.text?.toString()?.trim()?.uppercase() ?: ""
            if (code.length < 6) {
                showStatus(false, "⚠️", "Invalid Code",
                    "Please enter the full 6-character class code")
                return@setOnClickListener
            }
            pendingCode = code
            hideKeyboard()
            requestCameraAndScan()
        }

        binding.btnCancelFace.setOnClickListener {
            stopCamera()
            hideFaceScan()
            resetButton()
        }
    }

    // ════════════════════════════════════════
    // STEP 1 — Request camera
    // ════════════════════════════════════════

    private fun requestCameraAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startFaceScan()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ════════════════════════════════════════
    // STEP 2 — Open camera for verification
    // ════════════════════════════════════════

    private fun startFaceScan() {
        binding.cardStatus.visibility = View.GONE
        binding.cardFaceScan.visibility = View.VISIBLE
        binding.btnMarkAttendance.isEnabled = false
        binding.btnMarkAttendance.alpha = 0.6f
        isCapturing = false
        updateFaceStatus("🔍 Position your face in the circle", "#5C6BC0")
        startScanAnimation()

        val mainExecutor = ContextCompat.getMainExecutor(requireContext())
        ProcessCameraProvider.getInstance(requireContext()).also { future ->
            future.addListener(Runnable {
                try {
                    cameraProvider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { analysis ->
                            analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                                if (!isCapturing) processVerificationFrame(imageProxy)
                                else imageProxy.close()
                            }
                        }
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (e: Exception) {
                    showStatusAndHideScan(false, "❌", "Camera Error",
                        e.message ?: "Could not start camera")
                }
            }, mainExecutor)
        }
    }

    // ════════════════════════════════════════
    // STEP 3 — Process frame for verification
    // ════════════════════════════════════════

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processVerificationFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val bitmap = imageProxyToBitmap(imageProxy)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                when {
                    faces.isEmpty() ->
                        updateFaceStatus("🔍 No face detected — look at the camera", "#5C6BC0")
                    faces.size > 1 ->
                        updateFaceStatus("👥 Multiple faces — please be alone", "#E65100")
                    else -> {
                        val face = faces[0]
                        val yaw   = Math.abs(face.headEulerAngleY)
                        val pitch = Math.abs(face.headEulerAngleX)
                        val roll  = Math.abs(face.headEulerAngleZ)
                        val leftEyeOpen  = face.leftEyeOpenProbability ?: 0f
                        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
                        val box = face.boundingBox

                        val leftEyeLm  = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                        val rightEyeLm = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                        val noseLm     = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
                        val mouthLLm   = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT)
                        val mouthRLm   = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT)

                        when {
                            leftEyeLm == null || rightEyeLm == null || noseLm == null
                                    || mouthLLm == null || mouthRLm == null ->
                                updateFaceStatus("👁 Full face not visible — better lighting needed", "#E65100")
                            yaw > 10f || pitch > 10f || roll > 10f ->
                                updateFaceStatus("📐 Look straight at the camera", "#5C6BC0")
                            leftEyeOpen < 0.75f || rightEyeOpen < 0.75f ->
                                updateFaceStatus("👁 Keep both eyes fully open", "#5C6BC0")
                            box.width() < 130 ->
                                updateFaceStatus("🔎 Move closer to the camera", "#5C6BC0")
                            bitmap != null && !isCapturing -> {
                                val faceCrop = faceNetHelper?.cropFace(
                                    bitmap, box.left, box.top, box.width(), box.height()
                                )
                                if (faceCrop != null) {
                                    val brightness = getAverageBrightness(faceCrop)
                                    when {
                                        brightness < 70f ->
                                            updateFaceStatus("💡 Too dark — move to better lighting", "#E65100")
                                        brightness > 235f ->
                                            updateFaceStatus("☀️ Too bright — reduce light behind you", "#E65100")
                                        else -> {
                                            isCapturing = true
                                            updateFaceStatus("✅ Face detected — verifying...", "#2E7D32")
                                            val embedding = faceNetHelper?.getEmbedding(faceCrop)
                                            if (embedding != null) {
                                                lifecycleScope.launch { handleVerification(embedding) }
                                            } else {
                                                isCapturing = false
                                                updateFaceStatus("⚠️ Could not read face — try again", "#E65100")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    // ════════════════════════════════════════
    // STEP 4 — Verify face against stored
    // ════════════════════════════════════════

    private suspend fun handleVerification(embedding: FloatArray) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        try {
            val student = studentRepository.getStudentByEmail(email)
            if (student == null) {
                showStatusAndHideScan(false, "❌", "Profile Not Found",
                    "Your student profile is not set up. Contact admin.")
                resetButton(); return
            }

            stopCamera()

            // Block if face not registered
            if (!student.faceRegistered) {
                showStatusAndHideScan(false, "📋", "Face Not Registered",
                    "Go to your Profile tab and register your face first.")
                resetButton(); return
            }

            updateFaceStatus("🔐 Verifying your identity...", "#1565C0")
            delay(400)

            val stored = faceRepository.getFaceEmbedding(student.srn)
            if (stored == null) {
                showStatusAndHideScan(false, "❌", "Face Data Missing",
                    "Please re-register your face in the Profile tab.")
                resetButton(); return
            }

            val similarity = faceRepository.cosineSimilarity(embedding, stored)
            android.util.Log.d("FaceVerify", "Similarity: $similarity (threshold: ${FaceRepository.MATCH_THRESHOLD})")

            if (similarity >= FaceRepository.MATCH_THRESHOLD) {
                updateFaceStatus("✅ Identity verified!", "#2E7D32")
                delay(600)
                proceedToMarkAttendance(student)
            } else {
                showStatusAndHideScan(false, "🚫", "Face Not Recognised",
                    "Your face did not match. Try in better lighting or re-register in Profile.")
                resetButton()
            }

        } catch (e: Exception) {
            showStatusAndHideScan(false, "❌", "Error", e.message ?: "Something went wrong.")
            resetButton()
        }
    }

    // ════════════════════════════════════════
    // STEP 5 — Mark attendance
    // ════════════════════════════════════════

    private suspend fun proceedToMarkAttendance(
        student: com.nishant.smartattendance.domain.model.Student
    ) {
        hideFaceScan()

        val courses = courseRepository.getAllCourses()
        val course = courses.find { it.name == student.courseId }
        val currentSemester = if (course != null)
            studentRepository.calculateCurrentSemester(student.joinedAt, course.totalSemesters)
        else student.currentSemester

        val result = sessionRepository.markAttendanceWithCode(
            enteredCode = pendingCode,
            studentSrn = student.srn,
            studentCourseId = student.courseId,
            studentSection = student.section,
            studentSemester = currentSemester,
            studentName = student.name
        )

        when (result) {
            is SessionRepository.MarkResult.Success ->
                showStatus(true, "✅", "Attendance Marked!",
                    "Your attendance has been recorded successfully")
            is SessionRepository.MarkResult.InvalidCode ->
                showStatus(false, "❌", "Invalid Code",
                    "No active session found. Check the code and try again.")
            is SessionRepository.MarkResult.Expired ->
                showStatus(false, "⏰", "Code Expired",
                    "This session has expired. Ask your teacher for a new code.")
            is SessionRepository.MarkResult.AlreadyMarked ->
                showStatus(false, "ℹ️", "Already Marked",
                    "Your attendance is already recorded for this class.")
            is SessionRepository.MarkResult.WrongCourse ->
                showStatus(false, "🚫", "Wrong Class",
                    "This session is for a different course, section, or semester.")
            is SessionRepository.MarkResult.SessionInactive ->
                showStatus(false, "🔒", "Session Closed",
                    "This session has been closed by the teacher.")
        }

        if (result is SessionRepository.MarkResult.Success) {
            binding.etClassCode.text?.clear()
        }
        resetButton()
    }

    // ════════════════════════════════════════
    // BITMAP HELPERS
    // ════════════════════════════════════════

    // Returns average luminance 0–255. Below 70 = too dark, above 235 = too bright.
    private fun getAverageBrightness(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        var total = 0L
        var count = 0
        val step = 4
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8)  and 0xFF
                val b =  pixel         and 0xFF
                total = total + ((0.299f * r + 0.587f * g + 0.114f * b).toLong())
                count++
                x += step
            }
            y += step
        }
        return if (count == 0) 128f else total.toFloat() / count
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                90,
                out
            )
            val bytes = out.toByteArray()
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null
            val matrix = android.graphics.Matrix()
            matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            matrix.postScale(-1f, 1f, bmp.width / 2f, bmp.height / 2f)
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } catch (e: Exception) { null }
    }

    // ════════════════════════════════════════
    // UI HELPERS
    // ════════════════════════════════════════

    private fun updateFaceStatus(text: String, colorHex: String) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.tvFaceStatus.text = text
            binding.tvFaceStatus.setTextColor(android.graphics.Color.parseColor(colorHex))
        }
    }

    private fun showStatus(isSuccess: Boolean, icon: String, title: String, message: String) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvStatusIcon.text = icon
            binding.tvStatusTitle.text = title
            binding.tvStatusTitle.setTextColor(
                if (isSuccess) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt()
            )
            binding.tvStatusMessage.text = message
            binding.cardStatus.setCardBackgroundColor(
                if (isSuccess) 0xFFF1F8E9.toInt() else 0xFFFFF3F3.toInt()
            )
        }
    }

    private fun showStatusAndHideScan(isSuccess: Boolean, icon: String, title: String, message: String) {
        hideFaceScan()
        showStatus(isSuccess, icon, title, message)
    }

    private fun hideFaceScan() {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.cardFaceScan.visibility = View.GONE
            stopScanAnimation()
        }
    }

    private fun startScanAnimation() {
        binding.scanLine.visibility = View.VISIBLE
        scanAnimator = ObjectAnimator.ofFloat(binding.scanLine, "translationY", -100f, 100f).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopScanAnimation() {
        scanAnimator?.cancel()
        scanAnimator = null
        if (_binding != null) binding.scanLine.visibility = View.GONE
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun resetButton() {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.btnMarkAttendance.isEnabled = true
            binding.btnMarkAttendance.alpha = 1f
            isCapturing = false
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etClassCode.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        faceNetHelper?.close()
        _binding = null
    }
}