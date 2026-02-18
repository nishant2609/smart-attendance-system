package com.nishant.smartattendance.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseProvider {

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
}
