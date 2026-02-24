<div align="center">

<img src="docs/screenshots/01_login.png" width="220" alt="Smart Attendance Login"/>

# Smart Attendance

### AI-Powered Attendance Management for Android

[![Android](https://img.shields.io/badge/Platform-Android%20API%2026+-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![TFLite](https://img.shields.io/badge/ML-TensorFlow%20Lite-FF6F00?logo=tensorflow&logoColor=white)](https://tensorflow.org/lite)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A full-stack Android attendance system combining **session-code attendance** with **on-device face recognition** to eliminate proxy attendance — built for colleges and educational institutions.

</div>

---

## 📸 Screenshots

### Student App

<div align="center">

| Home | Mark Attendance | My Attendance | Profile |
|------|----------------|---------------|---------|
| <img src="docs/screenshots/02_student_home.png" width="180"/> | <img src="docs/screenshots/03_mark_attendance.png" width="180"/> | <img src="docs/screenshots/04_student_attendance.png" width="180"/> | <img src="docs/screenshots/05_student_profile.png" width="180"/> |
| Overall % + low-attendance warning + recent records | Enter 6-char code → face verification | Subject-wise % with progress bars | Face registration status + personal info |

</div>

### Admin App

<div align="center">

| Dashboard | Students | Attendance |
|-----------|----------|------------|
| <img src="docs/screenshots/06_admin_dashboard.png" width="180"/> | <img src="docs/screenshots/07_admin_students.png" width="180"/> | <img src="docs/screenshots/08_admin_attendance.png" width="180"/> |
| Stats + courses list | Student roster with face status badges | Filter by date/course/section/subject |

</div>

---

## ✨ Features

### 🎓 Student
- **Home dashboard** — overall attendance %, classes attended, low-attendance warning (lists subjects below 75%)
- **Recent attendance** feed — latest records with subject, date, and Present/Absent status
- **Mark attendance** — enter teacher's session code → camera opens → face verified → attendance recorded
- **Subject-wise breakdown** — color-coded progress bars per subject (🟢 ≥75%, 🟡 60–74%, 🔴 <60%), filterable by date
- **Profile** — academic info, face registration status, personal details editor
- **Face re-registration** — update face embedding at any time; old data auto-deleted

### 👨‍🏫 Admin (Teacher)
- **Dashboard** — total students, today's present count, course cards, low-attendance student alerts
- **Session management** — generate a 6-character code with 15-minute live countdown timer; auto-expires
- **Manual attendance** — mark P/A per student for any date; submit to Firestore
- **Attendance history** — filter by date, course, section, semester, subject
- **Student management** — add students, view face registration status (Face ✓ / No Face badges)
- **Export** — download attendance as PDF or Excel (`.xlsx`) for any date range

### 🔐 Security
- **On-device FaceNet** (TFLite, 128-dim embeddings) — no face images leave the device
- **5-frame averaging** — registration captures 5 frames and averages embeddings for robustness
- **Quality gates** — rejects frames that are too dark, overexposed, angled, eyes closed, or missing landmarks
- **Cosine similarity matching** — configurable threshold (default 0.60)
- **Session expiry** — codes valid 15 minutes, auto-deactivated on expiry
- **Course/section lock** — attendance only accepted if student is in the exact class

---

## 🏗 Architecture

```
SmartAttendance/
├── app/src/main/java/com/nishant/smartattendance/
│   ├── data/repository/
│   │   ├── AttendanceRepository.kt   # Attendance records CRUD
│   │   ├── CourseRepository.kt       # Courses and subjects
│   │   ├── ExportRepository.kt       # PDF / Excel generation
│   │   ├── FaceRepository.kt         # Face embeddings (Firestore)
│   │   ├── SessionRepository.kt      # Session create / validate / expire
│   │   └── StudentRepository.kt      # Student profiles
│   ├── domain/model/
│   │   ├── Student.kt
│   │   ├── Course.kt
│   │   ├── AttendanceRecord.kt
│   │   └── AttendanceSession.kt
│   └── feature/
│       ├── admin/
│       │   ├── HomeFragment.kt                # Dashboard + low-attendance alerts
│       │   ├── AttendanceFragment.kt          # Session timer + manual marking
│       │   ├── StudentsFragment.kt            # Student roster
│       │   └── ExportAttendanceFragment.kt
│       ├── auth/                              # Login / register
│       └── student/
│           ├── FaceNetHelper.kt               # TFLite inference wrapper
│           ├── StudentHomeFragment.kt         # Dashboard + recent records
│           ├── StudentProfileFragment.kt      # Face registration
│           ├── StudentMarkAttendanceFragment.kt
│           └── StudentAttendanceFragment.kt   # Subject-wise breakdown
└── app/src/main/assets/
    └── facenet.tflite                         # FaceNet model (128-dim)
```

**Tech Stack:**

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Material Design 3, ViewBinding |
| Navigation | Jetpack Navigation Component |
| Camera | CameraX |
| Face Detection | ML Kit Face Detection |
| Face Recognition | TensorFlow Lite (FaceNet) |
| Database | Firebase Firestore |
| Auth | Firebase Authentication |
| Async | Kotlin Coroutines + Lifecyclescope |
| Export | Apache POI (Excel), iText (PDF) |
| Loading states | Facebook Shimmer |

---

## 🚀 Setup

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Android Studio | Giraffe or later |
| JDK | 17 |
| Android device / emulator | API 26+ |
| Firebase project | Free Spark plan |

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/SmartAttendance.git
cd SmartAttendance
```

### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com) → **Add project**
2. Click **Android** to add an app:
   - Package name: `com.nishant.smartattendance`
   - Download `google-services.json`
   - Place it at `app/google-services.json`

3. Enable **Authentication**:
   - Firebase Console → **Authentication** → **Sign-in method** → Enable **Email/Password**

4. Create **Firestore Database**:
   - Firebase Console → **Firestore** → **Create database** → Start in **test mode**

5. Set Firestore Security Rules (for production):

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 3. Add the FaceNet Model

The TFLite model is not included in the repo due to file size. Download it manually:

```bash
# Download from GitHub:
# https://github.com/shubham0204/FaceRecognition_With_FaceNet_Android/raw/master/app/src/main/assets/facenet.tflite

# Place the file here:
app/src/main/assets/facenet.tflite
```

> ⚠️ The filename must be exactly `facenet.tflite` — this is what `FaceNetHelper.kt` expects.

### 4. Build & Run

```bash
# Sync and build from Android Studio, or via terminal:
./gradlew assembleDebug

# Install on connected device:
./gradlew installDebug
```

### 5. First Login

The app routes users based on their Firebase Auth account:

- **Admin accounts** — create manually via Firebase Console → Authentication → Add user. Then add their UID to a `admins` collection in Firestore:
  ```
  Collection: admins
  Document ID: <uid from Firebase Auth>
  Fields: { "email": "admin@example.com", "role": "admin" }
  ```
- **Student accounts** — admin adds students from the Students tab in the app. Students log in with the email the admin registered them with.

---

## 📱 Usage Guide

### Admin: Taking Attendance

#### Session Mode (Recommended)
1. Open **Attendance** tab
2. Select: **Course → Section → Semester → Subject → Date**
3. Tap **Load Students**, then tap **Start Session**
4. A 6-character code appears with a live countdown timer
5. Share the code with students (verbally or on a board)
6. Session auto-expires after 15 minutes, or tap **Stop Session** manually

#### Manual Mode
1. After loading students, tap **P** or **A** next to each student
2. Tap **Submit Attendance**

#### Exporting Reports
1. Go to **Attendance** tab → tap the **Export** button (top right)
2. Select date range, course, section
3. Choose **Export PDF** or **Export Excel**
4. File saves to the device Downloads folder

---

### Student: First-Time Face Registration

1. Log in → go to **Profile** tab
2. Scroll to **Face Recognition** → tap **"Register My Face"**
3. Position your face in good lighting and look straight at the camera
4. The app captures 5 quality frames automatically (you'll see `✅ Capturing... 3/5`)
5. Status changes to ✅ **"Face Registered"** when done

**Requirements for a successful registration:**
- All 5 face landmarks detected (both eyes, nose, both mouth corners)
- Head rotation within 10° (yaw, pitch, roll)
- Both eyes clearly open (probability ≥ 0.75)
- Face width ≥ 130px in frame (move closer if needed)
- Average face brightness between 70–235 (well-lit, not overexposed)

> 💡 **Best conditions:** Stand facing a window or lamp. Avoid backlighting. Remove glasses if you plan to wear them inconsistently.

---

### Student: Marking Attendance

1. Go to **Mark** tab
2. Enter the 6-character code your teacher wrote on the board
3. Tap **"Mark My Attendance"** — the camera opens
4. Look at the camera the same way you did during registration
5. If face similarity ≥ 0.60 → ✅ Attendance marked
6. If below threshold → 🚫 Face Not Recognised (try better lighting or re-register)

---

## 🔧 Configuration

### Face Matching Threshold

Edit `FaceRepository.kt`:

```kotlin
companion object {
    const val EMBEDDING_SIZE  = 128
    const val MATCH_THRESHOLD = 0.60f  // ← adjust here
    const val FRAMES_REQUIRED = 5
}
```

| Threshold | Effect |
|-----------|--------|
| `0.55` | Lenient — good for inconsistent lighting |
| `0.60` | **Default** — balanced |
| `0.65` | Stricter — good for controlled environments |
| `0.70` | Very strict — may occasionally reject the right person |

> **Tip:** During testing, a debug toast shows the exact similarity score (e.g., `Face score: 0.724 (need ≥0.60)`). Use this to find the right threshold for your environment before going to production.

### Session Duration

Edit `SessionRepository.kt`:

```kotlin
val expiry = now + (15 * 60 * 1000)  // ← change 15 to desired minutes
```

---

## 🗄 Firestore Collections

| Collection | Document ID | Key Fields |
|-----------|-------------|------------|
| `students` | `{srn}` | name, email, courseId, section, semester, faceRegistered |
| `face_embeddings` | `{srn}` | embedding (128 floats), registeredAt |
| `attendance_sessions` | `{sessionId}` | code, courseId, section, isActive, expiresAt |
| `attendance` | `{srn_course_sem_subject_date}` | srn, subject, date, status, markedVia |
| `courses` | `{courseId}` | name, fullName, totalSemesters, sections |

---

## 🐛 Troubleshooting

| Problem | Fix |
|---------|-----|
| `Face recognition model not loaded` | Place `facenet.tflite` in `app/src/main/assets/` |
| Face registered in dark, now fails | Delete `face_embeddings/{srn}` in Firestore Console and re-register |
| `Wrong Class` when marking attendance | Student's courseId/section/semester must match the session exactly |
| App crashes on camera open | Grant CAMERA permission in device Settings |
| Build fails: `No set method providing array access` | Use the latest `FaceRepository.kt` — requires explicit loop syntax for the `:data` module |
| IT Fundamentals shows 0% | Student was absent for all sessions — no bug, data is correct |
| Low attendance warning not disappearing | Warning only clears when overall % ≥ 75%; update attendance records first |

---

## 🤝 Contributing

```bash
# Create a feature branch
git checkout -b feature/your-feature

# Commit with conventional commits
git commit -m "feat: add your feature"

# Push and open a PR
git push origin feature/your-feature
```

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with Kotlin · Firebase · TensorFlow Lite · CameraX · ML Kit

</div>
