package com.github.xepozz.fs_info

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class StartupListener : ProjectActivity {
    override suspend fun execute(project: Project) {
        val fileSystemService = project.getService(FileSystemService::class.java)
        val settings = project.getService(FsInfoSettings::class.java)

        if (!settings.enabled) return

        coroutineScope {
            withContext(Dispatchers.IO) {
                fileSystemService.refresh()
            }
        }
    }
}