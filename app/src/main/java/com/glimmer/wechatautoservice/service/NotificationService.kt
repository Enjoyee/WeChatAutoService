package com.glimmer.wechatautoservice.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.glimmer.wechatautoservice.R
import com.glimmer.wechatautoservice.activity.MainActivity
import java.util.*

/**
 * @author Glimmer
 * 2019/01/28
 */
class NotificationService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val builder = NotificationCompat.Builder(baseContext, channelId)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon((Objects.requireNonNull<Drawable>(ContextCompat.getDrawable(baseContext, R.mipmap.ic_launcher)) as BitmapDrawable).bitmap)
                .setContentText("抢红包服务正在运行中")
                .setContentTitle("红包服务")

        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)

        startForeground(9999, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "weChatAutoService"
        val channelName = "抢红包服务"
        val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
        )
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

}