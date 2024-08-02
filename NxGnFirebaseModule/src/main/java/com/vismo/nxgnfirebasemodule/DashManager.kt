package com.vismo.nxgnfirebasemodule

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class DashManager @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    fun startTrip() {
        firestore.collection("meters").document("CABLE01").set(
            hashMapOf(
                "trip" to true,
                "firestore" to "is working"
            )
        )
    }
}