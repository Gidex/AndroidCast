package com.androidcast

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.samsung.multiscreen.Service
import com.androidcast.ui.theme.AndroidCastTheme

class MainActivity : ComponentActivity() {

    lateinit var castViewModel: CastViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = Service.search(this)
        castViewModel = CastViewModel(service)

        setContent {
            AndroidCastTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main(castViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (castViewModel.serviceState.value !is CastState.CONNECTING)
            castViewModel.startSearch()
    }

    override fun onStart() {
        super.onStart()

        if (castViewModel.serviceState.value !is CastState.CONNECTING)
            castViewModel.startSearch()
    }

    override fun onStop() {
        super.onStop()

        castViewModel.stopSearch()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(castViewModel: CastViewModel) {
    val context = LocalContext.current

    val serviceList = castViewModel.serviceList
    val serviceState by castViewModel.serviceState

    val playerState by castViewModel.playerState

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.padding(16.dp)) {

            val focusRequester = remember { FocusRequester() }
            var textUrl by remember { mutableStateOf("") }

            OutlinedTextField(modifier = Modifier.fillMaxWidth()
                .focusRequester(focusRequester),
                enabled = serviceState is CastState.SEARCHING,
                value = textUrl,
                placeholder = { Text(text = "Url to play") },
                onValueChange = {
                    textUrl = it
                },
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                textUrl = stringFromClipBoard(context)
            }

            if (serviceState is CastState.CONNECTING) {

                val service = (serviceState as CastState.CONNECTING).service
                val connected = (serviceState as CastState.CONNECTING).connected

                ListItem(
                    headlineContent = {
                        Text(text = service.name ?: "Unknown")
                    },
                    overlineContent = {
                        Text(text = service.type + " - " + service.version)
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                    },
                    trailingContent = {
                        if (!connected) {
                            CircularProgressIndicator(modifier = Modifier
                                .align(Alignment.CenterHorizontally))
                        }
                    }
                )
            } else {

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .semantics(mergeDescendants = true) {}
                        .padding(10.dp)
                )

                serviceList.forEach {
                    ListItem(modifier = Modifier.clickable {

                        if (textUrl.isNotEmpty()) {
                            castViewModel.connect(textUrl, it)
                        }

                    },
                        headlineContent = {
                            Text(text = it.name)
                        },
                        overlineContent = {
                            Text(text = it.type + " - " + it.version)
                        },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                        }
                    )
                }
            }

        }

        if ((serviceState as? CastState.CONNECTING)?.connected == true) {
        //if (true) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

                val playWhenReady by castViewModel.playWhenReady

                var currentTime by castViewModel.currentTime
                val duration by castViewModel.duration

                val playerReady = playerState == PlayerState.READY

                val startInteractionSource = remember { MutableInteractionSource() }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    
                    Text(modifier = Modifier.then(if (!playerReady) Modifier.alpha(.5f) else Modifier),
                        text = currentTime.toTimeString(),
                        style = MaterialTheme.typography.bodySmall )

                    if (!playerReady) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(6.dp)
                        )
                    } else {
                        Slider(modifier = Modifier
                            .height(6.dp)
                            .weight(1f)
                            .padding(6.dp),
                            value = currentTime.toFloat(),
                            onValueChange = { currentTime = it.toInt() },
                            valueRange = 0f..duration.toFloat(),
                            onValueChangeFinished = {
                                castViewModel.seek(currentTime)
                            },
                            thumb = {
                                SliderDefaults.Thumb(interactionSource = startInteractionSource,
                                    thumbSize = DpSize(16.dp, 20.dp))
                            },
                            track = {
                                SliderDefaults.Track(colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.secondary),
                                    sliderPositions = it
                                )
                            }
                        )
                    }

                    Text(modifier = Modifier.then(if (!playerReady) Modifier.alpha(.5f) else Modifier),
                        text = duration.toTimeString(),
                        style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(enabled = playerReady, onClick = {
                        castViewModel.rewind()
                    }) {
                        Icon(Icons.Filled.FastRewind, contentDescription = "Localized description")
                    }

                    IconButton(enabled = playerReady, onClick = {
                        if (playWhenReady) {
                            castViewModel.pause()
                        } else {
                            castViewModel.play()
                        }
                    }) {
                        Icon(if (playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Localized description")
                    }

                    IconButton(enabled = playerReady, onClick = {
                        castViewModel.forward()
                    }) {
                        Icon(Icons.Filled.FastForward, contentDescription = "Localized description")
                    }

                    IconButton(onClick = {
                        castViewModel.stop()
                        castViewModel.startSearch()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Localized description")
                    }
                }
            }
        }
    }
}

fun Int.toTimeString(): String {
    val timeInSeconds = this / 1000

    val hour = timeInSeconds / (60 * 60) % 24
    val minutes = (timeInSeconds / 60) % 60
    val seconds = timeInSeconds % 60

    return "%02d:%02d:%02d".format(hour, minutes, seconds)
}

fun stringFromClipBoard(context: Context): String {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text

    return if (clipText.isNullOrEmpty()) "" else clipText.toString()
}

@Preview(showBackground = true)
@Composable
fun AlgoPreview() {
    AndroidCastTheme() {
        Main(castViewModel = CastViewModel(Service.search(LocalContext.current)))
    }
}