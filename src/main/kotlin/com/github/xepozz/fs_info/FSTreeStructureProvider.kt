package com.github.xepozz.fs_info

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.text.DecimalFormat

class FSTreeStructureProvider(private val project: Project) : TreeStructureProvider {
    private val settings by lazy { project.getService(FsInfoSettings::class.java) }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        viewSettings: ViewSettings,
    ) = children.apply { children.forEach(::processNode) }

    private fun processNode(node: AbstractTreeNode<*>) {
        when (node) {
            is PsiDirectoryNode -> {
                node.children.forEach(::processNode)

                if (settings.showDirectoryItems) {
                    node.presentation.apply {
                        if (locationString == null) {
                            node.children.size.apply {
                                if (this > 0) {
                                    locationString = " $this items"
                                }
                            }
                        }
                    }
                }
            }

            is PsiFileNode -> {
                node.presentation.apply {
                    if (locationString != null) {
                        return@apply
                    }

                    buildList {
                        getFileSize(node.value).apply {
                            if (this > 0) {
                                val (size, type) = formatFileSize(this)

                                if (
                                    (type == FileSizeType.BYTES && !settings.showBytes)
                                    || (type == FileSizeType.KILOBYTES && !settings.showKBytes)
                                    || (type == FileSizeType.MEGABYTES && !settings.showMBytes)
                                    || (type == FileSizeType.GIGABYTES && !settings.showGBytes)
                                ) {
                                    return@apply
                                }

                                val preciseSize = size / type.bytes.toDouble()
                                add(String.format("%s%s", DecimalFormat("#.#").format(preciseSize), type.suffix))

                            }
                        }

                        if (settings.showLines) {
                            node.value.fileDocument.lineCount.apply {
                                if (this > 0) {
                                    add("$this lines")
                                }
                            }
                        }
                    }.apply {
                        if (isNotEmpty()) {
                            locationString = joinToString(" | ")
                            node.update()
                        }
                    }
                }
            }
        }
    }

    private fun getFileSize(psiFile: PsiFile) = psiFile.virtualFile.length

    private fun formatFileSize(size: Long): Pair<Long, FileSizeType> {
        return when {
            size < FileSizeType.KILOBYTES.bytes -> Pair(size, FileSizeType.BYTES)
            size < FileSizeType.MEGABYTES.bytes -> Pair(size, FileSizeType.KILOBYTES)
            size < FileSizeType.GIGABYTES.bytes -> Pair(size, FileSizeType.MEGABYTES)
            else -> Pair(size, FileSizeType.GIGABYTES)
        }
    }
}

enum class FileSizeType(val suffix: String, val bytes: Long) {
    BYTES("B", 1),
    KILOBYTES("KB", 1024),
    MEGABYTES("MB", 1024 * 1024),
    GIGABYTES("GB", 1024 * 1024 * 1024)
}