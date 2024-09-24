package com.urielsalis.codecrafters.bittorent.peer

import com.urielsalis.codecrafters.bittorent.ParserException
import com.urielsalis.codecrafters.bittorent.asType
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser
import com.urielsalis.codecrafters.bittorent.bencode.DictionaryBencodeValue
import com.urielsalis.codecrafters.bittorent.bencode.StringBencodeValue
import com.urielsalis.codecrafters.bittorent.magnet.Magnet
import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfo
import com.urielsalis.codecrafters.bittorent.peer.domain.Peer
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.net.URLCodec
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

object TrackerManager {
    fun getPeers(metaInfo: MetaInfo): List<Peer> {
        return getPeers(metaInfo.announce, encodeBytes(metaInfo.infoHash), metaInfo.fileLength)
    }

    fun getPeers(magnet: Magnet): List<Peer> {
        assert(magnet.trackerUrl != null, { "Tracker URL is required" })
        val encodedHash = encodeBytes(Hex.decodeHex(magnet.infoHash))
        // We don't know the file length yet, so send a value above 0
        return getPeers(magnet.trackerUrl!!, encodedHash, 1024)
    }

    private fun encodeBytes(bytes: ByteArray): String {
        return String(URLCodec.encodeUrl(BitSet(256), bytes))
    }

    private fun getPeers(
        announceUrl: String, encodedHash: String, fileLength: Long
    ): List<Peer> {
        val url =
            URL("$announceUrl?info_hash=$encodedHash&peer_id=00112233445566778899&port=6881&uploaded=0&downloaded=0&left=$fileLength&compact=1")
        val con = url.openConnection() as HttpURLConnection
        con.setRequestMethod("GET")
        val bencoded = con.inputStream.readBytes()
        val dict = BencodeParser.parseNext(bencoded).first.asType<DictionaryBencodeValue>()
        val peers = dict["peers"]?.asType<StringBencodeValue>()
            ?: throw ParserException("'peers' not present in tracker response")
        return peers.value.toList().chunked(6) { Peer(it.toByteArray()) }
    }
}