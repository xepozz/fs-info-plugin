package com.github.xepozz.fs_info.files

import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.fileVisitor
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.visitFileTree


class FileSystemStructureCollector() {
    private val nodeMap = mutableMapOf<Path, FileNodeDescriptor>()

    fun getNode(path: Path): FileNodeDescriptor? = nodeMap[path]

    fun refresh(projectPath: Path) {
        nodeMap[projectPath] = FileNodeDescriptor(
            path = projectPath,
            isDirectory = true
        )
        projectPath.visitFileTree(structureVisitor)
    }

    val structureVisitor = fileVisitor {
        onPreVisitDirectory { directory, attributes ->
            val parentPath = directory.parent
            val parentNode = nodeMap[parentPath]

            val dirNode = FileNodeDescriptor(
                path = directory,
                isDirectory = true,
                parent = parentNode,
            )

            nodeMap[directory] = dirNode
            parentNode?.children?.put(directory.name, dirNode)

            if (directory.name in setOf("build", ".git", "node_modules", ".idea")) {
                FileVisitResult.SKIP_SUBTREE
            } else {
                FileVisitResult.CONTINUE
            }
        }

        onVisitFile { file, attributes ->
            val parentPath = file.parent
            val parentNode = nodeMap[parentPath]

            val fileSize = attributes.size().toULong()
            val lines = if (fileSize > FileSizeType.MEGABYTES.bytes * 10.toULong()) 0 else countLines(file)

            val fileNode = FileNodeDescriptor(
                path = file,
                size = fileSize,
                lines = lines,
                isDirectory = false,
                parent = parentNode
            )

            nodeMap[file] = fileNode
            parentNode?.children?.put(file.name, fileNode)

            FileVisitResult.CONTINUE
        }

        onPostVisitDirectory { directory, exception ->
            FileVisitResult.CONTINUE
        }

        onVisitFileFailed { file, exception ->
            println("Failed to visit: $file, reason: $exception")
            FileVisitResult.CONTINUE
        }
    }

    fun countLines(path: Path): Int {
        var lines = 1

        val buffer = ByteArray(8 * FileSizeType.KILOBYTES.bytes.toInt())
        path.inputStream().apply {
            while ((read(buffer)) != -1) {
                lines += buffer.count { it == '\n'.code.toByte() }
            }

            close()
        }
        return lines
    }
}