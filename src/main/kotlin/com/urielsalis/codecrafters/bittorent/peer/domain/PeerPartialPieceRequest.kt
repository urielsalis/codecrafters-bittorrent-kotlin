package com.urielsalis.codecrafters.bittorent.peer.domain

data class PeerPartialPieceRequest(val pieceIndex: Int, val begin: Int, val length: Int)
