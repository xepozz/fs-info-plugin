package com.github.xepozz.fs_info.files

import com.intellij.util.io.IOUtil

enum class FileSizeType(val bytes: ULong) {
    BYTES(1.toULong()),
    KILOBYTES(IOUtil.KiB.toULong()),
    MEGABYTES(IOUtil.MiB.toULong()),
    GIGABYTES(IOUtil.GiB.toULong()),
}