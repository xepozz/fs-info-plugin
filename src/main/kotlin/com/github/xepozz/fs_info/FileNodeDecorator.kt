package com.github.xepozz.fs_info

import com.github.xepozz.fs_info.files.FileNodeDescriptor
import com.github.xepozz.fs_info.files.FileSizeType
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.FileStatus
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.SmartPsiElementPointer

class FileNodeDecorator(val project: Project) : ProjectViewNodeDecorator {
    private val settings by lazy { project.getService(FsInfoSettings::class.java) }
    private val fileSystemService by lazy { project.getService(FileSystemService::class.java) }
    private val projectRootManager by lazy { ProjectRootManager.getInstance(project) }

    override fun decorate(
        node: ProjectViewNode<*>,
        presentation: PresentationData
    ) {
        if (isNodeIgnored(node)) return

        val psiFile = node.value
        if (psiFile is PsiFileSystemItem && !psiFile.isPhysical) return

        val virtualFile = node.virtualFile
            ?: (node.equalityObject as? SmartPsiElementPointer<*>)?.virtualFile
            ?: return

        if (projectRootManager.fileIndex.isExcluded(virtualFile)) return

        val fileNodeDescriptor = fileSystemService.findDescriptor(virtualFile) ?: return

//        println("decorate: ${node.name}, ${psiFile::class.java}")

        when (psiFile) {
            is PsiDirectory -> decorateDirectoryPresentation(presentation, fileNodeDescriptor)
            is PsiFileSystemItem -> decorateFilePresentation(presentation, fileNodeDescriptor)

            else -> {
                println("unknown node: ${node.javaClass.name}")

                decorateFilePresentation(presentation, fileNodeDescriptor)
            }
        }
    }

    private fun decorateDirectoryPresentation(
        presentation: PresentationData,
        params: FileNodeDescriptor,
    ) {
        val directorySize = params.directorySize
        val directoryItems = params.childrenSize
        val fileSizeType = getFileSizeType(directorySize)

        buildList {
            presentation.locationString?.apply { add(this) }

            if (directorySize > 0u && settings.showDirectorySize && shouldShowSize(fileSizeType)) {
                add(StringUtil.formatFileSize(directorySize.toLong()))
            }

            if (directoryItems > 0 && settings.showDirectoryItemsAmount) {
                add(FsInfoBundle.message("items", directoryItems))
            }
        }.apply {
            if (isNotEmpty()) {
                val joinToString = joinToString(" | ")
                presentation.locationString = joinToString
            }
        }
    }

    private fun decorateFilePresentation(
        presentation: PresentationData,
        params: FileNodeDescriptor,
    ) {
        val fileSize = params.size
        val lineCount = params.lines

        buildList {
            presentation.locationString?.apply { add(this) }
            if (fileSize > 0u) {
                val fileSizeType = getFileSizeType(fileSize)

                if (shouldShowSize(fileSizeType)) {
                    add(StringUtil.formatFileSize(fileSize.toLong()))
                }
            }

            if (lineCount > 0 && settings.showLines) {
                add(FsInfoBundle.message("lines", lineCount))
            }
        }.apply {
            if (isNotEmpty()) {
                presentation.locationString = joinToString(" | ")
            }
        }
    }

    private fun isNodeIgnored(node: ProjectViewNode<*>) = node.run {
        fileStatus == FileStatus.IGNORED && parent?.fileStatus == FileStatus.IGNORED
    }

    private fun shouldShowSize(type: FileSizeType) = when (type) {
        FileSizeType.BYTES -> settings.showBytes
        FileSizeType.KILOBYTES -> settings.showKBytes
        FileSizeType.MEGABYTES -> settings.showMBytes
        FileSizeType.GIGABYTES -> settings.showGBytes
    }

    private fun getFileSizeType(size: ULong): FileSizeType = when {
        size < FileSizeType.KILOBYTES.bytes -> FileSizeType.BYTES
        size < FileSizeType.MEGABYTES.bytes -> FileSizeType.KILOBYTES
        size < FileSizeType.GIGABYTES.bytes -> FileSizeType.MEGABYTES
        else -> FileSizeType.GIGABYTES
    }
}

