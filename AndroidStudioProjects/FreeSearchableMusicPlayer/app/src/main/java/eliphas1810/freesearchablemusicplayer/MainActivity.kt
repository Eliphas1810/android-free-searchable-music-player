package eliphas1810.freesearchablemusicplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

//Public Domain
//
//メイン画面と1対1対応のアクティビティ
class MainActivity : AppCompatActivity() {


    companion object {

        //メイン画面のアクティビティから、音楽ファイル一覧画面のアクティビティへ送信する情報の名前
        //
        //重複を避けるため、「パッケージ名 + 情報の名前」
        //
        const val INCLUSION_PATTERN_KEY = "eliphas1810.freesearchablemusicplayer.INCLUSION_PATTERN"
        const val EXCLUSION_PATTERN_KEY = "eliphas1810.freesearchablemusicplayer.EXCLUSION_PATTERN"

        //アンドロイド アプリ開発者が管理する場合の、権限の許可のリクエストコードは、アンドロイド アプリ開発者の責任で重複させない事
        private const val READ_MEDIA_AUDIO_REQUEST_CODE = 1
        private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 2
    }


    //権限の許可の要求の結果が出た時に呼ばれます。
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可の有無が選択された場合
            if (requestCode == READ_MEDIA_AUDIO_REQUEST_CODE) {

                //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限を許可された場合
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    return

                    //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限を許可されなかった場合
                } else {

                    //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可が無いので、当アプリを実行できない事を説明して、処理を終了
                    AlertDialog.Builder(this)
                        .setMessage(getString(R.string.denied_read_media_audio))
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        }
                        .create()
                        .show()

                    return
                }
            }

            //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可の有無が選択された場合
            if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {

                //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限を許可された場合
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    return

                    //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限を許可されなかった場合
                } else {

                    //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可が無いので、当アプリを実行できない事を説明して、処理を終了
                    AlertDialog.Builder(this)
                        .setMessage(getString(R.string.denied_read_external_storage))
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        }
                        .create()
                        .show()

                    return
                }
            }

        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }


    //メモリー上に作成される時にのみ呼ばれます。
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)


            //検索ボタンが押された時の処理
            findViewById<Button>(R.id.mainSearch).setOnClickListener { view ->
                try {

                    var inclusionPattern: String = findViewById<EditText>(R.id.mainInclusionPattern)?.text?.toString() ?: "";
                    if (inclusionPattern != "") {
                        //検索条件の「正規表現」が正しいか検査
                        try {
                            Regex(inclusionPattern)
                        } catch (exception: Exception) {
                            Toast.makeText(view.context.applicationContext, getString(R.string.main_inclusion_pattern_wrong) + exception.message, Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    }

                    var exclusionPattern: String = findViewById<EditText>(R.id.mainExclusionPattern)?.text?.toString() ?: "";
                    if (exclusionPattern != "") {
                        //除外条件の「正規表現」が正しいか検査
                        try {
                            Regex(exclusionPattern)
                        } catch (exception: Exception) {
                            Toast.makeText(view.context.applicationContext, getString(R.string.main_exclusion_pattern_wrong) + exception.message, Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    }

                    //音楽ファイルの一覧画面へ遷移
                    val intent = Intent(this, MusicList::class.java)
                    intent.putExtra(INCLUSION_PATTERN_KEY, inclusionPattern)
                    intent.putExtra(EXCLUSION_PATTERN_KEY, exclusionPattern)
                    startActivity(intent)

                } catch (exception: Exception) {
                    Toast.makeText(view.context.applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
                    throw exception
                }
            }


            //権限の許可の確認

            //当アプリ以外によるファイルを読み取る権限の許可が無い場合
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                //アンドロイド ティラミス以上の場合
                //アンドロイド13以上の場合
                if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {

                    //アンドロイド13以降、Manifest.permission.READ_MEDIA_AUDIOが存在

                    //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可が無い場合
                    if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED) {

                        //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可ダイアログで「今後、表示しない」を未選択の場合
                        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)) {

                            //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可ダイアログを表示して、onRequestPermissionsResultで選択結果を受け取る
                            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), READ_MEDIA_AUDIO_REQUEST_CODE)

                            //一旦、当処理は終了
                            return

                            //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可ダイアログで「今後、表示しない」を選択中の場合
                        } else {

                            //当アプリ以外による音楽を含む音声メディア ファイルを読み取る権限の許可が無いので、当アプリを実行できない事を説明して、処理を終了

                            AlertDialog.Builder(this)
                                .setMessage(getString(R.string.denied_read_media_audio))
                                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                }
                                .create()
                                .show()

                            return
                        }
                    }

                    //アンドロイド12以下の場合
                } else {

                    //当アプリ以外によるファイルを読み取る権限の許可ダイアログで「今後、表示しない」を未選択の場合
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        //当アプリ以外によるファイルを読み取る権限の許可ダイアログを表示して、onRequestPermissionsResultで選択結果を受け取る
                        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQUEST_CODE)

                        //一旦、当処理は終了
                        return

                        //当アプリ以外によるファイルを読み取る権限の許可ダイアログで「今後、表示しない」を選択中の場合
                    } else {

                        //当アプリ以外によるファイルを読み取る権限の許可が無いので、当アプリを実行できない事を説明して、処理を終了
                        AlertDialog.Builder(this)
                            .setMessage(getString(R.string.denied_read_external_storage))
                            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            }
                            .create()
                            .show()

                        return
                    }
                }
            }


        } catch (exception: Exception) {
            Toast.makeText(applicationContext, exception.toString(), Toast.LENGTH_LONG).show()
            throw exception
        }
    }
}
