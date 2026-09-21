package com.vex.files.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOps {
    fun copy(source: File, destination: File) {
        if (source.isDirectory) {
            val target = File(destination, source.name)
            target.mkdirs()
            source.listFiles()?.forEach { child -> copy(child, target) }
        } else {
            val target = if (destination.isDirectory) File(destination, source.name) else destination
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
        }
    }

    fun move(source: File, destination: File) {
        val target = if (destination.isDirectory) File(destination, source.name) else destination
        target.parentFile?.mkdirs()
        if (!source.renameTo(target)) {
            copy(source, destination)
            delete(source)
        }
    }

    fun delete(file: File): Boolean = file.deleteRecursively()

    fun createZip(inputs: List<File>, output: File) {
        output.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            inputs.forEach { input -> addToZip(zip, input, input.name) }
        }
    }

    private fun addToZip(zip: ZipOutputStream, file: File, path: String) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: emptyArray()
            if (children.isEmpty()) {
                zip.putNextEntry(ZipEntry("$path/"))
                zip.closeEntry()
            } else {
                children.forEach { addToZip(zip, it, "$path/${it.name}") }
            }
            return
        }
        zip.putNextEntry(ZipEntry(path))
        FileInputStream(file).use { it.copyTo(zip, 64 * 1024) }
        zip.closeEntry()
    }

    fun extractZip(zipFile: File, destination: File) {
        destination.mkdirs()
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(destination, entry.name).canonicalFile
                val root = destination.canonicalFile
                require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
                    "Unsafe zip path"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> zip.copyTo(out, 64 * 1024) }
                }
                zip.closeEntry()
            }
        }
    }
}
