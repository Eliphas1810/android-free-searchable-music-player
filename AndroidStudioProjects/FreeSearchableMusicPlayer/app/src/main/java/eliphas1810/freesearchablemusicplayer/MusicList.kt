package eliphas1810.freesearchablemusicplayer

import android.content.Context
import android.content.Intent
import android.os.*
import android.os.Parcelable.Creator
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity

//Public Domain

//音楽ファイルの情報をひとまとめにした物
//
//Parcelableはアクティビティとサービス間で送信し合う事ができる情報
//
data class MusicInfo(
    var id: Long?,
    var musicTitle: String?,
    var artistName: String?,
    var albumTitle: String?,
    var filePath: String?,
    var duration: Int? //音楽の所要時間
) : Parcelable {

    constructor(parcel: Parcel) : this(0, null, null, null, null, 0) {
        id = parcel.readLong()
        musicTitle = parcel.readString()
        artistName = parcel.readString()
        albumTitle = parcel.readString()
        filePath = parcel.readString()
        duration = parcel.readInt()
    }

    companion object {

        @field: JvmField
        val CREATOR: Creator<MusicInfo?> = object : Creator<MusicInfo?> {
            override fun createFromParcel(parcel: Parcel): MusicInfo? {
                return MusicInfo(parcel)
            }

            override fun newArray(size: Int): Array<MusicInfo?> {
                return arrayOfNulls<MusicInfo>(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) { //コンストラクタでParcelから取得する順番と同じ順番でParcelに書き込む必要が有ります。
        parcel.writeLong(id ?: 0) //idがnullの場合はゼロ
        parcel.writeString(musicTitle ?: "null") //曲名がnullの場合は「null」という文字
        parcel.writeString(artistName ?: "null")
        parcel.writeString(albumTitle ?: "null")
        parcel.writeString(filePath ?: "null")
        parcel.writeInt(duration ?: 0)
    }

    //Parcel.describeContents()は普通はゼロを返すように実装
    override fun describeContents(): Int {
        return 0
    }
}


//音楽ファイルの一覧画面で、Listの各件の内容をListViewの各行に設定する物
private class Adapter(context: Context, list: List<MusicInfo>) : ArrayAdapter<MusicInfo>(context, R.layout.music_list_row, list) {

    //例えば、「1分02.003秒」という形式に音楽の総再生時間を編集
    fun convertMusicDurationToText(musicDuration: Int) : String {

        val minutes = musicDuration / (1000 * 60)
        val seconds = (musicDuration % (1000 * 60)) / 1000
        val milliSeconds = musicDuration % 1000

        var durationText = ""
        durationText = durationText + minutes
        durationText = durationText + context.getString(R.string.minutes_unit_label)
        durationText = durationText + seconds
        durationText = durationText + context.getString(R.string.seconds_and_milli_seconds_separator)
        durationText = durationText + "%03d".format(milliSeconds)
        durationText = durationText + context.getString(R.string.seconds_unit_label)
        return durationText
    }

    //音楽ファイルの一覧画面で、Listの各件の内容をListViewの各行に設定
    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {

        var view: View? = view

        try {

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.music_list_row, viewGroup, false)
            }

            val musicInfo = getItem(position)

            view?.findViewById<TextView>(R.id.musicListMusicTitle)?.text = musicInfo?.musicTitle
            view?.findViewById<TextView>(R.id.musicListArtistName)?.text = musicInfo?.artistName
            view?.findViewById<TextView>(R.id.musicListAlbumTitle)?.text = musicInfo?.albumTitle
            view?.findViewById<TextView>(R.id.musicListMusicFilePath)?.text = musicInfo?.filePath
            view?.findViewById<TextView>(R.id.musicListMusicDuration)?.text = convertMusicDurationToText(musicInfo?.duration ?: 0)

        } catch (exception: Exception) {
            Toast.makeText(view?.context?.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }

        return view!!
    }
}


//音楽ファイル一覧画面と1対1対応のアクティビティ
class MusicList : AppCompatActivity() {

    companion object {

        //メイン画面のアクティビティから、音楽ファイル一覧画面のアクティビティへ送信する情報の名前
        //
        //重複を避けるため、「パッケージ名 + 情報の名前」
        //
        const val INCLUSION_PATTERN_KEY = "eliphas1810.freesearchablemusicplayer.INCLUSION_PATTERN"
        const val EXCLUSION_PATTERN_KEY = "eliphas1810.freesearchablemusicplayer.EXCLUSION_PATTERN"


        //音楽ファイル一覧画面のアクティビティから、音楽ファイル詳細画面のアクティビティへ送信する情報の名前
        //
        //重複を避けるため、「パッケージ名 + 情報の名前」
        //
        const val MUSIC_INFO_LIST_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_LIST"
        const val MUSIC_INFO_INDEX_KEY = "eliphas1810.freesearchablemusicplayer.MUSIC_INFO_INDEX"
    }


    private var musicInfoList: MutableList<MusicInfo>? = null


    //全角の英大文字、半角の英大文字、全角の英小文字を半角の英小文字に置換
    private fun halfWidthLowerCase(string : String) : String {

        if (string == null) {
            return ""
        }

        return string
            .replace("ａ", "a")
            .replace("ｂ", "b")
            .replace("ｃ", "c")
            .replace("ｄ", "d")
            .replace("ｅ", "e")
            .replace("ｆ", "f")
            .replace("ｇ", "g")
            .replace("ｈ", "h")
            .replace("ｉ", "i")
            .replace("ｊ", "j")
            .replace("ｋ", "k")
            .replace("ｌ", "l")
            .replace("ｍ", "m")
            .replace("ｎ", "n")
            .replace("ｏ", "o")
            .replace("ｐ", "p")
            .replace("ｑ", "q")
            .replace("ｒ", "r")
            .replace("ｓ", "s")
            .replace("ｔ", "t")
            .replace("ｕ", "u")
            .replace("ｖ", "v")
            .replace("ｗ", "w")
            .replace("ｘ", "x")
            .replace("ｙ", "y")
            .replace("ｚ", "z")
            .replace("Ａ", "A")
            .replace("Ｂ", "B")
            .replace("Ｃ", "C")
            .replace("Ｄ", "D")
            .replace("Ｅ", "E")
            .replace("Ｆ", "F")
            .replace("Ｇ", "G")
            .replace("Ｈ", "H")
            .replace("Ｉ", "I")
            .replace("Ｊ", "J")
            .replace("Ｋ", "K")
            .replace("Ｌ", "L")
            .replace("Ｍ", "M")
            .replace("Ｎ", "N")
            .replace("Ｏ", "O")
            .replace("Ｐ", "P")
            .replace("Ｑ", "Q")
            .replace("Ｒ", "R")
            .replace("Ｓ", "S")
            .replace("Ｔ", "T")
            .replace("Ｕ", "U")
            .replace("Ｖ", "V")
            .replace("Ｗ", "W")
            .replace("Ｘ", "X")
            .replace("Ｙ", "Y")
            .replace("Ｚ", "Z")
            .lowercase()
    }


    //メモリー上に作成される時にのみ呼ばれます。
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.music_list)


            //音楽ファイル一覧画面の閉じるボタンが押された時の処理
            findViewById<Button>(R.id.musicListClose).setOnClickListener { view ->
                try {

                    //前の画面へ戻る
                    finish()

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            if (musicInfoList != null && 1 <= (musicInfoList?.size ?: 0)) {
                musicInfoList?.clear()
            }

            musicInfoList = mutableListOf<MusicInfo>()


            //前の画面から、検索条件の文字パターン「正規表現」を取得
            var inclusionPattern = getIntent()?.getStringExtra(INCLUSION_PATTERN_KEY) ?: ""

            inclusionPattern = halfWidthLowerCase(inclusionPattern) //全角の英大文字、半角の英大文字、全角の英小文字を半角の英小文字に置換

            var inclusionRegex : Regex? = null
            if (inclusionPattern != "") {
                inclusionRegex = Regex(inclusionPattern)
            }


            //前の画面から、除外条件の文字パターン「正規表現」を取得
            var exclusionPattern = getIntent()?.getStringExtra(EXCLUSION_PATTERN_KEY) ?: ""

            exclusionPattern = halfWidthLowerCase(exclusionPattern) //全角の英大文字、半角の英大文字、全角の英小文字を半角の英小文字に置換

            var exclusionRegex : Regex? = null
            if (exclusionPattern != "") {
                exclusionRegex = Regex(exclusionPattern)
            }


            //当アプリ以外によるファイルの取得先
            val externalContentUri =
                if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) { //アンドロイド10(Q)以上の場合
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

            getContentResolver().query(
                externalContentUri, //当アプリ以外によるファイルの取得先
                arrayOf( //SQLのSELECTに相当
                    MediaStore.Audio.Media._ID, //android.media.MediaPlayerへの曲の指定に必要なID
                    MediaStore.Audio.Media.TITLE, //音楽ファイルの曲名を取得
                    MediaStore.Audio.Media.ARTIST, //音楽ファイルのアーティスト名を取得
                    MediaStore.Audio.Media.ALBUM, //音楽ファイルのアルバム名を取得
                    MediaStore.Audio.Media.DATA, //音楽ファイルのパスを取得
                    MediaStore.Audio.Media.DURATION //音楽ファイルの総再生時間を取得
                ),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", //SQLのWHEREに相当。音楽ファイルに限定して一覧検索。
                null, //SQLのWHEREの?への指定パラメーターに相当
                "${MediaStore.Audio.Media.TITLE} ASC" //SQLのORDER BYに相当
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val filePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {

                    var id = cursor.getLong(idIndex)
                    var title = cursor.getString(titleIndex)
                    var artist = cursor.getString(artistIndex)
                    var album = cursor.getString(albumIndex)
                    var filePath = cursor.getString(filePathIndex)
                    var duration = cursor.getInt(durationIndex)


                    var musicInfoTsv = title + "\t" + artist + "\t" + album + "\t" + filePath

                    musicInfoTsv = halfWidthLowerCase(musicInfoTsv) //全角の英大文字、半角の英大文字、全角の英小文字を半角の英小文字に置換

                    //検索条件が未指定の場合か、検索条件が含まれている場合
                    if (inclusionRegex == null || inclusionRegex.containsMatchIn(musicInfoTsv)) {

                        //除外条件が未指定の場合か、除外条件が含まれていない場合
                        if (exclusionRegex == null || exclusionRegex.containsMatchIn(musicInfoTsv) == false) {

                            musicInfoList?.add(MusicInfo(id, title, artist, album, filePath, duration))
                        }
                    }
                }
            }

            //音楽ファイルが見つからない場合
            if ((musicInfoList?.size ?: 0) <= 0) {
                Toast.makeText(this, getString(R.string.no_music_file_list), Toast.LENGTH_LONG).show()
            }

            //音楽ファイルの一覧画面で、ListViewの各行の内容を設定する物を指定
            val listView = findViewById<ListView>(R.id.musicList)
            listView.adapter = Adapter(this, musicInfoList ?: mutableListOf<MusicInfo>())

            //音楽ファイルの一覧画面で、ListViewの各行が押された時の処理
            listView.onItemClickListener = OnItemClickListener { adapterView, view, position, id ->
                try {

                    //音楽ファイルの詳細画面へ遷移
                    val intent = Intent(this, MusicDetail::class.java)
                    intent.putExtra(MUSIC_INFO_LIST_KEY, ArrayList(musicInfoList))
                    intent.putExtra(MUSIC_INFO_INDEX_KEY, position)
                    startActivity(intent)

                } catch (exception: Exception) {
                    Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }

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

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        } finally {
            super.onDestroy()
        }
    }
}
