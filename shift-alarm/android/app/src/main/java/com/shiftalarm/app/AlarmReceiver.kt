package com.shiftalarm.app

import android.app.*
import android.content.*
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class AlarmReceiver:android.content.BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  if(i.action=="com.shiftalarm.app.QUICK_ALARM") c.getSharedPreferences("quick_alarm",0).edit().remove("time").apply()
  val service=Intent(c,AlarmService::class.java).putExtra("title",i.getStringExtra("title")?:"근무 알람")
  service.putExtra("alarm_key",if(i.action=="com.shiftalarm.app.QUICK_ALARM")"quick" else (i.getStringExtra("alarm_key")?:""))
  // Wake a locked screen briefly so the full-screen alarm notification can appear.
  try {
   @Suppress("DEPRECATION")
   val pm=c.getSystemService(PowerManager::class.java)
   @Suppress("DEPRECATION")
   val wake=pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,"shiftalarm:screen")
   wake.acquire(5000L)
  }catch(_:Exception){}
  if(Build.VERSION.SDK_INT>=26)c.startForegroundService(service) else c.startService(service)
 }
}
class BootReceiver:android.content.BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){Engine.schedule(c)}}
