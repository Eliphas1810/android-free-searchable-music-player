package eliphas1810.freesearchablemusicplayer


import android.content.*
import android.os.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

//Public Domain
//
//音楽ファイル詳細画面と1対1対応のアクティビティ
class MusicDetail : AppCompatActivity() {


    companion object {

        //音楽ファイル一覧画面のアクティビティから、音楽ファイル詳細画面のアクティビティへ送信する情報の名前
        //当アクティビティから、音楽を再生したりするサービスへ送信する情報の名前
        //サービスからの通知情報の名前
        //
        //重複を避けるため、「パッケージ名 + 情報の名前」
        //
        const val MUSIC_INFO_LIST_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_LIST"
        const val MUSIC_INFO_INDEX_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_INDEX"
        const val LOOP_MUSIC_KEY = "eliphas1810.freesearchablemusicplayer.LOOP_MUSIC"
        const val RANDOM_MUSIC_KEY = "eliphas1810.freesearchablemusicplayer.RANDOM_MUSIC"
        const val CURRENT_MUSIC_DURATION_KEY = "eliphas1810.freesearchablemusicplayer.CURRENT_MUSIC_DURATION"


        //音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促される通知の名前
        const val UPDATE_MUSIC_INFO_KEY = "eliphas1810.freesearchablemusicplayer.UPDATE_MUSIC_INFO"


        //音楽ファイルの詳細画面に表示する現在の再生時間の更新を促される通知の名前
        const val UPDATE_MUSIC_CURRENT_DURATION_KEY = "eliphas1810.freesearchablemusicplayer.UPDATE_MUSIC_CURRENT_DURATION"


        //当アクティビティから、音楽を再生したりするサービスへの送信情報のコードは、アンドロイド アプリ開発者の責任で重複させない事
        const val START_MUSIC_MESSAGE = 1
        const val PAUSE_MUSIC_MESSAGE = 2
        const val STOP_MUSIC_MESSAGE = 3
        const val PREVIOUS_MUSIC_MESSAGE = 4
        const val NEXT_MUSIC_MESSAGE = 5
        const val SEEK_MUSIC_MESSAGE = 6
        const val LOOP_MUSIC_MESSAGE = 7
        const val RANDOM_MUSIC_MESSAGE = 8
        const val REQUEST_MUSIC_INFO_MESSAGE = 9
    }


    var musicInfoList: ArrayList<MusicInfo>? = null
    var musicInfoIndex = 0
    var loop = false
    var random = false
    var currentMusicDuration = 0

    var musicCurrentDurationChanging = false

    var connectingWithService: Boolean = false

    private var serviceConnection: ServiceConnection? = null

    var messenger: Messenger? = null

    private var musicInfoUpdateBroadcastReceiver: BroadcastReceiver? = null

    private var musicCurrentDurationUpdateBroadcastReceiver: BroadcastReceiver? = null


    //例えば、「1分2.003秒」といった形式に音楽の再生時間を編集
    fun convertMusicDurationToText(musicDuration: Int) : String {

        val minutes = musicDuration / (1000 * 60)
        val seconds = (musicDuration % (1000 * 60)) / 1000
        val milliSeconds = musicDuration % 1000

        var musicDurationText = ""
        musicDurationText = musicDurationText + minutes
        musicDurationText = musicDurationText + getString(R.string.minutes_unit_label)
        musicDurationText = musicDurationText + seconds
        musicDurationText = musicDurationText + getString(R.string.seconds_and_milli_seconds_separator)
        musicDurationText = musicDurationText + "%03d".format(milliSeconds)
        musicDurationText = musicDurationText + getString(R.string.seconds_unit_label)
        return musicDurationText
    }


