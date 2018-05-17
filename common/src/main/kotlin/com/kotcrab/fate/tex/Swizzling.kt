package com.kotcrab.fate.tex

import com.kotcrab.fate.io.SequentialArrayReader
import com.kotcrab.fate.io.SequentialArrayWriter
import java.util.*

/** @author Kotcrab */
object Swizzling {
    fun unswizzle8BPP(swizzledBytes: ByteArray, width: Int, height: Int): ByteArray {
        return Swizzling.unswizzle(swizzledBytes, width, height, width / 16)
    }

    fun unswizzle4BPP(swizzledBytes: ByteArray, width: Int, height: Int): ByteArray {
        return Swizzling.unswizzle(swizzledBytes, width, height, width / 32)
    }

    fun unswizzle(swizzledBytes: ByteArray, width: Int, height: Int, blocksPerChunk: Int): ByteArray {
        val blockWidth = 16
        val blockHeight = 8
        val blockSize = blockWidth * blockHeight
        val availableBlocks = swizzledBytes.size / blockSize
        val blocks = ArrayList<ByteArray>(availableBlocks)
        repeat(availableBlocks) { blockIdx ->
            val blockStart = blockSize * blockIdx
            blocks.add(Arrays.copyOfRange(swizzledBytes, blockStart, blockStart + blockSize))
        }

        val availableChunks = availableBlocks / blocksPerChunk
        val chunks = ArrayList<ByteArray>(availableChunks)
        repeat(availableChunks) { chunkIdx ->
            val chunk = ByteArray(blockSize * blocksPerChunk)
            chunks.add(chunk)
            val chunkWriter = SequentialArrayWriter(chunk)

            var blockIdx = 0
            val startBlockIndex = chunkIdx * blocksPerChunk + blockIdx
            val endBlockIndex = startBlockIndex + blocksPerChunk
            val chunkBlocks = blocks.slice(startBlockIndex until endBlockIndex).map { SequentialArrayReader(it) }
            blockIdx += blocksPerChunk

            repeat(blockHeight) {
                repeat(blocksPerChunk) { idx ->
                    repeat(blockWidth) {
                        chunkWriter.write(chunkBlocks[idx].read())
                    }
                }
            }
        }

        val unswizzledBytes = ByteArray(width * height)
        val outWriter = SequentialArrayWriter(unswizzledBytes)
        chunks.forEach { chunkData ->
            chunkData.forEach { byte ->
                outWriter.write(byte)
            }
        }
        return unswizzledBytes
    }
}

