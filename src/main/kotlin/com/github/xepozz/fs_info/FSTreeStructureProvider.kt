package com.github.xepozz.fs_info

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.FileStatus
import com.intellij.psi.PsiFile

class FSTreeStructureProvider(private val project: Project) : TreeStructureProvider {
    private val settings by lazy { project.getService(FsInfoSettings::class.java) }
    private val projectRootManager by lazy { ProjectRootManager.getInstance(project) }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        viewSettings: ViewSettings,
    ): MutableCollection<AbstractTreeNode<*>> {
        if (parent !is PsiDirectoryNode && parent.parent !is ProjectViewProjectNode) return children
//        println("modify: ${parent.name} with ${children.size} children")

        children.forEach(::processNode)

        return children
    }

    private fun processNode(node: AbstractTreeNode<*>) {
        val sizeAccumulator = SizeAccumulator(0.toULong())

        processNode(node, sizeAccumulator)
    }

    private fun processNode(node: AbstractTreeNode<*>, sizeAccumulator: SizeAccumulator) {
        val presentation = node.presentation

        when (node) {
//            is FileNodeWithNestedFileNodes -> {
//                node.children.forEach {
//                    val localSizeAccumulator = SizeAccumulator(0.toULong())
//                    processNode(it, localSizeAccumulator)
//                    sizeAccumulator.size += localSizeAccumulator.size
//
//                    presentation.locationString = "compound"
//                }
//            }
            is PsiDirectoryNode -> {
                val psiDirectory = node.value
                val name = psiDirectory.name
//                println("name: ${name}, stat: ${node.leafState} ${node.fileStatus}")
                if (!psiDirectory.isPhysical) return

                node.children.forEach {
                    val localSizeAccumulator = SizeAccumulator(0.toULong())
                    processNode(it, localSizeAccumulator)
                    sizeAccumulator.size += localSizeAccumulator.size
                }
                val isIgnored = node.fileStatus == FileStatus.IGNORED &&
                        node.parent?.fileStatus == FileStatus.IGNORED

                if (isIgnored) return

                val size = sizeAccumulator.size
                val fileSizeType = getFileSizeType(size)

//                println("file1: ${name} size: ${size}")

                buildList {
                    presentation.locationString?.apply { add(this) }

                    if (size > 0u && settings.showDirectorySize && shouldShowSize(fileSizeType)) {
                        add(StringUtil.formatFileSize(size.toLong()))
                    }

                    val directoryItems = node.children.size
                    if (directoryItems > 0 && settings.showDirectoryItemsAmount) {
                        add("$directoryItems items")
                    }
                }.apply {
                    if (isNotEmpty()) {
                        val joinToString = joinToString(" | ")
                        presentation.locationString = joinToString
                        node.update()
                    }
                }
            }

            is PsiFileNode -> {
                val psiFile = node.value
                val name = psiFile.name

                if (!psiFile.isPhysical) return

                val fileSize = getFileSize(psiFile)
                sizeAccumulator.size += fileSize

                val isIgnored = node.fileStatus == FileStatus.IGNORED &&
                        node.parent?.fileStatus == FileStatus.IGNORED

                if (isIgnored) return

                if (projectRootManager.fileIndex.isExcluded(psiFile.virtualFile)) {
//                    println("file ${psiFile.virtualFile} is excluded")
                    return
                }
                buildList {
                    presentation.locationString?.apply { add(this) }
                    if (fileSize > 0u) {
//                            println("file2: ${node.value.name} size: ${this}")

                        val fileSizeType = getFileSizeType(fileSize)

                        if (shouldShowSize(fileSizeType)) {
                            add(StringUtil.formatFileSize(fileSize.toLong()))
                        }
                    }

                    if (settings.showLines) {
                        val lineCount = try {
                            psiFile.viewProvider.document?.lineCount
                        } catch (_: java.lang.IllegalStateException) {
                            0
                        }

                        if (lineCount != null && lineCount > 0) {
                            add("$lineCount lines")
                        }
                    }
                }.apply {
                    if (isNotEmpty()) {
                        presentation.locationString = joinToString(" | ")
                        node.update()
                    }
                }
            }

            else -> println("unknown node: ${node.javaClass.name} ${node.value.javaClass.name} ${node.value}")
        }
    }

    private fun getFileSize(psiFile: PsiFile): ULong {
        return try {
            psiFile.virtualFile.length
        } catch (_: java.nio.file.NoSuchFileException) {
            0
        }.toULong()
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

private data class SizeAccumulator(var size: ULong)