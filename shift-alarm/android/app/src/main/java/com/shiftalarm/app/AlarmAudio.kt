package com.shiftalarm.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings

object AlarmAudio {
 fun prefs(c:Context)=c.getSharedPreferences("alarm_audio",0)
 fun uri(c:Context):Uri {
  val saved=prefs(c).getString("uri",null)
  return if(saved.isNullOrBlank()) Settings.System.DEFAULT_ALARM_ALERT_URI else Uri.parse(saved)
 }
 fun play(c:Context):MediaPlayer? {
  val volume=prefs(c).getInt("volume",80).coerceIn(0,100)/100f
  fun open(uri:Uri):MediaPlayer? {
   val player=MediaPlayer()
   return try {
    player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
    player.setDataSource(c,uri);player.isLooping=true;player.setVolume(volume,volume);player.prepare();player.start();player
   }catch(_:Exception){player.release();null}
  }
  return open(uri(c))?:open(Settings.System.DEFAULT_ALARM_ALERT_URI)
 }
}
