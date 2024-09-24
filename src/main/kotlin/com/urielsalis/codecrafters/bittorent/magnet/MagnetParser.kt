package com.urielsalis.codecrafters.bittorent.magnet

import java.net.URLDecoder
import java.nio.charset.Charset

object MagnetParser {
    fun parse(url: String): Magnet {
        assert(url.startsWith("magnet:?xt=urn:btih:"))
        val elements = url.split("&")
        val infoHash = elements[0].substringAfterLast(":")
        val name = elements.find { it.startsWith("dn=") }?.substringAfter("dn=")
        val trackerUrl = URLDecoder.decode(
            elements.find { it.startsWith("tr=") }?.substringAfter("tr="),
            Charset.defaultCharset().name()
        )
        return Magnet(infoHash, name, trackerUrl)
    }
}