package com.example.rxsockettest

import android.util.Log
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import okhttp3.*
import okio.ByteString

object SocketManager {

    private lateinit var webSocket: WebSocket

    private const val CLOSURE_STATUS = 1000

    fun connectAndReceive(): Flowable<String> {
        return Flowable.create({ emitter ->
            val client = OkHttpClient()

            val request: Request = Request.Builder()
                .url("wss://demo.piesocket.com/v3/channel_1?api_key=VCXCEuvhGcBDP7XhiJJUDvR1e1D3eiVjgZ9VRiaV&notify_self")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)

//                    Log.d("wj", "onOpen")
                    emitter.onNext(response.message)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)

//                    Log.d("wj", "onMessage")
                    emitter.onNext(bytes.utf8())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)

                    emitter.onNext(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)

//                    Log.d("wj", "onClosing")
                    emitter.onNext(reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)

//                    Log.d("wj", "onClosed")
                    emitter.onNext(reason)
                }
            })
            client.dispatcher.executorService.shutdown()
        }, BackpressureStrategy.BUFFER)
    }

    fun disconnect(): Completable {
        return Completable.create { emitter ->
            try {
                webSocket.close(CLOSURE_STATUS, "disconnect from server")
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e.cause)
            }
        }
    }

    fun sendMessage(msg: String): Flowable<String> {
        return Flowable.create({ emitter ->
            webSocket.send(msg)
            emitter.onNext(msg)
        }, BackpressureStrategy.BUFFER)
    }

    fun isConnected(): Single<Boolean> {
        return Single.create { emitter ->
            if (webSocket.send("ping").not()) emitter.onSuccess(true)
            else emitter.onSuccess(false)
        }
    }
}