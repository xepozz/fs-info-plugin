package com.github.xepozz.fs_info

import com.github.xepozz.fs_info.files.FileNodeDescriptor
import com.github.xepozz.fs_info.files.FileSystemStructureCollector
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

@Service(value = [Service.Level.PROJECT])
class FileSystemService(val project: Project) {
    var collector = FileSystemStructureCollector()

    fun refresh() {
        val projectDirectory = project.guessProjectDir() ?: return

        collector.refresh(projectDirectory.toNioPath())
    }

    fun findDescriptor(virtualFile: VirtualFile): FileNodeDescriptor? {
        return collector.getNode(virtualFile.toNioPath())
    }
}