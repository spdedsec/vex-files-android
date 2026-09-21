package com.vex.files.core

import java.io.File

sealed class FileItem(open val file: File) {
    val name: String get() = file.name.ifBlank { "/" }
    val isDir: Boolean get() = file.isDirectory
    val size: Long get() = if (isDir) 0L else file.length()
}

class FsFile(override val file: File) : FileItem(file)
class FsDirectory(override val file: File) : FileItem(file)
