package com.willstep.ptutools.dataaccess.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions


var firestoreOptions: FirestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("ptu-tools-308700")
    .setCredentials(GoogleCredentials.getApplicationDefault())
    .build()
var db: Firestore = firestoreOptions.service

open class FirestoreService {
    open fun saveAsDocument(collectionName: String, documentName: String, obj: Any) {
        db.collection(collectionName).document(documentName).set(obj)
    }

    open fun getCollection(collectionName: String): CollectionReference {
        return db.collection(collectionName)
    }

    open fun getDocument(collectionName: String, documentName: String): DocumentReference {
        return db.collection(collectionName).document(documentName)
    }
}
