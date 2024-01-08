package eliphas1810.freesearchablemusicplayer


import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

//Public Domain

//アクティビティからサービスへの送信情報を受け取る物

class MusicPlayerActivityHandler(
    var musicPlayerService: MusicPlayerService?
) : Handler(Looper.getMainLooper()) {

    override fun handleMessage(message: Message) {
        try {

            if (message.what == MusicPlayerService.REQUEST_MUSIC_INFO_MESSAGE) {
                musicPlayerService?.updateMusicInfo()
                return
            }

            val bundle = message.data

            //アクティビティからサービスへの送信情報から、音楽ファイルの一覧の情報を受け取ります。

            musicPlayerService?.musicInfoList = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) { //アンドロイド13(ティラミス)以上の場合
                bundle.getParcelableArrayList(MusicPlayerService.MUSIC_INFO_LIST_KEY, MusicInfo::class.java)
            } else {
                bundle.getParcelableArrayList(MusicPlayerService.MUSIC_INFO_LIST_KEY)

            }

            //アクティビティからサービスへの送信情報から、選択中のゼロから始まる音楽ファイルの番号を受け取ります。
            musicPlayerService?.musicInfoIndex = bundle.getInt(MusicPlayerService.MUSIC_INFO_INDEX_KEY)

            //アクティビティからサービスへの送信情報から、ループ再生するか否かを受け取ります。
            musicPlayerService?.loop = bundle.getBoolean(MusicPlayerService.LOOP_MUSIC_KEY)

            //アクティビティからサービスへの送信情報から、ランダム再生するか否かを受け取ります。
            musicPlayerService?.random = bundle.getBoolean(MusicPlayerService.RANDOM_MUSIC_KEY)

            //アクティビティからサービスへの送信情報から、音楽の再生開始時間を受け取ります。
            musicPlayerService?.currentMusicDuration = bundle.getInt(MusicPlayerService.CURRENT_MUSIC_DURATION_KEY)

            if (message.what == MusicPlayerService.START_MUSIC_MESSAGE) {
                musicPlayerService?.start()
                return
            }

            if (message.what == MusicPlayerService.PAUSE_MUSIC_MESSAGE) {
                musicPlayerService?.pause()
                return
            }

            if (message.what == MusicPlayerService.STOP_MUSIC_MESSAGE) {
                musicPlayerService?.stop()
                return
            }

            if (message.what == MusicPlayerService.PREVIOUS_MUSIC_MESSAGE) {
                musicPlayerService?.previous()
                return
            }

            if (message.what == MusicPlayerService.NEXT_MUSIC_MESSAGE) {
                musicPlayerService?.next()
                return
            }

            if (message.what == MusicPlayerService.SEEK_MUSIC_MESSAGE) {
                musicPlayerService?.seek()
                return
            }

        } catch (exception: Exception) {
            Toast.makeText(musicPlayerService?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.handleMessage(message)
        }
    }
}


