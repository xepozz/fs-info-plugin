package com.github.xepozz.fs_info.files

import java.nio.file.Path

data class FileNodeDescriptor(
    val path: Path,
    var size: ULong = 0u,
    var lines: Int = 0,
    val isDirectory: Boolean,
    val children: MutableMap<String, FileNodeDescriptor> = mutableMapOf(),
    var parent: FileNodeDescriptor? = null
) {
    val childrenSize: Int
        get() = children.values.sumOf { if (it.isDirectory) it.childrenSize else 1 }

    val fileCount: Int
        get() = if (isDirectory) {
            children.values.sumOf { if (it.isDirectory) it.fileCount else 1 }
        } else 1

    val directorySize: ULong
        get() = if (isDirectory) {
            children.values.sumOf { if (it.isDirectory) it.directorySize else it.size }
        } else 1.toULong()
}
