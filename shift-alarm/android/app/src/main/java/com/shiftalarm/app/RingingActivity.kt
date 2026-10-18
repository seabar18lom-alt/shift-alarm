package com.shiftalarm.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import kotlin.random.Random

class RingingActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  setShowWhenLocked(true)
  setTurnScreenOn(true)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  val bg=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER
   setBackgroundColor(Color.rgb(24,44,89))
   setPadding(32,32,32,32)
  }
  val h=TextView(this).apply{text="⏰  교대알람";textSize=21f;setTextColor(Color.rgb(174,200,255));gravity=Gravity.CENTER}
  val title=TextView(this).apply{
   text=intent.getStringExtra("title")?:"근무 알람"
   textSize=33f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)
   gravity=Gravity.CENTER;setPadding(0,30,0,50)
  }
  bg.addView(h)
  bg.addView(title)
  if(intent.getBooleanExtra("challenge",false)){
   val a=Random.nextInt(2,20)
   val b=Random.nextInt(2,20)
   val multiply=Random.nextBoolean()
   val answer=if(multiply)a*b else a+b
   val problem=TextView(this).apply{
    text="문제를 풀면 알람이 꺼집니다.\n$a ${if(multiply)"×" else "+"} $b = ?"
    textSize=22f;setTextColor(Color.WHITE);gravity=Gravity.CENTER
   }
   val field=EditText(this).apply{
    hint="정답 입력";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
    setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.CENTER
   }
   val submit=Button(this).apply{text="정답 확인 · 알람 종료";textSize=18f}
   submit.setOnClickListener{
    if(field.text.toString().toIntOrNull()==answer){stop();finish()}
    else {field.error="정답이 아니에요";field.selectAll()}
   }
   bg.addView(problem)
   bg.addView(field,LinearLayout.LayoutParams(-1,-2))
   bg.addView(submit,LinearLayout.LayoutParams(-1,160))
  }else{
   val dismiss=Button(this).apply{text="알람 종료";textSize=20f;setOnClickListener{stop();finish()}}
   bg.addView(dismiss,LinearLayout.LayoutParams(-1,160))
  }
  setContentView(bg)
 }
 private fun stop(){stopService(Intent(this,AlarmService::class.java))}
}
