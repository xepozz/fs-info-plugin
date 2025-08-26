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
import com.intellij.util.io.IOUtil

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

                val size = sizeAccumulator.size
                val fileSizeType = getFileSizeType(size)

//                println("file1: ${name} size: ${size}")

                buildList {
                    presentation.locationString?.apply { add(this) }

                    if (size > 0u && shouldShowSize(fileSizeType)) {
                        add(StringUtil.formatFileSize(size.toLong()))
                    }

                    if (!isIgnored && settings.showDirectoryItems) {
                        node.children.size.apply {
                            if (this > 0) {
                                add("$this items")
                            }
                        }
                    }
                }.apply {
                    if (isNotEmpty() && !isIgnored) {
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

                buildList {
                    presentation.locationString?.apply { add(this) }
                    getFileSize(psiFile).apply {
                        if (this > 0u) {
//                            println("file2: ${node.value.name} size: ${this}")
                            sizeAccumulator.size += this

                            val size = this
                            val fileSizeType = getFileSizeType(size)

                            if (shouldShowSize(fileSizeType)) {
                                add(StringUtil.formatFileSize(size.toLong()))
                            }
                        }
                    }

                    if (settings.showLines) {
                        val lineCount = try {
                            node.value.viewProvider.document?.lineCount
                        } catch (_: java.lang.IllegalStateException) {
                            0
                        }

                        if (lineCount != null && lineCount > 0) {
                            add("$lineCount lines")
                        }
                    }
                }.apply {
                    if (projectRootManager.fileIndex.isExcluded(psiFile.virtualFile)) {
                        return@apply
                    }
                    if (isNotEmpty()) {
                        presentation.locationString = joinToString(" | ")
                        node.update()
                    }
                }
            }

            else -> println("unknown node: ${node.javaClass.name} ${node.value.javaClass.name} ${node.value}")
        }
    }

    private fun shouldShowSize(type: FileSizeType) = when (type) {
        FileSizeType.BYTES -> settings.showBytes
        FileSizeType.KILOBYTES -> settings.showKBytes
        FileSizeType.MEGABYTES -> settings.showMBytes
        FileSizeType.GIGABYTES -> settings.showGBytes
    }

    private fun getFileSize(psiFile: PsiFile): ULong {
        return try {
            psiFile.virtualFile.length
        } catch (_: java.nio.file.NoSuchFileException) {
            0
        }.toULong()
    }

    private fun getFileSizeType(size: ULong): FileSizeType = when {
        size < FileSizeType.KILOBYTES.bytes -> FileSizeType.BYTES
        size < FileSizeType.MEGABYTES.bytes -> FileSizeType.KILOBYTES
        size < FileSizeType.GIGABYTES.bytes -> FileSizeType.MEGABYTES
        else -> FileSizeType.GIGABYTES
    }
}

enum class FileSizeType(val bytes: ULong) {
    BYTES(1.toULong()),
    KILOBYTES(IOUtil.KiB.toULong()),
    MEGABYTES(IOUtil.MiB.toULong()),
    GIGABYTES(IOUtil.GiB.toULong()),
}

private data class SizeAccumulator(var size: ULong)