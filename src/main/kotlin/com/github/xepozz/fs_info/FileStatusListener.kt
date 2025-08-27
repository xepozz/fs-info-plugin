package com.github.xepozz.fs_info

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatusListener
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileStatusListener(
    val project: Project,
    private val coroutineScope: CoroutineScope,
) : FileStatusListener {
    val fileSystemService: FileSystemService = project.getService(FileSystemService::class.java)

    val queue = MergingUpdateQueue("FileSystemCollector.Queue", 200, true, null)
    val update = object : Update(this) {
        override fun run() {
            coroutineScope.launch(Dispatchers.IO) {
                fileSystemService.refresh()
            }
        }

        override fun canEat(update: Update) = true
    }

    override fun fileStatusesChanged() {
        queue.cancelAllUpdates()
        queue.queue(update)
    }
}