package com.androidcast

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.samsung.multiscreen.Error
import com.samsung.multiscreen.Player
import com.samsung.multiscreen.Search
import com.samsung.multiscreen.Service
import com.samsung.multiscreen.VideoPlayer
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class CastState {
    data class CONNECTING(val service: Service, val connected: Boolean): CastState()
    object SEARCHING: CastState()
}

sealed class PlayerState {
    object IDLE: PlayerState()
    object READY: PlayerState()
    object BUFFERING: PlayerState()
}

class CastViewModel(private val search: Search) : ViewModel() {

    val serviceState = mutableStateOf<CastState>(CastState.SEARCHING)
    val serviceList = mutableStateListOf<Service>()

    val playerState = mutableStateOf<PlayerState>(PlayerState.IDLE)
    val videoPlayer = mutableStateOf<VideoPlayer?>(null)

    val playWhenReady = mutableStateOf(true)

    val duration = mutableStateOf(0)
    val currentTime = mutableStateOf(0)
    val progress = mutableStateOf(0f)

    init {

        search.setOnServiceFoundListener(serviceFound())
        search.setOnServiceLostListener(serviceLost())

        search.start()
    }

    private fun serviceFound() = Search.OnServiceFoundListener { service ->
        println(service.name)

        if (!serviceList.any { it.id == service.id } && !service.isStandbyService)
            serviceList.add(service)
    }

    private fun serviceLost() = Search.OnServiceLostListener { service ->
        println(service.name)

        val servById = serviceList.find { it.id == service.id }
        serviceList.remove(servById)
    }

    fun startSearch(){
        search.start()
        serviceState.value = CastState.SEARCHING
    }

    fun stopSearch(){
        search.stop()
    }

    fun connect(url: String, service: Service) {

        serviceState.value = CastState.CONNECTING(service, false)
        stopSearch()

        videoPlayer.value = service.createVideoPlayer("AndroidCast")
        videoPlayer.value?.addOnMessageListener(videoPlayerListener())
        videoPlayer.value?.playContent(Uri.parse(url), "AndroidCast", Uri.parse(""), object : com.samsung.multiscreen.Result<Boolean> {
            override fun onSuccess(p0: Boolean?) {
                //result(true, null)
                serviceState.value = CastState.CONNECTING(service, true)
            }

            override fun onError(p0: Error?) {
                startSearch()
            }

        })

    }

    fun play(){
        videoPlayer.value?.play()
    }

    fun pause(){
        videoPlayer.value?.pause()
    }

    fun rewind(){
        videoPlayer.value?.rewind()
    }

    fun forward(){
        videoPlayer.value?.forward()
    }

    fun seek(time: Int){
        videoPlayer.value?.seekTo(time, TimeUnit.MILLISECONDS)
    }

    fun stop(){
        videoPlayer.value?.stop()
        videoPlayer.value?.disconnect()

        startSearch()

        videoPlayer.value = null
    }

    fun disconnect(){
        videoPlayer.value?.disconnect()
    }

    private fun videoPlayerListener() = object : VideoPlayer.OnVideoPlayerListener {
        override fun onBufferingStart() {
            println("onBufferingStart")
            //onBuffering(true)
            playerState.value = PlayerState.BUFFERING

        }

        override fun onBufferingComplete() {
            println("onBufferingComplete")
            playerState.value = PlayerState.READY
            /*if (playWhenReady.value) {
                play()
            } else {
                pause()
            }*/
        }

        override fun onBufferingProgress(progress: Int) {
        }

        override fun onCurrentPlayTime(currentProgress: Int) {
            println("onCurrentPlayTime")
            if (currentTime.value != currentProgress) {
                currentTime.value = currentProgress

                if (currentProgress > 0) {

                    val current = currentProgress.toFloat()
                    val dur = duration.value.toFloat()

                    println((current / dur) * 100)

                    progress.value = (current / dur)
                }
            }
        }

        override fun onStreamingStarted(dur: Int) {
            println("onStreamingStarted $dur")
            duration.value = dur
            playerState.value = PlayerState.READY
        }

        override fun onStreamCompleted() {
            println("onStreamingStarted")
            videoPlayer.value?.stop()
        }

        override fun onPlayerInitialized() {
            println("onPlayerInitialized")
        }

        override fun onPlayerChange(p0: String?) {
            println("onPlayerChange")
        }

        override fun onPlay() {
            println("onPlay")
            playWhenReady.value = true
        }

        override fun onPause() {
            println("onPause")
            playWhenReady.value = false
        }

        override fun onStop() {
            println("onStop")
            playWhenReady.value = false
        }

        override fun onForward() {
        }

        override fun onRewind() {
        }

        override fun onMute() {
        }

        override fun onUnMute() {
        }

        override fun onNext() {
        }

        override fun onPrevious() {
        }

        override fun onControlStatus(p0: Int, p1: Boolean?, p2: Player.RepeatMode?) {
        }

        override fun onVolumeChange(p0: Int) {
        }

        override fun onAddToList(p0: JSONObject?) {
        }

        override fun onRemoveFromList(p0: JSONObject?) {
        }

        override fun onClearList() {
        }

        override fun onGetList(p0: JSONArray?) {
        }

        override fun onRepeat(p0: Player.RepeatMode?) {
        }

        override fun onCurrentPlaying(p0: JSONObject?, p1: String?) {
        }

        override fun onApplicationResume() {
        }

        override fun onApplicationSuspend() {
        }

        override fun onError(p0: Error?) {
        }

    }

}