package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
<<<<<<< HEAD
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.*
=======
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
>>>>>>> 8f4d66b8d38a45ddbac798682b2872d365be9250

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
<<<<<<< HEAD
                    topBar = { AppBar() }
                ) { innerPadding ->
                    VideoPlayerScreen(modifier = Modifier.padding(innerPadding))
=======
                    topBar = { AppBar() } // 添加顶部栏
                ) { innerPadding ->
                    // 去掉 Greeting 组件中的文本内容
>>>>>>> 8f4d66b8d38a45ddbac798682b2872d365be9250
                }
            }
        }
    }
}

<<<<<<< HEAD
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    var startTime by remember { mutableStateOf(0L) }
    var endTime by remember { mutableStateOf(1000L) }
    var frameRate by remember { mutableStateOf(30) }
    var resolution by remember { mutableStateOf(500) }
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "编辑页",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        navigationIcon = {
            TextButton(onClick = { /* TODO: 处理取消逻辑 */ }) {
                Text(text = "取消", style = MaterialTheme.typography.labelLarge)
            }
        },
        actions = {
            TextButton(onClick = {
                if (!isGenerating) {
                    isGenerating = true
                    progress = 0f

                    // 启动协程生成 GIF
                    coroutineScope.launch {
                        generateGif(
                            startTime = startTime,
                            endTime = endTime,
                            frameRate = frameRate,
                            resolution = resolution,
                            onProgressUpdate = { newProgress ->
                                progress = newProgress
                            }
                        )
                        isGenerating = false // 生成完成
                    }
                }
            }) {
                Text(text = "生成 GIF", style = MaterialTheme.typography.labelLarge)
            }
        }
    )

    // 显示进度对话框
    if (isGenerating || progress == 1f) {
        ProgressDialog(progress = progress) {
            // 点击关闭按钮重置状态
            isGenerating = false
            progress = 0f
        }
    }
}

@Composable
fun ProgressDialog(progress: Float, onDismiss: () -> Unit) {
    // 显示进度的对话框
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (progress < 1f) {
                    Text(text = "GIF 正在生成中...")
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(progress = progress)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${(progress * 100).toInt()}%")
                } else {
                    Text(text = "GIF 生成成功！")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onDismiss() }) {
                        Text(text = "关闭")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    var startTime by remember { mutableStateOf(0L) }
    var endTime by remember { mutableStateOf(1000L) }
    var frameRate by remember { mutableStateOf(30) }
    var resolution by remember { mutableStateOf(500) }
    var duration by remember { mutableStateOf(1000L) }

    DisposableEffect(Unit) {
        val mediaItem = MediaItem.fromUri("android.resource://com.example.myapplication/raw/ab")
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.addListener(object : com.google.android.exoplayer2.Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                duration = exoPlayer.duration
                endTime = duration
            }
        })
        onDispose {
            exoPlayer.release()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "起始时间: $startTime 毫秒")
        Slider(
            value = startTime.toFloat(),
            onValueChange = { startTime = it.toLong() },
            valueRange = 0f..duration.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "结束时间: $endTime 毫秒")
        Slider(
            value = endTime.toFloat(),
            onValueChange = { endTime = it.toLong() },
            valueRange = 0f..duration.toFloat()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "帧率: $frameRate FPS")
        Slider(
            value = frameRate.toFloat(),
            onValueChange = { frameRate = it.toInt() },
            valueRange = 1f..60f
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "分辨率: ${resolution}x$resolution")
        Slider(
            value = resolution.toFloat(),
            onValueChange = { resolution = it.toInt() },
            valueRange = 100f..1000f
        )
    }
}

suspend fun generateGif(
    startTime: Long,
    endTime: Long,
    frameRate: Int,
    resolution: Int,
    onProgressUpdate: (Float) -> Unit
) {
    var progress = 0f
    while (progress < 1f) {
        delay(100)  // 模拟GIF生成的过程
        progress += 0.05f
        onProgressUpdate(progress)
    }
=======
@OptIn(ExperimentalMaterial3Api::class) // 明确标记使用实验性 API
@Composable
fun AppBar() {
    TopAppBar(
        title = {
            // 使用 Box 来让标题居中
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "编辑页",
                    modifier = Modifier.align(Alignment.Center) // 将标题居中
                )
            }
        },
        navigationIcon = { // 左上角按钮
            TextButton(onClick = { /* TODO: 处理取消按钮点击事件 */ }) {
                Text(text = "取消")
            }
        },
        actions = { // 右上角按钮
            TextButton(onClick = { /* TODO: 处理确定按钮点击事件 */ }) {
                Text(text = "确定")
            }
        }
    )
>>>>>>> 8f4d66b8d38a45ddbac798682b2872d365be9250
}

@Preview(showBackground = true)
@Composable
fun AppBarPreview() {
    MyApplicationTheme {
<<<<<<< HEAD
        AppBar()
    }
}
=======
        AppBar() // 仅显示顶部应用栏
    }
}
>>>>>>> 8f4d66b8d38a45ddbac798682b2872d365be9250
