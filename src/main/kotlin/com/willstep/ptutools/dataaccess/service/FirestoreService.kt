package com.willstep.ptutools.dataaccess.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.util.*


var firestoreOptions: FirestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("ptu-tools-308700")
    .setCredentials(GoogleCredentials.getApplicationDefault())
    .build()
var db: Firestore = firestoreOptions.service

class FirestoreService {
    fun saveAsDocument(collectionName: String, documentName: String, obj: Any) {
        db.collection(collectionName).document(documentName).set(obj)
    }

    fun getCollection(collectionName: String): CollectionReference {
        return db.collection(collectionName)
    }

    fun getDocument(collectionName: String, documentName: String): DocumentReference {
        return db.collection(collectionName).document(documentName)
    }
}
