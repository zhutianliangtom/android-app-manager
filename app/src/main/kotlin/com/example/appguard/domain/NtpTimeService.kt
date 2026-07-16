package com.example.appguard.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object NtpTimeService {

    private const val NTP_SERVER = "ntp.aliyun.com"
    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val NTP_OFFSET_SECONDS = 2208988800L
    private const val TIMEOUT_MS = 5000

    suspend fun getNtpTime(): Long? = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS

            val address = InetAddress.getByName(NTP_SERVER)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            buffer[0] = 0x1B.toByte()

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            socket.close()

            val seconds = readTimestamp(buffer, 40)
            val fraction = readTimestamp(buffer, 44)
            val millis = (seconds - NTP_OFFSET_SECONDS) * 1000 + (fraction * 1000L) / 0x100000000L
            millis
        } catch (e: Exception) {
            null
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0..3) {
            value = (value shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return value
    }
}