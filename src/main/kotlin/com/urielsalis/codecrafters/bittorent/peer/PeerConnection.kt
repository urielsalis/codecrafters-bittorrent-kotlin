package com.urielsalis.codecrafters.bittorent.peer

import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfo
import com.urielsalis.codecrafters.bittorent.skip
import java.io.DataInputStream
import java.net.Socket
import javax.net.SocketFactory

class PeerConnection(peer: Peer) {
    private val socket: Socket
    private val inputStream: DataInputStream

    init {
        socket = SocketFactory.getDefault().createSocket()
        socket.connect(peer.socket)
        inputStream = DataInputStream(socket.getInputStream())
    }

    fun handshake(metaInfo: MetaInfo): ByteArray {
        // Send handshake
        val handshake =
            byteArrayOf(19) + "BitTorrent protocol".toByteArray() + ByteArray(8) + metaInfo.infoHash + "00112233445566778899".toByteArray()
        socket.getOutputStream().write(handshake);

        val response = ByteArray(68)
        inputStream.readFully(response)
        return response.skip(48)
    }
}