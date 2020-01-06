@file:Suppress("DEPRECATION", "EXPERIMENTAL_API_USAGE")

package net.mamoe.mirai.demo

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity(),LoginCallback {


    private lateinit var progressDialog : ProgressDialog
    private val msgList = mutableListOf<String>()

    override suspend fun onCaptcha(bitmap: Bitmap) {
        withContext(Dispatchers.Main){
            ll_captcha.visibility = View.VISIBLE
            iv_captcha.setImageBitmap(bitmap)
            needCaptcha = true
            if (progressDialog.isShowing){
                progressDialog.dismiss()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override suspend fun onMessage(message:String) {
        withContext(Dispatchers.Main){
            msgList.add(message)
            while (msgList.size > 100) {
                msgList.removeAt(0)
            }
            var content = String()
            for (i in 0 until msgList.size) {
                content = msgList[i] + "\n" + content
            }
            msg.text = "开始抢饭中...\n${content}"
        }
    }

    override suspend fun onSuccess() {
        withContext(Dispatchers.Main){
            Toast.makeText(this@MainActivity,"登录成功",Toast.LENGTH_SHORT).show()
            if (progressDialog.isShowing){
                progressDialog.dismiss()
            }
            ll_captcha.visibility = View.GONE
            et_pwd.visibility = View.GONE
            et_qq.visibility = View.GONE
            bt_login.visibility = View.GONE
            msg.text = "开始抢饭中..."
            msgList.clear()
        }

    }

    override suspend fun onFailed() {
        withContext(Dispatchers.Main){
            Toast.makeText(this@MainActivity,"登录失败",Toast.LENGTH_SHORT).show()
            if (progressDialog.isShowing){
                progressDialog.dismiss()
            }
        }
    }

    var binder: MiraiService.MiraiBinder? = null

    private var needCaptcha = false


    private val conn = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as MiraiService.MiraiBinder?
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, MiraiService::class.java)
        startService(intent)
        bindService(intent, conn, Service.BIND_AUTO_CREATE)
        progressDialog = ProgressDialog(this)
        msg.movementMethod = ScrollingMovementMethod.getInstance()
        bt_login.setOnClickListener {
            if (!progressDialog.isShowing){
                progressDialog.show()
            }
            (this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(this.window.decorView.windowToken, 0)
            binder?.setCallback(this)
            if (!needCaptcha){
                try {
                    val qq = et_qq.text.toString().toLong()
                    val pwd = et_pwd.text.toString()
                    binder?.startLogin(qq, pwd)
                } catch (e : Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }else{
                val captcha = et_captcha.text.toString()
                binder?.setCaptcha(captcha)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(conn)
    }

}