    //メモリー上に作成される時にのみ呼ばれます。
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.music_detail)


            if (musicInfoList != null && 1 <= (musicInfoList?.size ?: 0)) {
                musicInfoList?.clear()
            }

            musicInfoList = ArrayList(listOf())


            serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                    messenger = Messenger(iBinder)
                    connectingWithService = true
                }
                override fun onServiceDisconnected(componentName: ComponentName) {
                    messenger = null
                    connectingWithService = false
                }
            }


            //アンドロイド8(オレオ)以上の場合
            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {

                //アンドロイド アプリのバックグラウンド処理の部分である「サービス」と呼ばれる物を起動
                //
                //音楽を再生する「サービス」を起動
                //
                //音楽を再生する「サービス」がまだ無い場合は新規作成されます。
                //
                startForegroundService(Intent(applicationContext, MusicPlayerService::class.java))

            } else {

                //アンドロイド アプリのバックグラウンド処理の部分である「サービス」と呼ばれる物を起動
                //
                //音楽を再生する「サービス」を起動
                //
                //音楽を再生する「サービス」がまだ無い場合は新規作成されます。
                //
                startService(Intent(applicationContext, MusicPlayerService::class.java))
            }


            //アンドロイド アプリのバックグラウンド処理の部分である「サービス」と呼ばれる物へ接続
            //
            //音楽を再生する「サービス」へ接続
            //
            //音楽を再生する「サービス」がまだ無い場合は新規作成されます。
            //
            bindService(Intent(applicationContext, MusicPlayerService::class.java), serviceConnection!!, Context.BIND_AUTO_CREATE)


            //サービスからの、音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知を受け取ります。
            musicInfoUpdateBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {

                        val bundle = intent?.extras

                        //サービスからの通知情報から、音楽ファイルの一覧の情報を受け取ります。

                        musicInfoList = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) { //アンドロイド13(ティラミス)以上の場合

                            bundle?.getParcelableArrayList(MUSIC_INFO_LIST_KEY, MusicInfo::class.java)

                        } else {

                            bundle?.getParcelableArrayList(MUSIC_INFO_LIST_KEY)
                        }


                        if ((musicInfoList?.size ?: 0) <= 0) {
                            return
                        }


                        //サービスからの通知情報から、選択中のゼロから始まる音楽ファイルの番号を受け取ります。
                        musicInfoIndex = bundle?.getInt(MUSIC_INFO_INDEX_KEY) ?: 0


                        //サービスからの通知情報から、ループ再生するか否かを受け取ります。
                        loop = bundle?.getBoolean(LOOP_MUSIC_KEY) ?: false


                        //サービスからの通知情報から、ループ再生するか否かを受け取ります。
                        random = bundle?.getBoolean(RANDOM_MUSIC_KEY) ?: false


                        //音楽ファイルの詳細画面の内容を更新

                        val musicInfo = musicInfoList?.get(musicInfoIndex)

                        findViewById<TextView>(R.id.musicDetailMusicTitle)?.text = musicInfo?.musicTitle
                        findViewById<TextView>(R.id.musicDetailArtistName)?.text = musicInfo?.artistName
                        findViewById<TextView>(R.id.musicDetailAlbumTitle)?.text = musicInfo?.albumTitle
                        findViewById<TextView>(R.id.musicDetailMusicFilePath)?.text = musicInfo?.filePath
                        findViewById<TextView>(R.id.musicDetailMusicDuration)?.text = convertMusicDurationToText(musicInfo?.duration ?: 0)

                        if (loop) {
                            findViewById<Button>(R.id.musicDetailLoop)?.setTextColor(resources.getColor(R.color.white, theme))
                            findViewById<Button>(R.id.musicDetailLoop)?.setBackgroundColor(resources.getColor(R.color.lime, theme))
                        } else {
                            findViewById<Button>(R.id.musicDetailLoop)?.setTextColor(resources.getColor(R.color.silver, theme))
                            findViewById<Button>(R.id.musicDetailLoop)?.setBackgroundColor(resources.getColor(R.color.gray, theme))
                        }


                        if (random) {
                            findViewById<Button>(R.id.musicDetailRandom)?.setTextColor(resources.getColor(R.color.white, theme))
                            findViewById<Button>(R.id.musicDetailRandom)?.setBackgroundColor(resources.getColor(R.color.lime, theme))
                        } else {
                            findViewById<Button>(R.id.musicDetailRandom)?.setTextColor(resources.getColor(R.color.silver, theme))
                            findViewById<Button>(R.id.musicDetailRandom)?.setBackgroundColor(resources.getColor(R.color.gray, theme))
                        }


                        //音楽ファイルの詳細画面の、現在の再生時間のシーク バーが動かされていない場合
                        if (musicCurrentDurationChanging == false) {

                            //サービスからの通知情報から、現在の再生時間を受け取ります。
                            currentMusicDuration = bundle?.getInt(CURRENT_MUSIC_DURATION_KEY) ?: 0

                            //音楽ファイルの詳細画面の、現在の再生時間のシーク バーを更新
                            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).max = musicInfo?.duration ?: 0
                            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

                            //音楽ファイルの詳細画面の、現在の再生時間を更新
                            findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)
                        }

                    } catch (exception: Exception) {
                        Toast.makeText(context?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }
                }
            }


            if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {
                registerReceiver(musicInfoUpdateBroadcastReceiver, IntentFilter(UPDATE_MUSIC_INFO_KEY), RECEIVER_EXPORTED)
            } else {
                registerReceiver(musicInfoUpdateBroadcastReceiver, IntentFilter(UPDATE_MUSIC_INFO_KEY))
            }


            //サービスからの、音楽ファイルの詳細画面に表示する現在の再生時間の更新を促す通知を受け取ります。
            musicCurrentDurationUpdateBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {

                        //音楽ファイルの詳細画面の、現在の再生時間のシーク バーが動かされていない場合
                        if (musicCurrentDurationChanging == false) {

                            val bundle = intent?.extras

                            //サービスからの通知情報から、音楽ファイルの一覧の情報を受け取ります。

                            musicInfoList = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) { //アンドロイド13(ティラミス)以上の場合
                                bundle?.getParcelableArrayList(MUSIC_INFO_LIST_KEY, MusicInfo::class.java)
                            } else {
                                bundle?.getParcelableArrayList(MUSIC_INFO_LIST_KEY)
                            }

                            //サービスからの通知情報から、選択中のゼロから始まる音楽ファイルの番号を受け取ります。
                            musicInfoIndex = bundle?.getInt(MUSIC_INFO_INDEX_KEY) ?: 0

                            val musicInfo = musicInfoList?.get(musicInfoIndex)

                            //サービスからの通知情報から、現在の再生時間を受け取ります。
                            currentMusicDuration = bundle?.getInt(CURRENT_MUSIC_DURATION_KEY) ?: 0

                            //音楽ファイルの詳細画面の、現在の再生時間のシーク バーを更新
                            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).max = musicInfo?.duration ?: 0
                            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

                            //音楽ファイルの詳細画面の、現在の再生時間を更新
                            findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)
                        }

                    } catch (exception: Exception) {
                        Toast.makeText(context?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                        throw exception
                    }
                }
            }


            if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {
                registerReceiver(musicCurrentDurationUpdateBroadcastReceiver, IntentFilter(UPDATE_MUSIC_CURRENT_DURATION_KEY), RECEIVER_EXPORTED)
            } else {
                registerReceiver(musicCurrentDurationUpdateBroadcastReceiver, IntentFilter(UPDATE_MUSIC_CURRENT_DURATION_KEY))
            }


            //前の画面から音楽ファイルの一覧を取得

            musicInfoList = if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) { //アンドロイド13(ティラミス)以上の場合
                ArrayList(intent.getParcelableArrayListExtra(MUSIC_INFO_LIST_KEY, MusicInfo::class.java))
            } else {
                ArrayList(intent.getParcelableArrayListExtra(MUSIC_INFO_LIST_KEY))
            }


            //前の画面から選択されたゼロから始まる音楽ファイルの番号を取得
            musicInfoIndex = intent.getIntExtra(MUSIC_INFO_INDEX_KEY, 0)


            val musicInfo = musicInfoList?.get(musicInfoIndex)


            //画面の1回目の表示時の処理
            findViewById<TextView>(R.id.musicDetailMusicTitle)?.text = musicInfo?.musicTitle
            findViewById<TextView>(R.id.musicDetailArtistName)?.text = musicInfo?.artistName
            findViewById<TextView>(R.id.musicDetailAlbumTitle)?.text = musicInfo?.albumTitle
            findViewById<TextView>(R.id.musicDetailMusicFilePath)?.text = musicInfo?.filePath
            findViewById<TextView>(R.id.musicDetailMusicDuration)?.text = convertMusicDurationToText(musicInfo?.duration ?: 0)

            findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)

            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).max = musicInfo?.duration ?: 0
            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

            findViewById<Button>(R.id.musicDetailLoop).setTextColor(resources.getColor(R.color.silver, theme))
            findViewById<Button>(R.id.musicDetailLoop).setBackgroundColor(resources.getColor(R.color.gray, theme))

            findViewById<Button>(R.id.musicDetailRandom).setTextColor(resources.getColor(R.color.silver, theme))
            findViewById<Button>(R.id.musicDetailRandom).setBackgroundColor(resources.getColor(R.color.gray, theme))


            //閉じるボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailClose).setOnClickListener { view ->
                try {

                    //音楽を再生する「サービス」に、音楽を停止するようにメッセージを送信

                    val bundle = Bundle()
                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, STOP_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                    //音楽を再生する「サービス」を終了させます。
                    stopService(Intent(applicationContext, MusicPlayerService::class.java))

                    //前の画面へ戻る
                    finish()

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //音楽ファイルの詳細画面の、現在の再生時間のシーク バーが操作された時の処理
            findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {

                    musicCurrentDurationChanging = true

                    //音楽を再生する「サービス」に、音楽を一時停止するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, PAUSE_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
                    currentMusicDuration = progress
                    findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {

                    //音楽を再生する「サービス」に、音楽の再生開始時間を指定して、再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, SEEK_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                    musicCurrentDurationChanging = false
                }
            })


            //再生ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailStart).setOnClickListener { view ->
                try {

                    //音楽を再生する「サービス」に、音楽を再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, START_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //一時停止ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailPause).setOnClickListener { view ->
                try {

                    //音楽を再生する「サービス」に、音楽を一時停止するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, PAUSE_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //停止ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailStop).setOnClickListener { view ->
                try {

                    currentMusicDuration = 0

                    findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)

                    findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

                    //音楽を再生する「サービス」に、音楽を停止するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, STOP_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //「前の曲へ戻る」ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailPrevious).setOnClickListener { view ->
                try {

                    currentMusicDuration = 0

                    findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)

                    findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

                    //音楽を再生する「サービス」に、前の曲へ戻って再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, PREVIOUS_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //「次の曲へ進む」ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailNext).setOnClickListener { view ->
                try {

                    currentMusicDuration = 0

                    findViewById<TextView>(R.id.musicDetailMusicCurrentDuration)?.text = convertMusicDurationToText(currentMusicDuration)

                    findViewById<SeekBar>(R.id.musicDetailMusicCurrentDurationSeekBar).progress = currentMusicDuration

                    //音楽を再生する「サービス」に、次の曲へ進んで再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, NEXT_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //「ループ モード」ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailLoop).setOnClickListener { view ->
                try {

                    loop = !loop

                    //音楽を再生する「サービス」に、ループ再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, LOOP_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                    if (loop) {
                        findViewById<Button>(R.id.musicDetailLoop)?.setTextColor(resources.getColor(R.color.white, theme))
                        findViewById<Button>(R.id.musicDetailLoop)?.setBackgroundColor(resources.getColor(R.color.lime, theme))
                    } else {
                        findViewById<Button>(R.id.musicDetailLoop)?.setTextColor(resources.getColor(R.color.silver, theme))
                        findViewById<Button>(R.id.musicDetailLoop)?.setBackgroundColor(resources.getColor(R.color.gray, theme))
                    }

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //「ランダム モード」ボタンが押された時の処理
            findViewById<Button>(R.id.musicDetailRandom).setOnClickListener { view ->
                try {

                    random = !random

                    //音楽を再生する「サービス」に、ランダム再生するようにメッセージを送信

                    val bundle = Bundle()

                    bundle.putParcelableArrayList(MUSIC_INFO_LIST_KEY, musicInfoList)
                    bundle.putInt(MUSIC_INFO_INDEX_KEY, musicInfoIndex)
                    bundle.putBoolean(LOOP_MUSIC_KEY, loop)
                    bundle.putBoolean(RANDOM_MUSIC_KEY, random)
                    bundle.putInt(CURRENT_MUSIC_DURATION_KEY, currentMusicDuration)

                    val message: Message = Message.obtain(null, RANDOM_MUSIC_MESSAGE)

                    message.data = bundle

                    messenger?.send(message)

                    if (random) {
                        findViewById<Button>(R.id.musicDetailRandom)?.setTextColor(resources.getColor(R.color.white, theme))
                        findViewById<Button>(R.id.musicDetailRandom)?.setBackgroundColor(resources.getColor(R.color.lime, theme))
                    } else {
                        findViewById<Button>(R.id.musicDetailRandom)?.setTextColor(resources.getColor(R.color.silver, theme))
                        findViewById<Button>(R.id.musicDetailRandom)?.setBackgroundColor(resources.getColor(R.color.gray, theme))
                    }

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //音楽を再生する「サービス」に、音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知をするようにメッセージを送信

            val message: Message = Message.obtain(null, REQUEST_MUSIC_INFO_MESSAGE)

            messenger?.send(message)


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    override fun onResume() {
        try {
            super.onResume()


            //音楽を再生する「サービス」に、音楽ファイルの詳細画面に表示する音楽ファイルの情報の更新を促す通知をするようにメッセージを送信

            val message: Message = Message.obtain(null, REQUEST_MUSIC_INFO_MESSAGE)

            messenger?.send(message)

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    //メモリーから破棄される時にのみ呼ばれます。
    override fun onDestroy() {
        try {

            musicInfoList?.clear()
            musicInfoList = null

            unregisterReceiver(musicInfoUpdateBroadcastReceiver)
            musicInfoUpdateBroadcastReceiver = null

            unregisterReceiver(musicCurrentDurationUpdateBroadcastReceiver)
            musicCurrentDurationUpdateBroadcastReceiver = null

            messenger = null

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.onDestroy()
        }
    }
}
