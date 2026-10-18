package com.shiftalarm.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.*

class AlarmService:Service(){
 private var player:MediaPlayer?=null
 private var vibrator:Vibrator?=null
 private var wakeLock:PowerManager.WakeLock?=null
 override fun onBind(intent:Intent?)=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(intent?.action==STOP){stopSelf();return START_NOT_STICKY}
  if(intent==null){stopSelf();return START_NOT_STICKY}
  Engine.channel(this)
  val screen=Intent(this,RingingActivity::class.java).putExtra("title",intent.getStringExtra("title")?:"근무 알람").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  val open=PendingIntent.getActivity(this,0,screen,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stop=PendingIntent.getService(this,1,Intent(this,AlarmService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=Notification.Builder(this,Engine.CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
   .setContentTitle("교대알람 · ${intent.getStringExtra("title")?:"근무 알람"}")
   .setContentText("알람 종료를 누르면 소리가 멈춥니다.").setCategory(Notification.CATEGORY_ALARM)
   .setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true).setContentIntent(open).setFullScreenIntent(open,true)
   .apply { if (!getSharedPreferences("alarm_options",0).getBoolean("math_to_dismiss",false)) addAction(Notification.Action.Builder(null,"알람 종료",stop).build()) }.build()
  if(Build.VERSION.SDK_INT>=29)startForeground(100,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)else startForeground(100,notification)
  if(player==null){
   wakeLock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"shiftalarm:ring").apply{acquire(10*60*1000L)}
   player=AlarmAudio.play(this)
   if(AlarmAudio.prefs(this).getBoolean("vibrate",true))try{
    vibrator=getSystemService(Vibrator::class.java)
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0,500,350),0))
   }catch(_:Exception){}
  }
  return START_NOT_STICKY
 }
 override fun onDestroy(){
  player?.release();player=null;vibrator?.cancel()
  wakeLock?.let{if(it.isHeld)it.release()};wakeLock=null
  stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()
 }
 companion object{const val STOP="com.shiftalarm.app.STOP_ALARM"}
}
