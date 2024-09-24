package com.urielsalis.codecrafters.bittorent.peer

import com.urielsalis.codecrafters.bittorent.ParserException
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser
import com.urielsalis.codecrafters.bittorent.bencode.DictionaryBencodeValue
import com.urielsalis.codecrafters.bittorent.bencode.IntegerBencodeValue
import com.urielsalis.codecrafters.bittorent.peer.domain.Peer
import com.urielsalis.codecrafters.bittorent.peer.domain.PeerPartialPieceRequest
import com.urielsalis.codecrafters.bittorent.peer.domain.PeerPartialPieceResponse
import com.urielsalis.codecrafters.bittorent.skip
import com.urielsalis.codecrafters.bittorent.toArray
import com.urielsalis.codecrafters.bittorent.toInt
import java.io.DataInputStream
import java.math.BigInteger
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.SocketFactory

class PeerConnection(peer: Peer) {
    private val socket: Socket
    private val inputStream: DataInputStream
    private var status: PeerStatus = PeerStatus(interested = false, choked = true)
    private var requestsInFlight = 0
    private var supportedExtensions = byteArrayOf(0, 0, 0, 0, 0, 16, 0, 0)
    private var hasExtensions = false
    var metadataExtensionId = -1

    init {
        socket = SocketFactory.getDefault().createSocket()
        socket.connect(peer.socket)
        inputStream = DataInputStream(socket.getInputStream())
    }

    fun handshake(infoHash: ByteArray): ByteArray {
        // Send handshake
        val handshake =
            byteArrayOf(19) + "BitTorrent protocol".toByteArray() + supportedExtensions + infoHash + "00112233445566778899".toByteArray()
        socket.getOutputStream().write(handshake);

        val response = ByteArray(68)
        inputStream.readFully(response)
        val reservedBytes = response.copyOfRange(20, 28)
        hasExtensions = reservedBytes[5].toInt() and 0x10 == 0x10
        return response.skip(48)
    }

    fun initConnection() {
        waitFor(PeerMessageType.BITFIELD)
        if (hasExtensions) {
            sendExtensionHandshake()
        }
    }

    fun markInterested() {
        if (!status.interested) {
            sendMessage(PeerMessage(PeerMessageType.INTERESTED, ByteArray(0)))
        }
        if (status.choked) {
            waitFor(PeerMessageType.UNCHOKE)
        }
    }

    private fun sendExtensionHandshake() {
        val dict = BencodeParser.toBencode(
            DictionaryBencodeValue(
                mapOf(
                    "m".toByteArray() to DictionaryBencodeValue(
                        mapOf(
                            "ut_metadata".toByteArray() to IntegerBencodeValue(
                                BigInteger.valueOf(1L)
                            ), "ut_pex".toByteArray() to IntegerBencodeValue(BigInteger.valueOf(2L))
                        )
                    )
                )
            )
        )
        val message = PeerMessage(PeerMessageType.EXTENDED, byteArrayOf(0) + dict)
        println("Sending message $message")
        sendMessage(message)
        val response = waitFor(PeerMessageType.EXTENDED)
        val dictResponse =
            BencodeParser.parseNext(response.payload.skip(1)).first as DictionaryBencodeValue
        val m = dictResponse.get("m") as DictionaryBencodeValue
        val metadata = m.get("ut_metadata") as IntegerBencodeValue
        metadataExtensionId = metadata.value.toInt()

    }

    private fun waitFor(type: PeerMessageType): PeerMessage {
        var message = readMessage()
        while (message.type != type) {
            println("Got $message, expecting $type")
            message = readMessage()
        }
        return message
    }

    private fun readMessage(): PeerMessage {
        val bytes = ByteArray(4)
        inputStream.readFully(bytes)
        val length = bytes.toInt()
        if (length == 0) {
            return PeerMessage(PeerMessageType.KEEP_ALIVE, ByteArray(0))
        }
        val messageType = inputStream.readByte().toInt()
        val type = PeerMessageType.valueOf(messageType)
            ?: throw ParserException("Unknown message type received $messageType")
        val payload = if (length > 1) {
            val ret = ByteArray(length - 1)
            inputStream.readFully(ret)
            ret
        } else {
            ByteArray(0)
        }
        when (type) {
            PeerMessageType.CHOKE -> status = status.markAsChoked()
            PeerMessageType.UNCHOKE -> status.markAsUnchoked()
            PeerMessageType.INTERESTED -> status.markAsInterested()
            PeerMessageType.NOT_INTERESTED -> status.markAsNotInterested()
            else -> null // Do nothing
        }
        return PeerMessage(type, payload)
    }

    private fun sendMessage(message: PeerMessage) {
        val length = 4 + 1 + message.payload.size
        val buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN).putInt(length)
            .put(message.type.protocolType.toByte()) // Request
            .put(message.payload)
        val out = socket.getOutputStream()
        out.write(buffer.toArray())
        out.flush()
    }

    fun download(
        getNextItem: () -> PeerPartialPieceRequest?,
        saveResponse: (PeerPartialPieceResponse) -> Boolean
    ) {
        var item = getNextItem()
        while (item != null) {
            while (requestsInFlight < 5 && item != null) {
                sendRequest(item)
                item = getNextItem()
            }
            if (requestsInFlight > 0) {
                readPiece(saveResponse)
            }
        }
        while (requestsInFlight != 0) {
            readPiece(saveResponse)
        }
    }

    private fun sendRequest(item: PeerPartialPieceRequest) {
        val buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN).putInt(item.pieceIndex)
            .putInt(item.begin).putInt(item.length)
        sendMessage(PeerMessage(PeerMessageType.REQUEST, buf.toArray()))
        requestsInFlight++
    }

    private fun readPiece(saveResponse: (PeerPartialPieceResponse) -> Boolean) {
        val piece = waitFor(PeerMessageType.PIECE)
        val response = PeerPartialPieceResponse(
            piece.payload.copyOfRange(0, 4).toInt(),
            piece.payload.copyOfRange(4, 8).toInt(),
            piece.payload.copyOfRange(8, piece.payload.size)
        )
        saveResponse(response)
        requestsInFlight--
    }
}

private enum class PeerMessageType(val protocolType: Int) {
    KEEP_ALIVE(-1), CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3), HAVE(4), BITFIELD(5), REQUEST(
        6
    ),
    PIECE(7), CANCEL(8), EXTENDED(20);

    companion object {
        fun valueOf(value: Int) = entries.firstOrNull { it.protocolType == value }
    }
}

private data class PeerMessage(val type: PeerMessageType, val payload: ByteArray)

private data class PeerStatus(val interested: Boolean, val choked: Boolean) {
    fun markAsInterested() = PeerStatus(true, choked)
    fun markAsNotInterested() = PeerStatus(false, choked)
    fun markAsChoked() = PeerStatus(interested, true)
    fun markAsUnchoked() = PeerStatus(interested, false)
}