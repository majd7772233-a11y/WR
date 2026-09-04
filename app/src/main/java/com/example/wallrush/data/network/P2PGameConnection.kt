package com.example.wallrush.data.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

sealed class P2PConnectionType {
    data class WifiHotspotHost(val port: Int = 8888) : P2PConnectionType()
    data class WifiHotspotClient(val hostIp: String, val port: Int = 8888) : P2PConnectionType()
    data class BluetoothHost(val uuid: UUID = WALLRUSH_BT_UUID) : P2PConnectionType()
    data class BluetoothClient(val deviceAddress: String, val uuid: UUID = WALLRUSH_BT_UUID) : P2PConnectionType()

    companion object {
        val WALLRUSH_BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

interface P2PGameListener {
    fun onConnected(isHost: Boolean, opponentName: String, opponentAvatar: Int, wallsCount: Int, timeLimit: Int)
    fun onMoveReceived(targetX: Int, targetY: Int)
    fun onWallReceived(x: Int, y: Int, isHorizontal: Boolean)
    fun onEmoteReceived(emoji: String)
    fun onOpponentResigned()
    fun onOpponentRequestedRematch()
    fun onConnectionLost(reason: String)
    fun onError(errorMessage: String)
}

class P2PGameConnection(
    private val context: Context,
    private val listener: P2PGameListener
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var tcpSocket: Socket? = null

    private var btServerSocket: BluetoothServerSocket? = null
    private var btSocket: BluetoothSocket? = null

    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    @Volatile
    private var isClosingExpected = false

    private var heartbeatJob: Job? = null
    private var readJob: Job? = null

    /**
     * Starts listening as a Wi-Fi / Hotspot TCP host.
     */
    fun startWifiHost(localPlayerName: String, localAvatar: Int, wallsCount: Int, timeLimit: Int, port: Int = 8888) {
        disconnect()
        isClosingExpected = false
        scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                val client = serverSocket?.accept() ?: return@launch
                tcpSocket = client
                setupStreamsAndListen(
                    isHost = true,
                    localName = localPlayerName,
                    localAvatar = localAvatar,
                    wallsCount = wallsCount,
                    timeLimit = timeLimit
                )
            } catch (e: Exception) {
                if (!isClosingExpected) {
                    withContext(Dispatchers.Main) {
                        listener.onError("خطأ في تشغيل خادم المضيف: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    /**
     * Connects to a Wi-Fi / Hotspot host by IP and port.
     */
    fun connectToWifiHost(hostIp: String, localPlayerName: String, localAvatar: Int, port: Int = 8888) {
        disconnect()
        isClosingExpected = false
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostIp, port), 6000)
                tcpSocket = socket
                setupStreamsAndListen(
                    isHost = false,
                    localName = localPlayerName,
                    localAvatar = localAvatar,
                    wallsCount = 10,
                    timeLimit = 300
                )
            } catch (e: Exception) {
                if (!isClosingExpected) {
                    withContext(Dispatchers.Main) {
                        listener.onError("تعذر الاتصال بالمضيف على $hostIp: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    /**
     * Starts listening as a Bluetooth Host.
     */
    @SuppressLint("MissingPermission")
    fun startBluetoothHost(localPlayerName: String, localAvatar: Int, wallsCount: Int, timeLimit: Int) {
        disconnect()
        isClosingExpected = false
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            listener.onError("البلوتوث غير مفعّل على هذا الجهاز!")
            return
        }

        scope.launch {
            try {
                btServerSocket = adapter.listenUsingRfcommWithServiceRecord("WallRush", P2PConnectionType.WALLRUSH_BT_UUID)
                val socket = btServerSocket?.accept() ?: return@launch
                btSocket = socket
                btServerSocket?.close()
                btServerSocket = null

                setupStreamsAndListen(
                    isHost = true,
                    localName = localPlayerName,
                    localAvatar = localAvatar,
                    wallsCount = wallsCount,
                    timeLimit = timeLimit
                )
            } catch (e: Exception) {
                if (!isClosingExpected) {
                    withContext(Dispatchers.Main) {
                        listener.onError("خطأ في استقبال اتصال البلوتوث: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    /**
     * Connects to a paired Bluetooth device.
     */
    @SuppressLint("MissingPermission")
    fun connectToBluetoothDevice(deviceAddress: String, localPlayerName: String, localAvatar: Int) {
        disconnect()
        isClosingExpected = false
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            listener.onError("البلوتوث غير مفعّل على هذا الجهاز!")
            return
        }

        scope.launch {
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                adapter.cancelDiscovery()
                val socket = device.createRfcommSocketToServiceRecord(P2PConnectionType.WALLRUSH_BT_UUID)
                socket.connect()
                btSocket = socket

                setupStreamsAndListen(
                    isHost = false,
                    localName = localPlayerName,
                    localAvatar = localAvatar,
                    wallsCount = 10,
                    timeLimit = 300
                )
            } catch (e: Exception) {
                if (!isClosingExpected) {
                    withContext(Dispatchers.Main) {
                        listener.onError("فشل الاتصال بجهاز البلوتوث: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private suspend fun setupStreamsAndListen(
        isHost: Boolean,
        localName: String,
        localAvatar: Int,
        wallsCount: Int,
        timeLimit: Int
    ) {
        try {
            val inStream = tcpSocket?.getInputStream() ?: btSocket?.inputStream
            val outStream = tcpSocket?.getOutputStream() ?: btSocket?.outputStream

            if (inStream == null || outStream == null) {
                throw IllegalStateException("Streams cannot be opened")
            }

            reader = BufferedReader(InputStreamReader(inStream, Charsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(outStream, Charsets.UTF_8))

            // Handshake flow
            if (isHost) {
                // Host sends match rules & identity
                sendMessageDirect("HANDSHAKE:$localName:$localAvatar:$wallsCount:$timeLimit")
            }

            // Start listening loop
            startReadingLoop(isHost, localName, localAvatar)
            startHeartbeat()
        } catch (e: Exception) {
            handleDisconnection("فشل تجهيز قنوات الاتصال: ${e.localizedMessage}")
        }
    }

    private fun startReadingLoop(isHost: Boolean, localName: String, localAvatar: Int) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                while (isActive) {
                    val line = reader?.readLine() ?: break // EOF means connection broken
                    processIncomingMessage(line, isHost, localName, localAvatar)
                }
                if (!isClosingExpected) {
                    handleDisconnection("انقطع الاتصال بالطرف الآخر (انقطاع القناة).")
                }
            } catch (e: Exception) {
                if (!isClosingExpected) {
                    handleDisconnection("تم فقدان الاتصال: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun processIncomingMessage(
        line: String,
        isHost: Boolean,
        localName: String,
        localAvatar: Int
    ) {
        val parts = line.split(":")
        if (parts.isEmpty()) return

        when (parts[0]) {
            "HANDSHAKE" -> {
                // Received by Client from Host
                val oppName = parts.getOrNull(1) ?: "Player 1"
                val oppAvatar = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val walls = parts.getOrNull(3)?.toIntOrNull() ?: 10
                val time = parts.getOrNull(4)?.toIntOrNull() ?: 300

                // Client acknowledges with identity
                sendMessageDirect("HANDSHAKE_ACK:$localName:$localAvatar")
                isConnected = true
                withContext(Dispatchers.Main) {
                    listener.onConnected(
                        isHost = false,
                        opponentName = oppName,
                        opponentAvatar = oppAvatar,
                        wallsCount = walls,
                        timeLimit = time
                    )
                }
            }
            "HANDSHAKE_ACK" -> {
                // Received by Host from Client
                val oppName = parts.getOrNull(1) ?: "Player 2"
                val oppAvatar = parts.getOrNull(2)?.toIntOrNull() ?: 1
                isConnected = true
                withContext(Dispatchers.Main) {
                    listener.onConnected(
                        isHost = true,
                        opponentName = oppName,
                        opponentAvatar = oppAvatar,
                        wallsCount = 10,
                        timeLimit = 300
                    )
                }
            }
            "MOVE" -> {
                val x = parts.getOrNull(1)?.toIntOrNull()
                val y = parts.getOrNull(2)?.toIntOrNull()
                if (x != null && y != null) {
                    withContext(Dispatchers.Main) {
                        listener.onMoveReceived(x, y)
                    }
                }
            }
            "WALL" -> {
                val x = parts.getOrNull(1)?.toIntOrNull()
                val y = parts.getOrNull(2)?.toIntOrNull()
                val isHoriz = parts.getOrNull(3) == "H"
                if (x != null && y != null) {
                    withContext(Dispatchers.Main) {
                        listener.onWallReceived(x, y, isHoriz)
                    }
                }
            }
            "EMOTE" -> {
                val emoji = parts.getOrNull(1) ?: "👋"
                withContext(Dispatchers.Main) {
                    listener.onEmoteReceived(emoji)
                }
            }
            "RESIGN" -> {
                withContext(Dispatchers.Main) {
                    listener.onOpponentResigned()
                }
            }
            "REMATCH" -> {
                withContext(Dispatchers.Main) {
                    listener.onOpponentRequestedRematch()
                }
            }
            "PING" -> {
                sendMessageDirect("PONG")
            }
            "PONG" -> {
                // Heartbeat alive
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    sendMessageDirect("PING")
                } catch (e: Exception) {
                    handleDisconnection("انقطع الاتصال (فشل إرسال إشارة النبض).")
                    break
                }
            }
        }
    }

    fun sendMove(x: Int, y: Int) {
        scope.launch {
            try {
                sendMessageDirect("MOVE:$x:$y")
            } catch (e: Exception) {
                handleDisconnection("فشل إرسال الحركة.")
            }
        }
    }

    fun sendWall(x: Int, y: Int, isHorizontal: Boolean) {
        scope.launch {
            try {
                val dir = if (isHorizontal) "H" else "V"
                sendMessageDirect("WALL:$x:$y:$dir")
            } catch (e: Exception) {
                handleDisconnection("فشل إرسال الجدار.")
            }
        }
    }

    fun sendEmote(emoji: String) {
        scope.launch {
            try {
                sendMessageDirect("EMOTE:$emoji")
            } catch (e: Exception) {
                // Silently ignore emote error or disconnect
            }
        }
    }

    fun sendResign() {
        scope.launch {
            try {
                sendMessageDirect("RESIGN")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun sendRematch() {
        scope.launch {
            try {
                sendMessageDirect("REMATCH")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun sendMessageDirect(msg: String) {
        val w = writer ?: throw IllegalStateException("Writer not open")
        synchronized(w) {
            w.write(msg)
            w.newLine()
            w.flush()
        }
    }

    private fun handleDisconnection(reason: String) {
        if (!isConnected && isClosingExpected) return
        isConnected = false
        disconnect()
        scope.launch(Dispatchers.Main) {
            listener.onConnectionLost(reason)
        }
    }

    fun disconnect() {
        isClosingExpected = true
        isConnected = false
        heartbeatJob?.cancel()
        readJob?.cancel()

        try {
            reader?.close()
        } catch (e: Exception) { }
        try {
            writer?.close()
        } catch (e: Exception) { }
        try {
            tcpSocket?.close()
        } catch (e: Exception) { }
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        try {
            btSocket?.close()
        } catch (e: Exception) { }
        try {
            btServerSocket?.close()
        } catch (e: Exception) { }

        reader = null
        writer = null
        tcpSocket = null
        serverSocket = null
        btSocket = null
        btServerSocket = null
    }
}
