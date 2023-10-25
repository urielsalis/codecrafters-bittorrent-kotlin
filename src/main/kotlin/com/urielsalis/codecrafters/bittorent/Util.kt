package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeValue
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Byte.toBigInteger() = this.toInt().toChar().toString().toBigInteger()
fun ByteArray.skip(num: Int) = this.copyOfRange(num, this.size)
fun ByteArray.toShort(order: ByteOrder = ByteOrder.BIG_ENDIAN): Short =
    ByteBuffer.wrap(this).order(order).getShort()

fun ByteArray.toInt(order: ByteOrder = ByteOrder.BIG_ENDIAN): Int =
    ByteBuffer.wrap(this).order(order).getInt()

inline fun <reified T : BencodeValue> BencodeValue.asType(): T = if (this !is T) {
    throw MismatchedTypeException(T::class, this)
} else {
    this
}
