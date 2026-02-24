package com.nishant.smartattendance.feature.student

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.nishant.smartattendance.data.repository.CourseRepository
import com.nishant.smartattendance.data.repository.FaceRepository
import com.nishant.smartattendance.data.repository.StudentRepository
import com.nishant.smartattendance.databinding.FragmentStudentProfileBinding
import com.nishant.smartattendance.domain.model.Student
import com.nishant.smartattendance.feature.auth.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudentProfileFragment : Fragment() {

    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!

    private val studentRepository = StudentRepository()
    private val courseRepository = CourseRepository()
    private val faceRepository = FaceRepository()

    private var currentStudent: Student? = null
    private var isEditMode = false

    // Camera & FaceNet
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var faceNetHelper: FaceNetHelper? = null
    private var captureMode = false

    // Registration state — collect 5 good frames
    private val capturedEmbeddings = mutableListOf<FloatArray>()
    private var isProcessingFrame = false
    private lateinit var faceDetector: FaceDetector

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openFaceCamera()
        else Toast.makeText(requireContext(),
            "Camera permission needed to register face", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize ML Kit face detector — strict settings
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.35f)  // Face must occupy at least 35% of frame width
            .build()
        faceDetector = FaceDetection.getClient(options)

        // Initialize TFLite model
        faceNetHelper = FaceNetHelper(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            val ready = faceNetHelper?.initialize() ?: false
            if (!ready) {
                withContext(Dispatchers.Main) {
                    binding.tvFaceStatusDesc.text =
                        "⚠️ Model file missing — see setup instructions"
                }
            }
        }

        loadProfile()

        binding.btnEditToggle.setOnClickListener { enterEditMode() }
        binding.btnCancelEdit.setOnClickListener { exitEditMode() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.etDob.setOnClickListener { showDatePicker() }

        binding.btnRegisterFace.setOnClickListener {
            if (captureMode) stopFaceCamera()
            else requestCameraAndRegister()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    // ════════════════════════════════════════
    // PROFILE LOAD
    // ════════════════════════════════════════

    private fun loadProfile() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        lifecycleScope.launch {
            try {
                val student = studentRepository.getStudentByEmail(email)
                if (student == null) {
                    binding.tvProfileName.text = "Not registered"
                    binding.tvProfileEmail.text = email
                    binding.tvProfileSrn.text = "Contact admin to get registered"
                    return@launch
                }
                currentStudent = student

                val courses = courseRepository.getAllCourses()
                val course = courses.find { it.name == student.courseId }
                val currentSemester = if (course != null) {
                    studentRepository.calculateCurrentSemester(student.joinedAt, course.totalSemesters)
                } else student.currentSemester

                binding.tvProfileName.text = student.name
                binding.tvProfileSrn.text = student.srn
                binding.tvProfileEmail.text = student.email
                binding.tvProfileCourse.text = student.courseId
                binding.tvProfileSection.text = "${student.section} | Roll No: ${student.rollNo}"
                binding.tvProfileSemester.text = "Semester $currentSemester"
                binding.tvViewPhone.text = student.phone.ifEmpty { "Not provided" }
                binding.tvViewAddress.text = student.address.ifEmpty { "Not provided" }
                binding.tvViewDob.text = student.dateOfBirth.ifEmpty { "Not provided" }
                binding.etPhone.setText(student.phone)
                binding.etAddress.setText(student.address)
                binding.etDob.setText(student.dateOfBirth)

                updateFaceStatusUI(student.faceRegistered)

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateFaceStatusUI(registered: Boolean) {
        if (registered) {
            binding.tvFaceStatusIcon.text = "✅"
            binding.tvFaceStatusTitle.text = "Face Registered"
            binding.tvFaceStatusTitle.setTextColor(0xFF2E7D32.toInt())
            binding.tvFaceStatusDesc.text = "Your face is set up for attendance verification"
            binding.btnRegisterFace.text = "🔄  Re-register My Face"
        } else {
            binding.tvFaceStatusIcon.text = "❌"
            binding.tvFaceStatusTitle.text = "Face Not Registered"
            binding.tvFaceStatusTitle.setTextColor(0xFFB71C1C.toInt())
            binding.tvFaceStatusDesc.text = "Register your face to mark attendance"
            binding.btnRegisterFace.text = "📷  Register My Face"
        }
    }

    // ════════════════════════════════════════
    // FACE REGISTRATION
    // ════════════════════════════════════════

    private fun requestCameraAndRegister() {
        if (faceNetHelper?.isReady() == false) {
            Toast.makeText(requireContext(),
                "Face recognition model not loaded. Check assets folder.",
                Toast.LENGTH_LONG).show()
            return
        }
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> openFaceCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openFaceCamera() {
        captureMode = true
        capturedEmbeddings.clear()
        isProcessingFrame = false
        binding.layoutFaceCapture.visibility = View.VISIBLE
        binding.btnRegisterFace.text = "❌  Cancel"
        updateCaptureHint("📷 Position your full face in the circle")

        val mainExecutor = ContextCompat.getMainExecutor(requireContext())
        ProcessCameraProvider.getInstance(requireContext()).also { future ->
            future.addListener(Runnable {
                try {
                    cameraProvider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.profileCameraPreview.surfaceProvider)
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { analysis ->
                            analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                                processRegistrationFrame(imageProxy)
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
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                        stopFaceCamera()
                    }
                }
            }, mainExecutor)
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processRegistrationFrame(imageProxy: ImageProxy) {
        if (isProcessingFrame || !captureMode) {
            imageProxy.close()
            return
        }
        isProcessingFrame = true

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); isProcessingFrame = false; return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Convert frame to Bitmap for FaceNet cropping
        val bitmap = imageProxyToBitmap(imageProxy)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                when {
                    faces.isEmpty() ->
                        updateCaptureHint("🔍 No face detected — look at the camera")

                    faces.size > 1 ->
                        updateCaptureHint("👥 Only one person allowed — step back")

                    else -> {
                        val face = faces[0]
                        val yaw   = Math.abs(face.headEulerAngleY)
                        val pitch = Math.abs(face.headEulerAngleX)
                        val roll  = Math.abs(face.headEulerAngleZ)
                        val leftEyeOpen  = face.leftEyeOpenProbability ?: 0f
                        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
                        val box = face.boundingBox

                        // Require actual landmark detection — unreliable in dark/partial face
                        val leftEyeLm  = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                        val rightEyeLm = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                        val noseLm     = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
                        val mouthLLm   = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT)
                        val mouthRLm   = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT)

                        when {
                            // All 5 key landmarks must be visible
                            leftEyeLm == null || rightEyeLm == null || noseLm == null
                                    || mouthLLm == null || mouthRLm == null ->
                                updateCaptureHint("👁 Full face not visible — adjust position & lighting")

                            // Strict rotation — no tilted or angled faces
                            yaw > 10f || pitch > 10f || roll > 10f ->
                                updateCaptureHint("📐 Look straight at the camera — don't tilt")

                            // Both eyes must be clearly open
                            leftEyeOpen < 0.75f || rightEyeOpen < 0.75f ->
                                updateCaptureHint("👁 Keep both eyes fully open")

                            // Face must be large enough in frame
                            box.width() < 130 ->
                                updateCaptureHint("🔎 Move closer to the camera")

                            bitmap != null -> {
                                val faceCrop = faceNetHelper?.cropFace(
                                    bitmap, box.left, box.top, box.width(), box.height()
                                )
                                if (faceCrop != null) {
                                    // Brightness check — reject dark or overexposed frames
                                    val brightness = getAverageBrightness(faceCrop)
                                    when {
                                        brightness < 70f ->
                                            updateCaptureHint("💡 Too dark — move to better lighting")
                                        brightness > 235f ->
                                            updateCaptureHint("☀️ Too bright — reduce light behind you")
                                        else -> {
                                            val embedding = faceNetHelper?.getEmbedding(faceCrop)
                                            if (embedding != null) {
                                                capturedEmbeddings.add(embedding)
                                                val count = capturedEmbeddings.size
                                                val required = FaceRepository.FRAMES_REQUIRED
                                                updateCaptureHint("✅ Capturing... $count/$required (hold still)")
                                                if (count >= required) {
                                                    val avgEmbedding = faceRepository.averageEmbeddings(capturedEmbeddings)
                                                    saveRegistration(avgEmbedding)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> updateCaptureHint("📷 Position your full face in the circle")
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessingFrame = false
            }
    }

    private fun saveRegistration(embedding: FloatArray) {
        captureMode = false  // Stop processing more frames
        val student = currentStudent ?: run { stopFaceCamera(); return }

        lifecycleScope.launch {
            updateCaptureHint("💾 Saving face data...")
            val success = faceRepository.saveFaceEmbedding(student.srn, embedding)
            stopFaceCamera()
            if (success) {
                currentStudent = currentStudent?.copy(faceRegistered = true)
                updateFaceStatusUI(true)
                Toast.makeText(requireContext(),
                    "✅ Face registered successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(),
                    "Failed to save. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopFaceCamera() {
        captureMode = false
        isProcessingFrame = false
        capturedEmbeddings.clear()
        cameraProvider?.unbindAll()
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.layoutFaceCapture.visibility = View.GONE
            val registered = currentStudent?.faceRegistered ?: false
            binding.btnRegisterFace.text =
                if (registered) "🔄  Re-register My Face" else "📷  Register My Face"
        }
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
            // Convert YUV_420_888 to Bitmap via NV21 intermediate
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
            // Front camera — flip horizontally
            matrix.postScale(-1f, 1f, bmp.width / 2f, bmp.height / 2f)
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateCaptureHint(text: String) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            binding.tvCaptureFaceHint.text = text
        }
    }

    // ════════════════════════════════════════
    // PROFILE EDIT
    // ════════════════════════════════════════

    private fun enterEditMode() {
        isEditMode = true
        binding.cardViewMode.visibility = View.GONE
        binding.cardEditMode.visibility = View.VISIBLE
        binding.btnEditToggle.visibility = View.GONE
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.cardViewMode.visibility = View.VISIBLE
        binding.cardEditMode.visibility = View.GONE
        binding.btnEditToggle.visibility = View.VISIBLE
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val existingDob = binding.etDob.text.toString()
        if (existingDob.isNotEmpty()) {
            try {
                val parts = existingDob.split("/")
                if (parts.size == 3)
                    calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            } catch (e: Exception) { }
        }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            binding.etDob.setText("${day.toString().padStart(2,'0')}/${(month+1).toString().padStart(2,'0')}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            .also { it.datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun saveProfile() {
        val student = currentStudent ?: return
        lifecycleScope.launch {
            try {
                val success = studentRepository.updateStudentProfile(
                    srn = student.srn,
                    phone = binding.etPhone.text.toString().trim(),
                    address = binding.etAddress.text.toString().trim(),
                    dateOfBirth = binding.etDob.text.toString().trim()
                )
                if (success) {
                    Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                    exitEditMode()
                    loadProfile()
                } else {
                    Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        faceNetHelper?.close()
        _binding = null
    }
}