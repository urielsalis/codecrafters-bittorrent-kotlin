package com.urielsalis.codecrafters.bittorent.peer.domain

data class PeerPartialPieceResponse(val pieceIndex: Int, val begin: Int, val bytes: ByteArray) {

    // FIXME: Remove once data classes support equals of arrays
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeerPartialPieceResponse) return false

        if (pieceIndex != other.pieceIndex) return false
        if (begin != other.begin) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pieceIndex
        result = 31 * result + begin.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
