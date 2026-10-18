package com.shiftalarm.app

import android.app.*
import android.content.*
import android.os.Build
import android.provider.Settings

class AlarmReceiver:android.content.BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  if(i.action=="com.shiftalarm.app.QUICK_ALARM") c.getSharedPreferences("quick_alarm",0).edit().remove("time").apply()
  val service=Intent(c,AlarmService::class.java).putExtra("title",i.getStringExtra("title")?:"근무 알람")
  c.startForegroundService(service)
 }
}
class BootReceiver:android.content.BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){Engine.schedule(c)}}
