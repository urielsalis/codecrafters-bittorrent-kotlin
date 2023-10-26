package com.urielsalis.codecrafters.bittorent.peer.domain

import com.urielsalis.codecrafters.bittorent.skip
import com.urielsalis.codecrafters.bittorent.toShort
import java.net.InetAddress
import java.net.InetSocketAddress

data class Peer(val socket: InetSocketAddress) {
    constructor(inetAddress: InetAddress, port: Int) : this(InetSocketAddress(inetAddress, port))
    constructor(byteArray: ByteArray) : this(
        InetAddress.getByAddress(byteArray.copyOf(4)),
        byteArray.skip(4).toShort().toInt() and 0xffff
    )

    constructor(peer: String) : this(
        InetAddress.getByName(peer.substringBefore(':')), peer.substringAfter(':').toInt()
    )
}

