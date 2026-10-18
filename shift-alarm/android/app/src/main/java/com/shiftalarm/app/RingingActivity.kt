package com.shiftalarm.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.*
import android.view.*
import android.widget.*

class RingingActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  setShowWhenLocked(true);setTurnScreenOn(true);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  val bg=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(24,44,89));setPadding(32,32,32,32)}
  val h=TextView(this).apply{text="⏰  교대알람";textSize=21f;setTextColor(Color.rgb(174,200,255));gravity=Gravity.CENTER}
  val title=TextView(this).apply{text=intent.getStringExtra("title")?:"근무 알람";textSize=33f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,30,0,50)}
  val dismiss=Button(this).apply{text="알람 종료";textSize=20f;setOnClickListener{stop();finish()}}
  bg.addView(h);bg.addView(title);bg.addView(dismiss,LinearLayout.LayoutParams(-1,64));setContentView(bg)

 }
 private fun stop(){stopService(Intent(this,AlarmService::class.java))}
}
