package com.urielsalis.codecrafters.bittorent.magnet

data class MagnetMetadata(
    val fileLength: Long, val pieceLength: Int, val pieces: List<ByteArray>
)
