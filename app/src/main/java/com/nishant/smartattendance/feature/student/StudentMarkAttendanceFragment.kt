package com.nishant.smartattendance.feature.student

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.nishant.smartattendance.data.repository.FaceRepository
import com.nishant.smartattendance.data.repository.SessionRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.databinding.FragmentMarkAttendanceBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt

class StudentMarkAttendanceFragment : Fragment() {

    private var _binding: FragmentMarkAttendanceBinding? = null
    private val binding get() = _binding!!

    private val sessionRepository = SessionRepository()
    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private val faceRepository = FaceRepository()

    private var cameraExecutor: ExecutorService? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // State
    private var pendingCode: String = ""
    private var isCapturing = false
    private var scanAnimator: ObjectAnimator? = null

    // ────────────────────────────────────────
    // Camera permission launcher
    // ────────────────────────────────────────
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
    // STEP 1 — Request camera, then scan
    // ════════════════════════════════════════

    private fun requestCameraAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startFaceScan()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ════════════════════════════════════════
    // STEP 2 — Show camera + start face detection
    // ════════════════════════════════════════

    private fun startFaceScan() {
        binding.cardStatus.visibility = View.GONE
        binding.cardFaceScan.visibility = View.VISIBLE
        binding.btnMarkAttendance.isEnabled = false
        binding.btnMarkAttendance.alpha = 0.6f
        isCapturing = false
        updateFaceStatus("🔍 Looking for your face...", "#5C6BC0")
        startScanAnimation()
        startCamera()
    }

    private fun startCamera() {
        val mainExecutor = ContextCompat.getMainExecutor(requireContext())
        ProcessCameraProvider.getInstance(requireContext()).also { future ->
            future.addListener(Runnable {
                try {
                    cameraProvider = future.get()
                    bindCamera()
                } catch (e: Exception) {
                    showStatusAndHideScan(false, "❌", "Camera Error", e.message ?: "Could not start camera")
                }
            }, mainExecutor)
        }
    }

