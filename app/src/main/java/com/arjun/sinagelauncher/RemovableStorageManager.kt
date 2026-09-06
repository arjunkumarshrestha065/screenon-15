package com.arjun.sinagelauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

class RemovableStorageManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "signage_storage_prefs"
        private const val KEY_TREE_URI = "selected_removable_tree_uri"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun createSelectFolderIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        return intent
    }

    fun saveSelectedFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }

    fun getTreeUri(): Uri? {
        val saved = prefs.getString(KEY_TREE_URI, null) ?: return null
        return Uri.parse(saved)
    }

    fun isStorageReady(): Boolean {
        val treeUri = getTreeUri() ?: return false
        return try {
            val childrenUri = getChildrenUri(treeUri)
            context.contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use {
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun listFileNames(): List<String> {
        val treeUri = getTreeUri() ?: return emptyList()
        val result = mutableListOf<String>()

        try {
            val childrenUri = getChildrenUri(treeUri)
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) result.add(name)
                    }
                }
            }
        } catch (_: Exception) {}

        return result
    }

    fun findFileUri(fileName: String): Uri? {
        val treeUri = getTreeUri() ?: return null

        try {
            val childrenUri = getChildrenUri(treeUri)
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val id = if (idIndex >= 0) cursor.getString(idIndex) else null
                    if (name == fileName && !id.isNullOrBlank()) {
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun deleteFile(fileName: String): Boolean {
        val uri = findFileUri(fileName) ?: return false
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {
            false
        }
    }

    fun createOrReplaceFile(fileName: String, mimeType: String): Uri? {
        val treeUri = getTreeUri() ?: return null

        try {
            val oldUri = findFileUri(fileName)
            if (oldUri != null) {
                try { DocumentsContract.deleteDocument(context.contentResolver, oldUri) } catch (_: Exception) {}
            }

            val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId)
            return DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, fileName)
        } catch (_: Exception) {
            return null
        }
    }

    private fun getChildrenUri(treeUri: Uri): Uri {
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        return DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    }
}