//事故で意図せず有線イヤホンが抜けた場合などに音楽の再生を一時停止する物
//
//有線イヤホンや無線イヤホンなどからスマホやタブレットの内蔵スピーカーへ音声出力先が戻る通知を受け取る物
//
//アンドロイド システムからの通知を受け取ります。
//
private class AudioBecomingNoisyBroadcastReceiver(
    var musicPlayerService: MusicPlayerService?
) : BroadcastReceiver() {

    //事故で意図せず有線イヤホンが抜けた場合などの処理
    //
    //有線イヤホンや無線イヤホンなどからスマホやタブレットの内蔵スピーカーへ音声出力先が戻る通知を受け取った場合の処理
    //
    //アンドロイド システムからの通知を受け取った場合の処理
    //
    override fun onReceive(context: Context?, intent: Intent?) {
        try {

            //音楽の再生を一時停止
            musicPlayerService?.pause()

        } catch (exception: Exception) {
            Toast.makeText(musicPlayerService?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }
}


//アンドロイド アプリのバックグラウンド処理の部分である「サービス」と呼ばれる物
//
//音楽を再生する「サービス」
//
//Bluetoothイヤホンの物理ボタンなどの「メディア ボタン」との接続である「メディア セッション」に対応する場合は、「サービス」はServiceクラスではなくMediaBrowserServiceCompatクラスを継承
//
class MusicPlayerService : MediaBrowserServiceCompat(), MediaPlayer.OnCompletionListener {


    companion object {


        //アクティビティから、音楽を再生したりする当サービスへ送信する情報の名前
        //当サービスから、アクティビティへ通知する情報の名前
        //
        //重複を避けるため、「パッケージ名 + 情報の名前」
        //
        const val MUSIC_INFO_LIST_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_LIST"
        const val MUSIC_INFO_INDEX_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_INDEX"
        const val LOOP_MUSIC_KEY = "eliphas1810.freesearchablemusicplayer.LOOP_MUSIC"
        const val RANDOM_MUSIC_KEY = "eliphas1810.freesearchablemusicplayer.RANDOM_MUSIC"
        const val CURRENT_MUSIC_DURATION_KEY = "eliphas1810.freesearchablemusicplayer.CURRENT_MUSIC_DURATION"


        //アクティビティから、音楽を再生したりする当サービスへの送信情報のコードは、アンドロイド アプリ開発者の責任で重複させない事
        const val START_MUSIC_MESSAGE = 1
        const val PAUSE_MUSIC_MESSAGE = 2
        const val STOP_MUSIC_MESSAGE = 3
        const val PREVIOUS_MUSIC_MESSAGE = 4
        const val NEXT_MUSIC_MESSAGE = 5
        const val SEEK_MUSIC_MESSAGE = 6
        //const val LOOP_MUSIC_MESSAGE = 7
        //const val RANDOM_MUSIC_MESSAGE = 8
        const val REQUEST_MUSIC_INFO_MESSAGE = 9


        //音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知の名前
        const val UPDATE_MUSIC_INFO_KEY = "eliphas1810.freesearchablemusicplayer.UPDATE_MUSIC_INFO"


        //音楽ファイルの詳細画面に表示する現在の再生時間の更新を促す通知の名前
        const val UPDATE_MUSIC_CURRENT_DURATION_KEY = "eliphas1810.freesearchablemusicplayer.UPDATE_MUSIC_CURRENT_DURATION"


        //メディア セッションで、当サービスへの接続は許可するが、何も情報を返さない場合のメディア ルートID。
        //
        //当サービス内だけの、アプリ開発者独自の値
        //
        private const val EMPTY_MEDIA_ROOT_ID = "eliphas1810.freesearchablemusicplayer.EMPTY_MEDIA_ROOT_ID"
        private const val CHANNEL_ID = "eliphas1810.freesearchablemusicplayer.CHANNEL_ID"
    }


    private var messenger: Messenger? = null

    private var mediaPlayer: MediaPlayer? = null

    var musicInfoList: ArrayList<MusicInfo>? = null
    var musicInfoIndex = 0

    var loop = false
    var random = false

    var currentMusicDuration = 0

    var starting = false
    var pausing = false
    var stopping = false


    //当アプリ以外によるファイルの取得先
    var externalContentUri: Uri? = null

    private var audioBecomingNoisyBroadcastReceiver: AudioBecomingNoisyBroadcastReceiver? = null

    var audioManager: AudioManager? = null

    var audioFocusRequestCompat: AudioFocusRequestCompat? = null

    var mediaSessionCompat: MediaSessionCompat? = null

    var scheduledExecutorService: ScheduledExecutorService? = null


    //音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知をします。
    fun updateMusicInfo() {

        val intent = Intent(UPDATE_MUSIC_INFO_KEY)

        intent.putParcelableArrayListExtra(MUSIC_INFO_LIST_KEY, musicInfoList)
        intent.putExtra(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
        intent.putExtra(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)
        intent.putExtra(LOOP_MUSIC_KEY, loop)
        intent.putExtra(RANDOM_MUSIC_KEY, random)

        baseContext.sendBroadcast(intent)
    }


    //音楽ファイルの詳細画面に表示する現在の再生時間の更新を促す通知をします。
    fun updateMusicCuurentDuration() {

        currentMusicDuration = mediaPlayer?.currentPosition ?: 0

        val intent = Intent(UPDATE_MUSIC_CURRENT_DURATION_KEY)

        intent.putParcelableArrayListExtra(MUSIC_INFO_LIST_KEY, musicInfoList)
        intent.putExtra(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
        intent.putExtra(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

        baseContext.sendBroadcast(intent)
    }


    //音楽を再生
    fun start() {

        //オーディオ フォーカス(Audio Focus)を得られない場合
        if (AudioManagerCompat.requestAudioFocus(audioManager!!, audioFocusRequestCompat!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            //音楽を再生せず終了
            return
        }

        val musicInfo = musicInfoList?.get(musicInfoIndex)
        val mediaStoreId = musicInfo?.id
        val uri = ContentUris.withAppendedId(externalContentUri!!, mediaStoreId!!)

        if (starting) {

            mediaPlayer?.stop()

            stopping = true
            starting = false
            pausing = false

            currentMusicDuration = 0

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (pausing) {

            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (stopping) {

            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            stopping = false
            pausing = false

        } else {

            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false
        }
    }


    //音楽の再生を一時停止
    fun pause() {

        if (starting) {

            mediaPlayer?.pause()
            pausing = true
            starting = false
            stopping = false
        }
    }


    //音楽の再生を停止
    fun stop() {

        //得ていたオーディオ フォーカス(Audio Focus)を放棄
        AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, audioFocusRequestCompat!!)

        if (starting || pausing) {

            mediaPlayer?.stop()

            stopping = true
            starting = false
            pausing = false

            currentMusicDuration = 0
        }
    }


    //前の曲へ戻って再生
    fun previous() {

        if (loop == false) {

            val musicInfoCount = musicInfoList?.size ?: 0

            if (random) {
                musicInfoIndex = (0..(musicInfoCount - 1)).random()
            } else {
                musicInfoIndex = musicInfoIndex - 1
                if (musicInfoIndex <= -1) {
                    musicInfoIndex = musicInfoCount - 1
                }
            }
        }

        //音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知をします。
        updateMusicInfo()

        //オーディオ フォーカス(Audio Focus)を得られない場合
        if (AudioManagerCompat.requestAudioFocus(audioManager!!, audioFocusRequestCompat!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            //音楽を再生せず終了
            return
        }

        val musicInfo = musicInfoList?.get(musicInfoIndex)
        val mediaStoreId = musicInfo?.id
        val uri = ContentUris.withAppendedId(externalContentUri!!, mediaStoreId!!)

        if (starting || pausing) {

            mediaPlayer?.stop()

            stopping = true
            starting = false
            pausing = false

            currentMusicDuration = 0

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (stopping) {

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else {

            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false
        }
    }


    //次の曲へ進んで再生
    fun next() {

        if (loop == false) {

            val musicInfoCount = musicInfoList?.size ?: 0

            if (random) {
                musicInfoIndex = (0..(musicInfoCount - 1)).random()
            } else {
                musicInfoIndex = musicInfoIndex + 1
                if (musicInfoCount <= musicInfoIndex) {
                    musicInfoIndex = 0
                }
            }
        }

        //音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知をします。
        updateMusicInfo()

        //オーディオ フォーカス(Audio Focus)を得られない場合
        if (AudioManagerCompat.requestAudioFocus(audioManager!!, audioFocusRequestCompat!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            //音楽を再生せず終了
            return
        }

        val musicInfo = musicInfoList?.get(musicInfoIndex)
        val mediaStoreId = musicInfo?.id
        val uri = ContentUris.withAppendedId(externalContentUri!!, mediaStoreId!!)

        if (starting || pausing) {

            mediaPlayer?.stop()

            stopping = true
            starting = false
            pausing = false

            currentMusicDuration = 0

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (stopping) {

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else {

            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false
        }
    }


    //音楽の再生開始時間を指定して再生
    fun seek() {

        //オーディオ フォーカス(Audio Focus)を得られない場合
        if (AudioManagerCompat.requestAudioFocus(audioManager!!, audioFocusRequestCompat!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            //音楽を再生せず終了
            return
        }

        val musicInfo = musicInfoList?.get(musicInfoIndex)
        val mediaStoreId = musicInfo?.id
        val uri = ContentUris.withAppendedId(externalContentUri!!, mediaStoreId!!)

        if (starting) {

            mediaPlayer?.stop()

            stopping = true
            starting = false
            pausing = false

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.seekTo(currentMusicDuration)
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (pausing) {

            mediaPlayer?.seekTo(currentMusicDuration)
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false

        } else if (stopping) {

            mediaPlayer?.prepare()
            mediaPlayer?.seekTo(currentMusicDuration)
            mediaPlayer?.start()

            starting = true
            stopping = false
            pausing = false

        } else {

            mediaPlayer?.setDataSource(this, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.seekTo(currentMusicDuration)
            mediaPlayer?.start()

            starting = true
            pausing = false
            stopping = false
        }
    }


    //再生中の音楽が再生終了した場合を処理
    override fun onCompletion(mediaPlayer: MediaPlayer) {
        try {

            //次の音楽を再生
            next()

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    //メモリー上に作成される時にのみ呼ばれます。
    override fun onCreate() {
        try {
            super.onCreate()


            //アンドロイド8(オレオ)以上の場合
            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {

                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val notificationChannel = NotificationChannel(CHANNEL_ID, getText(R.string.notification_content_title), NotificationManager.IMPORTANCE_LOW)

                notificationManager.createNotificationChannel(notificationChannel)

                val notificationBuilder = Notification.Builder(applicationContext, CHANNEL_ID)

                notificationBuilder.setContentTitle(getText(R.string.notification_content_title))
                notificationBuilder.setContentText(getText(R.string.notification_content_text))
                notificationBuilder.setTicker(getText(R.string.notification_ticker))

                val notification = notificationBuilder.build()

                //startForeground()を実行しないと、アンドロイド システムに強制終了されてしまいます。
                //
                //startForeground()には、通知(Notification)が必要です。
                //
                startForeground(1, notification)
            }

            mediaPlayer = MediaPlayer()

            if (musicInfoList != null && 1 <= (musicInfoList?.size ?: 0)) {
                musicInfoList?.clear()
            }
            musicInfoList = ArrayList(listOf())

            //当アプリ以外によるファイルの取得先

            externalContentUri =
                if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) { //アンドロイド10(Q)以上の場合
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

            //再生中の音楽の再生終了を受け取る物を設定
            mediaPlayer?.setOnCompletionListener(this)

            //音楽再生中、画面がスリープされても、CPUがスリープされないようにします。
            mediaPlayer?.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)

            //アンドロイド システムの音楽の音量の設定を適用
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            //事故で意図せず有線イヤホンが抜けた場合などの通知を受け取る物を設定
            //
            //有線イヤホンや無線イヤホンなどからスマホやタブレットの内蔵スピーカーへ音声出力先が戻る通知を受け取る物を設定
            //
            audioBecomingNoisyBroadcastReceiver = AudioBecomingNoisyBroadcastReceiver(this)
            registerReceiver(audioBecomingNoisyBroadcastReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

            //電話アプリやYoutubeアプリといった他の音声を再生するアプリによってオーディオ フォーカス(Audio Focus)が失われた場合の処理

            audioManager = getSystemService(AudioManager::class.java)

            val audioAttributesCompatBuilder = AudioAttributesCompat.Builder()

            audioAttributesCompatBuilder.setUsage(AudioAttributesCompat.USAGE_MEDIA)
            audioAttributesCompatBuilder.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)

            val audioFocusRequestCompatBuilder = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)

            audioFocusRequestCompatBuilder.setAudioAttributes(audioAttributesCompatBuilder.build())

            audioFocusRequestCompatBuilder.setOnAudioFocusChangeListener { focusChange ->

                //電話が、かかってきて、通話が終了した時などの場合
                //
                //オーディオ フォーカス(Audio Focus)が戻ってきた場合
                if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                    //音楽の再生を再開
                    start()

                    //電話が、かかってきている時や、通話中などの場合
                    //
                    //オーディオ フォーカス(Audio Focus)が一時的に失われた場合
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

                    //音楽の再生を一時停止
                    pause()

                    //Youtubeアプリなどの音声を再生するアプリを起動した場合
                    //
                    //オーディオ フォーカス(Audio Focus)が失われた場合
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {

                    //音楽の再生を停止
                    stop()
                }
            }

            audioFocusRequestCompat = audioFocusRequestCompatBuilder.build()

            //Bluetoothイヤホンの物理ボタンなどの「メディア ボタン」との接続である「メディア セッション」の処理

            mediaSessionCompat = MediaSessionCompat(
                this,
                MusicPlayerService::class.java.name
            )

            val playbackStateCompatBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_STOP
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )

            mediaSessionCompat?.setPlaybackState(playbackStateCompatBuilder?.build())

            mediaSessionCompat?.setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(intent: Intent): Boolean {

                    var keyEvent = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) { //アンドロイド13(ティラミス)以上の場合
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent == null) {
                        return false
                    }

                    //ACTION_DOWNとACTION_UPの二重でonMediaButtonEventが呼ばれるので、ACTION_DOWNの場合だけ処理して、処理の重複を回避
                    if (keyEvent.action != KeyEvent.ACTION_DOWN) {
                        return false
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        start()
                        return true
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        pause()
                        return true
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { //再生と一時停止が同一の物理ボタンの場合

                        if (starting) {
                            pause()
                            return true
                        }

                        if (pausing || stopping) {
                            start()
                        }

                        return true
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                        stop()
                        return true
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        previous()
                        return true
                    }

                    if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        next()
                        return true
                    }

                    return super.onMediaButtonEvent(intent)
                }
            })

            sessionToken = mediaSessionCompat?.sessionToken

            mediaSessionCompat?.isActive = true

            MediaButtonReceiver.handleIntent(
                mediaSessionCompat,
                Intent(
                    applicationContext,
                    MusicPlayerService::class.java
                )
            )

            //1秒おきに、音楽ファイルの詳細画面に表示する現在の再生時間の更新を促す通知をします。
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            scheduledExecutorService?.scheduleAtFixedRate(
                {
                    try {

                        if (starting) {

                            //音楽ファイルの詳細画面に表示する現在の再生時間の更新を促す通知をします。
                            updateMusicCuurentDuration()
                        }

                    } catch (exception: Exception) {
                        Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }
                },
                1, //1回目までの時間間隔の時間数
                1, //1回目以降の時間間隔の時間数
                TimeUnit.SECONDS //時間の単位。秒。
            )

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    //「サービス」が起動される時に呼ばれます。
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            return START_NOT_STICKY
        }
    }


    //「サービス」がバインドされる時に呼ばれます。
    override fun onBind(intent: Intent): IBinder? {
        try {

            messenger = Messenger(MusicPlayerActivityHandler(this))

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {

            return messenger?.binder
        }
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
    }


    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null) //メディア セッションで、当サービスへの接続は許可するが、何も情報を返しません。
        return
    }


    //メモリーから破棄される時にのみ呼ばれます。
    override fun onDestroy() {
        try {

            scheduledExecutorService?.shutdownNow()
            scheduledExecutorService = null

            messenger = null

            if (mediaPlayer?.isPlaying() ?: false) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null

            musicInfoList?.clear()
            musicInfoList = null

            externalContentUri = null

            unregisterReceiver(audioBecomingNoisyBroadcastReceiver)
            audioBecomingNoisyBroadcastReceiver?.musicPlayerService = null
            audioBecomingNoisyBroadcastReceiver = null

            AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, audioFocusRequestCompat!!)
            audioManager = null
            audioFocusRequestCompat = null

            mediaSessionCompat?.release()
            mediaSessionCompat = null

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.onDestroy()
        }
    }
}
