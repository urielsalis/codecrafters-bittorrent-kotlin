package com.urielsalis.codecrafters.bittorent.peer

import com.urielsalis.codecrafters.bittorent.skip
import com.urielsalis.codecrafters.bittorent.toShort
import java.net.InetAddress
import java.net.InetSocketAddress

data class Peer(val socket: InetSocketAddress) {
    constructor(byteArray: ByteArray) : this(
        InetSocketAddress(
            InetAddress.getByAddress(byteArray.copyOf(4)),
            byteArray.skip(4).toShort().toInt() and 0xffff
        )
    )
}