    private fun bindCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.25f)
            .build()
        val detector = FaceDetection.getClient(options)

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also { analysis ->
                analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    if (!isCapturing) {
                        processFrame(imageProxy, detector)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            showStatusAndHideScan(false, "❌", "Camera Error", e.message ?: "Could not bind camera")
        }
    }

    // ════════════════════════════════════════
    // STEP 3 — Process each camera frame
    // ════════════════════════════════════════

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processFrame(imageProxy: ImageProxy, detector: com.google.mlkit.vision.face.FaceDetector) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    updateFaceStatus("🔍 No face detected — look at the camera", "#5C6BC0")
                } else if (faces.size > 1) {
                    updateFaceStatus("👥 Multiple faces detected — please be alone", "#E65100")
                } else {
                    // Exactly one face found — good
                    val face = faces[0]
                    if (!isCapturing && isFaceWellPositioned(face)) {
                        isCapturing = true
                        updateFaceStatus("✅ Face detected — verifying...", "#2E7D32")
                        val embedding = extractEmbedding(face)
                        lifecycleScope.launch {
                            handleFaceCapture(embedding)
                        }
                    } else if (!isCapturing) {
                        updateFaceStatus("📐 Center your face in the circle", "#5C6BC0")
                    }
                }
            }
            .addOnFailureListener {
                updateFaceStatus("⚠️ Detection error — try again", "#E65100")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    // ════════════════════════════════════════
    // STEP 4 — Handle captured face embedding
    // ════════════════════════════════════════

    private suspend fun handleFaceCapture(embedding: List<Float>) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        try {
            val student = studentRepository.getStudentByEmail(email)
            if (student == null) {
                showStatusAndHideScan(false, "❌", "Profile Not Found",
                    "Your student profile is not set up. Contact admin.")
                resetButton()
                return
            }

            stopCamera()

            if (!student.faceRegistered) {
                // ── First time: register face ──
                updateFaceStatus("📸 Registering your face...", "#1565C0")
                delay(500)
                val saved = faceRepository.saveFaceEmbedding(student.srn, embedding)
                if (!saved) {
                    showStatusAndHideScan(false, "❌", "Registration Failed",
                        "Could not save face data. Please try again.")
                    resetButton()
                    return
                }
                updateFaceStatus("✅ Face registered!", "#2E7D32")
                delay(800)
                // Now proceed to mark attendance
                proceedToMarkAttendance(student, email)

            } else {
                // ── Returning student: verify face ──
                updateFaceStatus("🔐 Verifying identity...", "#1565C0")
                delay(500)
                val stored = faceRepository.getFaceEmbedding(student.srn)
                if (stored == null) {
                    // Stored embedding missing — re-register
                    faceRepository.saveFaceEmbedding(student.srn, embedding)
                    proceedToMarkAttendance(student, email)
                    return
                }

                val similarity = faceRepository.cosineSimilarity(embedding, stored)
                if (similarity >= FaceRepository.MATCH_THRESHOLD) {
                    updateFaceStatus("✅ Face matched!", "#2E7D32")
                    delay(600)
                    proceedToMarkAttendance(student, email)
                } else {
                    showStatusAndHideScan(false, "🚫", "Face Not Recognised",
                        "Your face could not be verified. Make sure you're in good lighting and try again.")
                    resetButton()
                }
            }

        } catch (e: Exception) {
            showStatusAndHideScan(false, "❌", "Error", e.message ?: "Something went wrong.")
            resetButton()
        }
    }

    // ════════════════════════════════════════
    // STEP 5 — Mark attendance after face pass
    // ════════════════════════════════════════

    private suspend fun proceedToMarkAttendance(
        student: com.nishant.smartattendance.domain.model.Student,
        email: String
    ) {
        hideFaceScan()

        val courses = courseRepository.getAllCourses()
        val course = courses.find { it.name == student.courseId }
        val currentSemester = if (course != null) {
            studentRepository.calculateCurrentSemester(student.joinedAt, course.totalSemesters)
        } else student.currentSemester

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
                    "No active session found for this code. Check the code and try again.")
            is SessionRepository.MarkResult.Expired ->
                showStatus(false, "⏰", "Code Expired",
                    "This session code has expired. Ask your teacher for a new one.")
            is SessionRepository.MarkResult.AlreadyMarked ->
                showStatus(false, "ℹ️", "Already Marked",
                    "Your attendance is already recorded for this class today.")
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
    // FACE HELPERS
    // ════════════════════════════════════════

    // Extract a pseudo-embedding from face landmarks
    // Uses relative positions of facial landmarks as a feature vector
    private fun extractEmbedding(face: Face): List<Float> {
        val features = mutableListOf<Float>()
        val box = face.boundingBox
        val w = box.width().toFloat()
        val h = box.height().toFloat()

        // Normalize bounding box aspect ratio
        features.add(w / (h + 1f))

        // Head rotation angles (reliable biometric features)
        features.add(face.headEulerAngleX / 90f)  // pitch
        features.add(face.headEulerAngleY / 90f)  // yaw
        features.add(face.headEulerAngleZ / 90f)  // roll

        // Eye open probabilities
        features.add(face.leftEyeOpenProbability ?: 0.5f)
        features.add(face.rightEyeOpenProbability ?: 0.5f)

        // Smiling probability
        features.add(face.smilingProbability ?: 0.0f)

        // Landmark positions normalized to face bounding box
        val landmarks = listOf(
            com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE,
            com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE,
            com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE,
            com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT,
            com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT,
            com.google.mlkit.vision.face.FaceLandmark.LEFT_EAR,
            com.google.mlkit.vision.face.FaceLandmark.RIGHT_EAR,
            com.google.mlkit.vision.face.FaceLandmark.LEFT_CHEEK,
            com.google.mlkit.vision.face.FaceLandmark.RIGHT_CHEEK
        )

        for (landmarkType in landmarks) {
            val lm = face.getLandmark(landmarkType)
            if (lm != null) {
                features.add((lm.position.x - box.left) / (w + 1f))
                features.add((lm.position.y - box.top) / (h + 1f))
            } else {
                features.add(0f)
                features.add(0f)
            }
        }

        // Inter-landmark distances (eye distance, eye-nose distance etc.)
        val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
        val nose = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
        val mouthL = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT)
        val mouthR = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT)

        fun dist(
            a: com.google.mlkit.vision.face.FaceLandmark?,
            b: com.google.mlkit.vision.face.FaceLandmark?
        ): Float {
            if (a == null || b == null) return 0f
            val dx = a.position.x - b.position.x
            val dy = a.position.y - b.position.y
            return sqrt(dx * dx + dy * dy) / (w + 1f)
        }

        features.add(dist(leftEye, rightEye))   // eye distance
        features.add(dist(leftEye, nose))        // left eye to nose
        features.add(dist(rightEye, nose))       // right eye to nose
        features.add(dist(nose, mouthL))         // nose to mouth left
        features.add(dist(nose, mouthR))         // nose to mouth right
        features.add(dist(mouthL, mouthR))       // mouth width

        return features
    }

    // Face must be reasonably centered and not too tilted
    private fun isFaceWellPositioned(face: Face): Boolean {
        val yaw = Math.abs(face.headEulerAngleY)
        val pitch = Math.abs(face.headEulerAngleX)
        val roll = Math.abs(face.headEulerAngleZ)
        return yaw < 20f && pitch < 20f && roll < 20f
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
        scanAnimator = ObjectAnimator.ofFloat(
            binding.scanLine, "translationY", -100f, 100f
        ).apply {
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
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etClassCode.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
    }
}