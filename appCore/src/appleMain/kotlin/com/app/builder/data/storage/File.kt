package com.app.builder.data.storage

import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.lastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry

actual suspend fun saveFile(path: String, content: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        NSString.create(string = content).writeToFile(
            path = file,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to save file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun loadFile(path: String): String? = withContext(context = Dispatcher.IO) {
    runCatching {
        val fullPath = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path = fullPath)) {
            NSString.stringWithContentsOfFile(
                path = fullPath,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        } else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load file $path", throwable = it)
    }.getOrNull()
}

actual suspend fun deleteFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val fullPath = NSString.create(string = appDataPath).stringByAppendingPathComponent(str = path)
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path = fullPath)) {
            fileManager.removeItemAtPath(
                path = fullPath,
                error = null
            )
        } else true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to delete file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun clearCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val fileManager = NSFileManager.defaultManager
        fileManager.contentsOfDirectoryAtPath(path = appCachePath, error = null)
            ?.filterIsInstance<String>()
            ?.forEach {
                val itemPath = NSString.create(string = appCachePath).stringByAppendingPathComponent(str = it)
                fileManager.removeItemAtPath(path = itemPath, error = null)
            }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun listFiles(path: String): List<DeviceFile> = withContext(context = Dispatcher.IO) {
    runCatching {
        val fileManager = NSFileManager.defaultManager
        fileManager.subpathsAtPath(path = path)
            .orEmpty()
            .filterIsInstance<String>()
            .mapNotNull { subpath ->
                val fullPath = NSString.create(string = path).stringByAppendingPathComponent(str = subpath)
                val attributes = fileManager.attributesOfItemAtPath(path = fullPath, error = null) ?: return@mapNotNull null
                if (attributes[NSFileType] != NSFileTypeRegular) return@mapNotNull null
                DeviceFile(
                    path = fullPath,
                    name = NSString.create(string = fullPath).lastPathComponent,
                    size = (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L,
                    modifiedAt = (attributes[NSFileModificationDate] as? NSDate)?.let { (it.timeIntervalSince1970 * 1000).toLong() }
                )
            }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to list files in $path", throwable = it)
    }.getOrDefault(defaultValue = emptyList())
}

actual suspend fun openFile(path: String): Boolean = withContext(context = Dispatcher.Main) {
    runCatching {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path = path)) error(message = "File does not exist")
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: error(message = "No root view controller")
        val controller = UIDocumentInteractionController.interactionControllerWithURL(url = NSURL.fileURLWithPath(path = path))
        controller.delegate = documentDelegate
        documentController = controller
        controller.presentPreviewAnimated(animated = true) || controller.presentOptionsMenuFromRect(
            rect = viewController.view.bounds,
            inView = viewController.view,
            animated = true
        )
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to open file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

/** Holds the controller presenting the current preview, which UIKit does not retain on its own. */
private var documentController: UIDocumentInteractionController? = null

/** Delegate handing UIKit the view controller the preview is presented from. */
private val documentDelegate: UIDocumentInteractionControllerDelegateProtocol = DocumentDelegate()

/** Resolves the view controller a [UIDocumentInteractionController] preview is presented from. */
private class DocumentDelegate: NSObject(), UIDocumentInteractionControllerDelegateProtocol {
    override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController =
        UIApplication.sharedApplication.keyWindow?.rootViewController ?: UIViewController()

    override fun documentInteractionControllerDidEndPreview(controller: UIDocumentInteractionController) {
        documentController = null
    }
}

private const val TAG = "File"
