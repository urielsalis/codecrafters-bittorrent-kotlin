package com.urielsalis.codecrafters.bittorent.peer

import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfo
import com.urielsalis.codecrafters.bittorent.peer.domain.Peer
import com.urielsalis.codecrafters.bittorent.peer.domain.PeerPartialPieceRequest
import com.urielsalis.codecrafters.bittorent.peer.domain.PeerPartialPieceResponse
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val MAX_PARTIAL_PIECE_LENGTH = 16384 // 2^14

class ConnectionManager(val metaInfo: MetaInfo, peers: List<Peer>) {
    val connections: Map<Int, PeerConnection>
    val workQueue = mutableListOf<PeerPartialPieceRequest>()
    val receivedPieces = mutableListOf<PeerPartialPieceResponse>()
    val lock = Any()

    init {
        connections = peers.mapIndexed { i, peer -> i to PeerConnection(peer) }.toMap()
    }

    fun connect() {
        connections.values.forEach {
            it.handshake(metaInfo.infoHash)
            it.initConnection()
            it.markInterested()
        }
    }

    fun requestPiece(pieceNumber: Int) {
        workQueue.addAll(getRequests(pieceNumber))
    }

    fun requestAllPieces() {
        for (i in 0 until metaInfo.pieces.size) {
            requestPiece(i)
        }
    }

    fun download() {
        val peers = connections.values
        val threads = peers.map {
            thread(start = true) {
                it.download({ getNextItem() }) { downloaded ->
                    synchronized(receivedPieces) {
                        receivedPieces.add(downloaded)
                    }
                }
            }
        }
        while (threads.any { it.isAlive }) {
            TimeUnit.MILLISECONDS.sleep(1)
        }
    }

    fun getPiece(pieceNumber: Int): ByteArray {
        val ret = ByteArray(getLengthOfPiece(pieceNumber))
        receivedPieces.filter { it.pieceIndex == pieceNumber }.sortedBy { it.begin }.forEach {
            System.arraycopy(
                it.bytes, 0, ret, it.begin, it.bytes.size
            )
        }
        return ret
    }

    fun writeToFile(fileName: String) {
        val out = FileOutputStream(fileName)
        for (i in 0 until metaInfo.pieces.size) {
            val bytes = getPiece(i)
            out.write(bytes)
        }
        out.close()
    }

    private fun getNextItem(): PeerPartialPieceRequest? =
        synchronized(lock) { workQueue.removeFirstOrNull() }

    private fun getRequests(pieceIndex: Int): List<PeerPartialPieceRequest> {
        val pieceLength = getLengthOfPiece(pieceIndex)
        val fullPartialPieces = pieceLength / MAX_PARTIAL_PIECE_LENGTH
        val remainder = pieceLength % MAX_PARTIAL_PIECE_LENGTH
        val ret = mutableListOf<PeerPartialPieceRequest>()
        for (i in 0 until fullPartialPieces) {
            ret.add(
                PeerPartialPieceRequest(
                    pieceIndex, i * MAX_PARTIAL_PIECE_LENGTH, MAX_PARTIAL_PIECE_LENGTH
                )
            )
        }
        if (remainder != 0) {
            ret.add(
                PeerPartialPieceRequest(
                    pieceIndex, fullPartialPieces * MAX_PARTIAL_PIECE_LENGTH, remainder
                )
            )
        }
        return ret
    }

    private fun getLengthOfPiece(pieceNumber: Int): Int = if (isLastPiece(pieceNumber)) {
        val remainder = metaInfo.fileLength % metaInfo.pieceLength
        if (remainder == 0L) {
            metaInfo.pieceLength
        } else {
            remainder.toInt()
        }
    } else {
        metaInfo.pieceLength
    }

    private fun isLastPiece(pieceNumber: Int) = pieceNumber == metaInfo.pieces.size - 1

}
