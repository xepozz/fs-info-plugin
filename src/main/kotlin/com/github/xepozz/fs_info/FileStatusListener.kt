package com.github.xepozz.fs_info

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatusListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FileStatusListener(
    val project: Project,
    private val coroutineScope: CoroutineScope,
) : FileStatusListener {
    val fileSystemService: FileSystemService = project.getService(FileSystemService::class.java)
    var job: Job? = null

    override fun fileStatusesChanged() {
        job?.cancel()
        job = coroutineScope.launch(Dispatchers.IO) {
            fileSystemService.refresh()
        }
    }
}