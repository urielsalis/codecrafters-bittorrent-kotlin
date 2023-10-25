package com.urielsalis.codecrafters.bittorent.metainfo

data class MetaInfo(
    val announce: String,
    val infoHash: ByteArray,
    val fileLength: Long,
    val fileName: String,
    val pieceLength: Int,
    val pieces: List<ByteArray>
) {

    // FIXME: Overriding due to having an array as parameter, replace once Kotlin supports autogenerating them
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MetaInfo) return false

        if (announce != other.announce) return false
        if (!infoHash.contentEquals(other.infoHash)) return false
        if (fileLength != other.fileLength) return false
        if (fileName != other.fileName) return false
        if (pieceLength != other.pieceLength) return false
        if (pieces != other.pieces) return false

        return true
    }

    override fun hashCode(): Int {
        var result = announce.hashCode()
        result = 31 * result + infoHash.contentHashCode()
        result = 31 * result + fileLength.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + pieceLength
        result = 31 * result + pieces.hashCode()
        return result
    }
}
