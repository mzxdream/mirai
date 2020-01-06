@file:Suppress("EXPERIMENTAL_API_USAGE")

package net.mamoe.mirai.demo

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.core.readBytes
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.QQ
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.GroupMessage
import net.mamoe.mirai.timpc.TIMPC
import net.mamoe.mirai.utils.LoginFailedException
import java.lang.ref.WeakReference
import java.security.acl.Group
import java.text.DateFormat
import java.util.Date;
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import java.util.*
import java.util.regex.Pattern
import java.util.regex.Matcher
import kotlin.concurrent.timerTask

class MiraiService : Service() {

    private lateinit var mCaptchaDeferred: CompletableDeferred<String>

    private lateinit var mBot: Bot

    private var mCaptcha = ""
        set(value) {
            field = value
            mCaptchaDeferred.complete(value)
        }

    private var mBinder: MiraiBinder? = null

    private var mCallback: WeakReference<LoginCallback>? = null

    override fun onCreate() {
        super.onCreate()
        mBinder = MiraiBinder()

    }

    private fun login(qq: Long, pwd: String) {
        GlobalScope.launch {
            mBot = TIMPC.Bot(qq, pwd) {
                captchaSolver = {
                    val bytes = it.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    mCaptchaDeferred = CompletableDeferred()
                    mCallback?.get()?.onCaptcha(bitmap)
                    mCaptchaDeferred.await()
                }
            }.apply {
                try {
                    login()
                    mCallback?.get()?.onSuccess()
                } catch (e: LoginFailedException) {
                    mCallback?.get()?.onFailed()
                }
            }


            mBot.subscribeMessages {
                always {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                    val sId : Long
                    val sName : String
                    if (this is GroupMessage) {
                        sId = senderId
                        sName = "$senderName@$groupNumber"
                    } else {
                        sId = sender!!.id
                        sName = bot.getQQ(sId).queryProfile().nickname
                    }
                    mCallback?.get()?.onMessage("[$date]到来自${sName}(${sId})的消息:${message}")
                    val msg: String = message.toString()
                    val pattern: Pattern  = Pattern.compile("群内回复[a-zA-Z0-9 \\u0080-\\u9fff](.+)[a-zA-Z0-9 \\u0080-\\u9fff]进行报名")
                    val matcher: Matcher  = pattern.matcher(msg)
                    if (matcher.find()) {
                        val rand = Random()
                        val num = rand.nextInt(15000).toLong()
                        delay(num)
                        val replyMsg = matcher.group(1)
                        if (replyMsg != null) {
                            reply(replyMsg)
                            val delayDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                            mCallback?.get()?.onMessage("[$delayDate]延迟${num}ms回复来自${sName}(${sId})的消息:$replyMsg")
                        } else {
                            mCallback?.get()?.onMessage("解析${message}失败")
                        }
                    }
                }
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }


    inner class MiraiBinder : Binder() {

        fun startLogin(qq: Long, pwd: String) {
            login(qq, pwd)
        }

        fun setCaptcha(captcha: String) {
            mCaptcha = captcha
        }

        fun setCallback(callback: LoginCallback) {
            mCallback = WeakReference(callback)
        }
    }


}