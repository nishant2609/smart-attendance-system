package com.nishant.smartattendance.data.remote

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreDataSource {

    private val firestore = FirebaseFirestore.getInstance()

    fun getFirestore(): FirebaseFirestore = firestore
}
