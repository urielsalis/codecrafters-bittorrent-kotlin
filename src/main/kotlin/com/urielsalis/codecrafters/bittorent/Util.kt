package com.urielsalis.codecrafters.bittorent

fun Byte.toBigInteger() = this.toInt().toChar().toString().toBigInteger()
fun ByteArray.skip(num: Int) = this.copyOfRange(num, this.size)