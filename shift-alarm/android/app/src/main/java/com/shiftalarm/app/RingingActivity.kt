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
   val prefs=getSharedPreferences("alarm_options",0)
   val difficulty=prefs.getInt("math_difficulty",0).coerceIn(0,2)
   val configuredMode=prefs.getInt("math_mode",0).coerceIn(0,2)
   val multipleChoice=if(configuredMode==2) Random.nextBoolean() else configuredMode==0
   val limit=when(difficulty){0->10;1->25;else->60}
   val a=Random.nextInt(2,limit+1)
   val b=Random.nextInt(2,limit+1)
   val operation=when(difficulty){0->Random.nextInt(2);else->Random.nextInt(3)}
   val answer=when(operation){0->a+b;1->a*b;else->a-b}
   val symbol=when(operation){0->"+";1->"×";else->"−"}
   val problem=TextView(this).apply{
    text="문제를 풀면 알람이 꺼집니다.\\n$a $symbol $b = ?"
    textSize=22f;setTextColor(Color.WHITE);gravity=Gravity.CENTER
   }
   bg.addView(problem)
   if(multipleChoice){
    val options=mutableSetOf(answer)
    while(options.size<4){
     val delta=Random.nextInt(1,12)*(if(Random.nextBoolean())1 else -1)
     options.add(answer+delta)
    }
    val grid=GridLayout(this).apply{columnCount=2}
    for(value in options.shuffled()){
     val button=Button(this).apply{text=value.toString();textSize=21f}
     button.setOnClickListener{
      if(value==answer){stop();finish()}
      else Toast.makeText(this,"오답이에요. 다시 선택하세요.",Toast.LENGTH_SHORT).show()
     }
     grid.addView(button,GridLayout.LayoutParams().apply{width=0;height=150;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})
    }
    bg.addView(grid,LinearLayout.LayoutParams(-1,-2))
   }else{
    val field=EditText(this).apply{
     hint="정답 입력";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
     setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);gravity=Gravity.CENTER
    }
    val submit=Button(this).apply{text="정답 확인 · 알람 종료";textSize=18f}
    submit.setOnClickListener{
     if(field.text.toString().toIntOrNull()==answer){stop();finish()}
     else {field.error="정답이 아니에요";field.selectAll()}
    }
    bg.addView(field,LinearLayout.LayoutParams(-1,-2))
    bg.addView(submit,LinearLayout.LayoutParams(-1,160))
   }
  }else{
   val dismiss=Button(this).apply{text="알람 종료";textSize=20f;setOnClickListener{stop();finish()}}
   bg.addView(dismiss,LinearLayout.LayoutParams(-1,160))
  }
  setContentView(bg)
 }
 private fun stop(){stopService(Intent(this,AlarmService::class.java))}
}
