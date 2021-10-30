package com.willstep.ptutools.dataaccess.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import java.util.*


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

    open fun getDocuments(collectionName: String, fieldName: String, fieldValues: List<String>, doSort: Boolean): MutableList<QueryDocumentSnapshot> {
        if (fieldValues.isEmpty()) {
            return ArrayList()
        }

        var resultList = LinkedList<QueryDocumentSnapshot>()
        for (i in fieldValues.indices step 10) {
            var q = db.collection(collectionName).whereIn(fieldName,
                fieldValues.subList(i, if (fieldValues.size > i+10)  i+10 else fieldValues.size))
            if (doSort) {
                q = q.orderBy(FieldPath.documentId())
            }
            resultList.addAll(q.get().get().documents)
        }
        return resultList
    }
}
