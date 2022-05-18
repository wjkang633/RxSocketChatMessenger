package com.example.rxsockettest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.rxsockettest.databinding.ActivityMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val subscriptions by lazy {
        CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        connect()
    }

    override fun onResume() {
        super.onResume()
        connect()
    }

    private fun initViews() {
        binding.sendButton.setOnClickListener {
            val msg = binding.inputTextField.text.toString()
            sendMessage(msg)
        }
    }

    override fun onStop() {
        super.onStop()
        disconnect()
    }

    private fun connect() {
        //connect to server and register server response listener
        var isConnected = false

        SocketManager.isConnected()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ status ->
                isConnected = status
            }, {
                isConnected = false
            })

        if (!isConnected) {
            val disposable = SocketManager.connectAndReceive()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ str ->
                    Log.e("wj", "receive message: $str")
                }, { throwable ->
                    Log.e("wj", "${throwable.message}")
                }, {
                    Log.e("wj", "connect complete!")
                })
            subscriptions.add(disposable)
        }
    }

    private fun disconnect() {
        //disconnect from server
        val disposable = SocketManager.disconnect()
            .subscribe({
                Log.e("wj", "disconnect complete!")
            }, { throwable ->
                Log.e("wj", "${throwable.message}")
            })
        subscriptions.add(disposable)
    }

    private fun sendMessage(msg: String) {
        val disposable = SocketManager.sendMessage(msg)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ str ->
                Log.e("wj", "send message: $str")
            }, { throwable ->
                Log.e("wj", "Failed to send message: ${throwable.message}")
            })
        subscriptions.add(disposable)
    }
}