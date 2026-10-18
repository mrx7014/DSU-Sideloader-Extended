package vegabobo.dsusideloader.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.topjohnwu.superuser.Shell

/**
 * Configures the host-side folder used by the Magisk service to bind-mount
 * host storage into a running DSU guest.
 *
 * The configuration is deliberately stored below /data/media/0 so it is
 * visible through Android's DSU pass-through mount. This feature requires
 * root and a device/GSI exposing that mount.
 */
object SharedFolderManager {
    const val CONFIG_DIRECTORY = "/data/media/0/DSU-Sideloader"
    const val CONFIG_FILE = "$CONFIG_DIRECTORY/.shared_folder"
    const val DISPLAY_DIRECTORY = "DSU-Sideloader"

    fun getPrimaryStorageRelativePath(context: Context, treeUri: Uri): String? {
        val document = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        if (!document.canRead() || !document.canWrite()) return null

        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        if (!documentId.startsWith("primary:")) return null
        return documentId.removePrefix("primary:").trim('/').takeIf { it.isNotEmpty() }
    }

    fun setConfiguredPath(relativePath: String): Boolean {
        if (!isSafeRelativePath(relativePath)) return false
        val quotedPath = shellQuote(relativePath)
        val command = "mkdir -p $CONFIG_DIRECTORY && printf '%s' $quotedPath > $CONFIG_FILE"
        return Shell.cmd(command).exec().isSuccess
    }

    fun clearConfiguredPath(): Boolean =
        Shell.cmd("rm -f $CONFIG_FILE").exec().isSuccess

    fun unmount(relativePath: String): Boolean {
        if (!isSafeRelativePath(relativePath)) return false
        val target = shellQuote("/storage/emulated/0/$relativePath")
        return Shell.cmd("umount $target 2>/dev/null || true").exec().isSuccess
    }

    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() &&
            !path.startsWith('/') &&
            path.split('/').none { it.isEmpty() || it == "." || it == ".." }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
