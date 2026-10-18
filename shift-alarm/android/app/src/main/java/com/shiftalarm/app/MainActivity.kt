package com.shiftalarm.app

import android.Manifest
import android.app.*
import android.content.*
import android.os.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.*
import android.widget.*
import org.json.*
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity:Activity(){
 private var preview:android.media.MediaPlayer?=null
 private val navy=Color.rgb(24,44,89);private val blue=Color.rgb(53,104,232);private val gray=Color.rgb(104,120,145);private val bg=Color.rgb(246,248,252)
 private lateinit var body:LinearLayout;private var selected=LocalDate.now();private var month=YearMonth.now()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun shape(color:Int,r:Int=14)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
 private fun text(s:String,size:Int=15,bold:Boolean=false,color:Int=navy)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)typeface=Typeface.DEFAULT_BOLD}
 private fun btn(s:String,action:()->Unit)=Button(this).apply{text=s;setAllCaps(false);setOnClickListener{action()}}
 private fun layout(vertical:Boolean=true)=LinearLayout(this).apply{orientation=if(vertical)1 else 0}
 private fun inset(l:Int=0,t:Int=0,r:Int=0,b:Int=0)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
 private fun card()=layout().apply{background=shape(Color.WHITE,20);setPadding(dp(17),dp(17),dp(17),dp(17))}
 override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=bg;window.navigationBarColor=bg;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),5)
  Engine.schedule(this);showHome()
 }
 override fun onResume(){super.onResume();if(::body.isInitialized)showHome()}
 private fun init(title:String){val shell=layout();shell.setBackgroundColor(bg);val scroll=ScrollView(this).apply{isFillViewport=true};body=layout();body.setPadding(dp(18),dp(22),dp(18),dp(30));scroll.addView(body);shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val nav=layout(false).apply{setPadding(dp(12),0,dp(12),dp(12));setBackgroundColor(Color.WHITE)}
  for((name,action) in listOf("▦  근무표" to {showHome()},"◷  알람 규칙" to {showRules()},"⚙  설정" to {showSettings()})){nav.addView(btn(name,action),LinearLayout.LayoutParams(0,dp(54),1f))}
  shell.addView(nav);setContentView(shell);body.addView(text("교대알람.",24,true),inset(0,0,0,22));if(title!="")body.addView(text(title,20,true),inset(0,0,0,13))
 }
 private fun showHome(){init("");val j=Engine.data(this);val current=Engine.shift(j,LocalDate.now());val hero=layout().apply{background=shape(navy,22);setPadding(dp(20),dp(23),dp(20),dp(23))}
  hero.addView(text("TODAY'S SHIFT",12,true,Color.rgb(160,183,236)))
  hero.addView(text("$current 근무",29,true,Color.WHITE),inset(0,9,0,5))
  hero.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE")),13,false,Color.rgb(190,205,232)))
  val upcoming=Engine.events(j,LocalDate.now()).filter{it.time.isAfter(ZonedDateTime.now())}.firstOrNull()
  hero.addView(text("다음 알람",12,false,Color.rgb(160,183,236)),inset(0,24,0,3))
  hero.addView(text(if(upcoming!=null)"${Engine.time(upcoming.time)}  ·  ${upcoming.name}" else "예정 없음",19,true,Color.WHITE))
  body.addView(hero,inset(0,0,0,26))
  val quick=card()
  quick.addView(text("빠른 알람",19,true),inset(0,0,0,8))
  val quickPrefs=getSharedPreferences("quick_alarm",0)
  var minutes=quickPrefs.getInt("selected_minutes",15).coerceIn(5,1440)
  minutes=((minutes+4)/5)*5
  fun quickLabel(value:Int):String=if(value<60) "${value}분" else if(value%60==0) "${value/60}시간" else "${value/60}시간 ${value%60}분"
  val quickRow=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
  val display=btn(quickLabel(minutes)){}
  fun updateQuick(value:Int){
   minutes=value.coerceIn(5,1440)
   quickPrefs.edit().putInt("selected_minutes",minutes).apply()
   display.text=quickLabel(minutes)+"  ▾"
  }
  val down=btn("−"){updateQuick(minutes-5)}
  val up=btn("+"){updateQuick(minutes+5)}
  display.setOnClickListener{
   val values=(1..288).map{it*5}
   AlertDialog.Builder(this).setTitle("빠른 알람 시간 선택")
    .setSingleChoiceItems(values.map{quickLabel(it)}.toTypedArray(),(minutes/5)-1){dialog,which->
     updateQuick(values[which]);dialog.dismiss()
    }.setNegativeButton("취소",null).show()
  }
  updateQuick(minutes)
  quickRow.addView(down,LinearLayout.LayoutParams(dp(58),dp(52)))
  quickRow.addView(display,LinearLayout.LayoutParams(0,dp(52),1f))
  quickRow.addView(up,LinearLayout.LayoutParams(dp(58),dp(52)))
  quick.addView(quickRow)
  quick.addView(btn("이 시간으로 알람 설정"){setQuickAlarm(minutes)},inset(0,10,0,0))
  val quickAt=quickPrefs.getLong("time",0L)
  if(quickAt>System.currentTimeMillis()){
   quick.addView(text("예약됨: "+java.time.Instant.ofEpochMilli(quickAt).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M월 d일 HH:mm")),13,false,gray),inset(0,10,0,4))
   quick.addView(btn("빠른 알람 취소"){cancelQuickAlarm();showHome()})
  }
  body.addView(quick,inset(0,0,0,20))
  val header=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};header.addView(text("근무 캘린더",19,true),LinearLayout.LayoutParams(0,-2,1f));header.addView(btn("패턴 설정") {patternDialog()});body.addView(header,inset(0,0,0,10))
  val cal=card()
  val bar=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
  bar.addView(btn("‹"){month=month.minusMonths(1);showHome()})
  bar.addView(text(month.format(DateTimeFormatter.ofPattern("yyyy년 M월")),18,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f))
  bar.addView(btn("›"){month=month.plusMonths(1);showHome()})
  cal.addView(bar)
  val colors=intArrayOf(Color.rgb(75,146,214),Color.rgb(148,113,199),Color.rgb(89,179,125),Color.rgb(238,164,89),Color.rgb(208,112,145),Color.rgb(92,164,174))
  val known=Engine.shiftTypes(j).filter{it!="휴무"}
  fun shiftColor(name:String):Int=if(name=="휴무")Color.rgb(152,160,173) else colors[(known.indexOf(name).coerceAtLeast(0))%colors.size]
  val counts=linkedMapOf<String,Int>()
  for(dayOfMonth in 1..month.lengthOfMonth()){
   val shiftName=Engine.shift(j,month.atDay(dayOfMonth))
   counts[shiftName]=(counts[shiftName]?:0)+1
  }
  val legend=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
  for((name,count) in counts){
   val segment=text("● $name $count  ",11,true,shiftColor(name))
   legend.addView(segment)
  }
  val legendScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(legend)}
  cal.addView(legendScroll,inset(0,10,0,10))
  val grid=GridLayout(this).apply{columnCount=7}
  listOf("일","월","화","수","목","금","토").forEachIndexed{index,dayName->
   val color=if(index==0)Color.rgb(210,93,93) else if(index==6)blue else gray
   grid.addView(text(dayName,12,true,color).apply{gravity=Gravity.CENTER},
    GridLayout.LayoutParams().apply{width=0;height=dp(34);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})
  }
  val first=month.atDay(1).dayOfWeek.value%7
  val slots=((month.lengthOfMonth()+first+6)/7)*7
  for(i in 0 until slots){
   val date=month.atDay(1).plusDays((i-first).toLong())
   val inside=date.monthValue==month.monthValue
   val shiftName=Engine.shift(j,date)
   val cell=layout().apply{
    gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL
    val baseColor=if(date==selected)Color.rgb(231,240,255) else if(inside)Color.WHITE else Color.rgb(246,247,249)
    background=shape(baseColor,9)
    setPadding(dp(1),dp(5),dp(1),dp(4))
    setOnClickListener{selected=date;shiftDialog(date)}
   }
   val dayColor=if(!inside)Color.LTGRAY else if(i%7==0)Color.rgb(210,93,93) else if(i%7==6)blue else navy
   cell.addView(text(date.dayOfMonth.toString(),12,date==LocalDate.now(),dayColor).apply{gravity=Gravity.CENTER})
   if(inside){
    val label=text(shiftName,10,true,Color.WHITE).apply{
     gravity=Gravity.CENTER
     maxLines=2
     background=shape(shiftColor(shiftName),24)
     setPadding(dp(1),dp(5),dp(1),dp(5))
    }
    cell.addView(label,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(6),0,0)})
    if(date==LocalDate.now())cell.addView(text("오늘",9,true,blue).apply{gravity=Gravity.CENTER})
   }
   grid.addView(cell,GridLayout.LayoutParams().apply{
    width=0;height=dp(85)
    columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
    setMargins(dp(1),dp(1),dp(1),dp(1))
   })
  }
  cal.addView(grid)
  body.addView(cal,inset(0,0,0,25))
  val row=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(text("${selected.monthValue}월 ${selected.dayOfMonth}일 알람",19,true),LinearLayout.LayoutParams(0,-2,1f));row.addView(btn("규칙 관리") {showRules()});body.addView(row,inset(0,0,0,10))
  val list=card();val events=Engine.events(j,selected);if(events.isEmpty())list.addView(text("등록된 알람이 없어요.",13,false,gray))
  for(e in events){val line=layout(false).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(9),0,dp(9))};line.addView(text("◷",23,true,blue),LinearLayout.LayoutParams(dp(43),-2));line.addView(text(e.name,15,true),LinearLayout.LayoutParams(0,-2,1f));line.addView(text(Engine.time(e.time),15,true));list.addView(line)}
  body.addView(list)
 }
 private fun quickIntent():PendingIntent {
  val intent=Intent(this,AlarmReceiver::class.java).setAction("com.shiftalarm.app.QUICK_ALARM")
   .putExtra("title","빠른 알람")
  return PendingIntent.getBroadcast(this,991711,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 }
 private fun cancelQuickAlarm(){
  getSystemService(AlarmManager::class.java).cancel(quickIntent())
  getSharedPreferences("quick_alarm",0).edit().remove("time").apply()
 }
 private fun setQuickAlarm(minutes:Int){
  cancelQuickAlarm()
  val at=System.currentTimeMillis()+minutes*60_000L
  val alarm=getSystemService(AlarmManager::class.java)
  try{
   if(Build.VERSION.SDK_INT>=31&&!alarm.canScheduleExactAlarms()){
    Toast.makeText(this,"정확한 알람 권한을 허용한 뒤 다시 설정해 주세요",Toast.LENGTH_LONG).show()
    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:$packageName")))
    return
   }
   alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,quickIntent())
   getSharedPreferences("quick_alarm",0).edit().putLong("time",at).apply()
   Toast.makeText(this,"$minutes 분 후 알람을 예약했어요",Toast.LENGTH_SHORT).show()
   showHome()
  }catch(_:Exception){Toast.makeText(this,"알람을 예약하지 못했어요. 권한을 확인해 주세요",Toast.LENGTH_LONG).show()}
 }
 private fun shiftDialog(date:LocalDate){val choices=Engine.shiftTypes(Engine.data(this)).toTypedArray();val current=Engine.shift(Engine.data(this),date);AlertDialog.Builder(this).setTitle("${date} 근무 지정").setSingleChoiceItems(choices,choices.indexOf(current)){dialog,index->val j=Engine.data(this);j.getJSONObject("overrides").put(date.toString(),choices[index]);Engine.save(this,j);Engine.schedule(this);dialog.dismiss();showHome()}.setNeutralButton("반복 패턴 복원"){_,_->val j=Engine.data(this);j.getJSONObject("overrides").remove(date.toString());Engine.save(this,j);Engine.schedule(this);showHome()}.setNegativeButton("취소",null).show()}
 private fun patternDialog(){val j=Engine.data(this);val view=layout().apply{setPadding(dp(20),dp(12),dp(20),0)};val anchor=EditText(this).apply{setText(j.getString("anchor"));hint="2026-10-18"};val pattern=EditText(this).apply{setText((0 until j.getJSONArray("pattern").length()).joinToString(", "){j.getJSONArray("pattern").getString(it)});hint="예: 데이, 이브닝, 나이트, 휴무"};view.addView(text("기준 날짜 (YYYY-MM-DD)",13));view.addView(anchor);view.addView(text("반복 순서 (쉼표 구분)",13));view.addView(pattern)
  AlertDialog.Builder(this).setTitle("근무 반복 패턴").setView(view).setNegativeButton("취소",null).setPositiveButton("저장"){_,_->try{LocalDate.parse(anchor.text.toString());val arr=pattern.text.toString().split(",").map{it.trim()};require(arr.isNotEmpty()&&arr.all{it in Engine.shiftTypes(j)});j.put("anchor",anchor.text.toString());j.put("pattern",JSONArray(arr));Engine.save(this,j);Engine.schedule(this);showHome()}catch(_:Exception){Toast.makeText(this,"날짜와 근무 이름을 확인해 주세요",Toast.LENGTH_LONG).show()}}.show()
 }
 private fun showRules(){init("알람 규칙");body.addView(btn("+ 새 알람 규칙") {ruleDialog(null)},inset(0,0,0,14));body.addView(text("고정 시각 또는 근무 시작 기준으로 알람을 지정합니다. 야간 근무처럼 자정을 넘기는 일정도 지원해요.",13,false,gray),inset(0,0,0,15))
  val arr=Engine.data(this).getJSONArray("rules");for(i in 0 until arr.length()){val r=arr.getJSONObject(i);val item=card();val line=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};val name=layout();name.addView(text(r.getString("name"),16,true));val desc="${r.getString("shift")} · "+if(r.getString("mode")=="fixed")(when(r.getInt("minute")/1440){0->"당일 ";1->"다음 날 ";else->"이틀 뒤 "}+"%02d:%02d".format(Math.floorMod(r.getInt("minute"),1440)/60,Math.floorMod(r.getInt("minute"),60))) else "근무 시작 기준 ${r.getInt("minute")}분";name.addView(text(desc,12,false,gray),inset(0,5,0,0));line.addView(name,LinearLayout.LayoutParams(0,-2,1f));val check=Switch(this@MainActivity).apply{isChecked=r.optBoolean("enabled",true);setOnCheckedChangeListener{_,on->val j=Engine.data(this@MainActivity);val rules=j.getJSONArray("rules");for(k in 0 until rules.length())if(rules.getJSONObject(k).getLong("id")==r.getLong("id"))rules.getJSONObject(k).put("enabled",on);Engine.save(this@MainActivity,j);Engine.schedule(this@MainActivity)}};line.addView(check);item.addView(line);item.setOnClickListener{ruleDialog(r.getLong("id"))};body.addView(item,inset(0,0,0,10))}
 }
 private fun ruleDialog(id:Long?){
  val j=Engine.data(this);val arr=j.getJSONArray("rules")
  var original:JSONObject?=null
  for(i in 0 until arr.length())if(arr.getJSONObject(i).getLong("id")==id)original=arr.getJSONObject(i)
  val root=layout().apply{setPadding(dp(18),dp(8),dp(18),0)}
  val name=EditText(this).apply{hint="예: 순찰";setText(original?.optString("name")?:"")}
  val shift=Spinner(this).apply{
   adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,Engine.shiftTypes(j))
   setSelection(Engine.shiftTypes(j).indexOf(original?.optString("shift")?:"휴무").coerceAtLeast(0))
  }
  val mode=Spinner(this).apply{
   adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("고정 시각", "근무 시작 기준"))
   setSelection(if(original==null||original?.optString("mode")=="fixed")0 else 1)
  }
  fun field(label:String,control:View){root.addView(text(label,13,true,gray),inset(0,13,0,2));root.addView(control)}
  field("이름",name);field("적용할 근무 (주간·야간 각각 등록)",shift);field("알람 시간 방식",mode)
  val stored=original?.optInt("minute")?:540
  val initial=if(original?.optString("mode")=="fixed")stored else 540
  var hour=Math.floorMod(initial,1440)/60
  var minute=Math.floorMod(initial,1440)%60
  val fixedBox=layout()
  val clock=Button(this).apply{
   text="%02d:%02d".format(hour,minute)
   setOnClickListener{
    TimePickerDialog(this@MainActivity,{_,h,m->hour=h;minute=m;text="%02d:%02d".format(h,m)},hour,minute,true).show()
   }
  }
  val day=Spinner(this).apply{
   adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("근무 당일", "다음 날", "이틀 뒤"))
   setSelection((initial/1440).coerceIn(0,2))
  }
  fixedBox.addView(text("울릴 시각",13,true,gray),inset(0,13,0,2));fixedBox.addView(clock)
  fixedBox.addView(text("근무 날짜 기준",13,true,gray),inset(0,10,0,2));fixedBox.addView(day)
  fixedBox.addView(text("예: 야간 근무 다음 날 02:30 순찰 → 야간 / 02:30 / 다음 날",12,false,gray),inset(0,8,0,0))
  root.addView(fixedBox)
  val relativeBox=layout()
  val offset=EditText(this).apply{
   inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
   setText((if(original?.optString("mode")=="relative")stored else 120).toString())
  }
  relativeBox.addView(text("근무 시작에서 몇 분 전·후",13,true,gray),inset(0,13,0,2));relativeBox.addView(offset)
  relativeBox.addView(text("예: -30 = 30분 전 / 120 = 2시간 후",12,false,gray))
  root.addView(relativeBox)
  fun updateMode(){fixedBox.visibility=if(mode.selectedItemPosition==0)View.VISIBLE else View.GONE;relativeBox.visibility=if(mode.selectedItemPosition==1)View.VISIBLE else View.GONE}
  mode.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
   override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,itemId:Long){updateMode()}
   override fun onNothingSelected(parent:AdapterView<*>?){}
  }
  updateMode()
  val scroll=ScrollView(this).apply{addView(root)}
  val builder=AlertDialog.Builder(this).setTitle(if(id==null)"새 규칙" else "규칙 편집").setView(scroll).setNegativeButton("취소",null).setPositiveButton("저장",null)
  if(id!=null)builder.setNeutralButton("삭제"){_,_->
   val out=JSONArray();for(i in 0 until arr.length())if(arr.getJSONObject(i).getLong("id")!=id)out.put(arr.getJSONObject(i))
   j.put("rules",out);Engine.save(this,j);Engine.schedule(this);showRules()
  }
  val dlg=builder.create()
  dlg.setOnShowListener{
   dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
    try{
     val str=name.text.toString().trim();require(str.isNotBlank())
     val fixed=mode.selectedItemPosition==0
     val min=if(fixed)day.selectedItemPosition*1440+hour*60+minute else offset.text.toString().toInt()
     require(min in -2880..2880)
     val r=original?:JSONObject().put("id",System.nanoTime()).put("enabled",true)
     r.put("name",str).put("shift",Engine.shiftTypes(j)[shift.selectedItemPosition]).put("mode",if(fixed)"fixed" else "relative").put("minute",min)
     if(original==null)arr.put(r)
     Engine.save(this,j);Engine.schedule(this);dlg.dismiss();showRules()
    }catch(_:Exception){Toast.makeText(this,"이름과 시간을 확인해 주세요 (근무 기준 전후 48시간 이내)",Toast.LENGTH_LONG).show()}
   }
  }
  dlg.show()
 }
 private fun shiftTimeDialog(){
  val j=Engine.data(this)
  val root=layout().apply{setPadding(dp(18),dp(10),dp(18),dp(10))}
  val times=JSONObject((j.optJSONObject("shiftTimes")?:JSONObject()).toString())
  fun saveTimes(){
   j.put("shiftTimes",times)
   Engine.save(this,j)
   Engine.schedule(this)
  }
  val names=Engine.shiftTypes(j).filter{it!="휴무"}
  if(names.isEmpty())root.addView(text("등록된 근무 유형이 없어요. 아래에서 자유롭게 추가하세요.",14,false,gray))
  for(shiftName in names){
   val line=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
   line.addView(text(shiftName,15,true),LinearLayout.LayoutParams(0,-2,1f))
   var total=times.optInt(shiftName,540)
   val timeButton=btn("%02d:%02d".format(total/60,total%60)){
    TimePickerDialog(this@MainActivity,{_,h,m->
     total=h*60+m
     times.put(shiftName,total)
     saveTimes()
     shiftTimeDialog()
    },total/60,total%60,true).show()
   }
   line.addView(timeButton)
   line.addView(btn("삭제"){
    AlertDialog.Builder(this).setTitle("$shiftName 삭제")
     .setMessage("해당 근무 유형과 연결된 알람 규칙 및 근무표 항목도 휴무로 변경됩니다. 계속할까요?")
     .setNegativeButton("취소",null)
     .setPositiveButton("삭제"){_,_->
      val arr=JSONArray()
      for(n in Engine.shiftTypes(j))if(n!="휴무"&&n!=shiftName)arr.put(n)
      j.put("customShifts",arr)
      j.optJSONObject("shiftTimes")?.remove(shiftName)
      val pattern=j.getJSONArray("pattern")
      for(k in 0 until pattern.length())if(pattern.getString(k)==shiftName)pattern.put(k,"휴무")
      val overrides=j.getJSONObject("overrides")
      val keys=overrides.keys()
      while(keys.hasNext()){val key=keys.next();if(overrides.optString(key)==shiftName)overrides.put(key,"휴무")}
      val rules=j.getJSONArray("rules")
      for(k in 0 until rules.length())if(rules.getJSONObject(k).optString("shift")==shiftName)rules.getJSONObject(k).put("enabled",false)
      Engine.save(this,j);Engine.schedule(this);showSettings()
     }.show()
   })
   root.addView(line,inset(0,5,0,5))
  }
  root.addView(btn("+ 근무 유형 추가"){
   val entry=EditText(this).apply{hint="예: 데이, 이브닝, 나이트"}
   AlertDialog.Builder(this).setTitle("근무 유형 추가").setView(entry)
    .setNegativeButton("취소",null)
    .setPositiveButton("추가"){_,_->
     val name=entry.text.toString().trim()
     if(name.isNotBlank()&&name.length<=20&&name !in Engine.shiftTypes(j)){
      val arr=j.optJSONArray("customShifts")?:JSONArray()
      arr.put(name);j.put("customShifts",arr)
      times.put(name,540)
      saveTimes()
      showSettings()
      shiftTimeDialog()
     }else Toast.makeText(this,"중복되지 않는 이름을 입력해 주세요 (20자 이하)",Toast.LENGTH_LONG).show()
    }.show()
  },inset(0,12,0,0))
  AlertDialog.Builder(this).setTitle("근무 유형 및 시작 시간")
   .setView(ScrollView(this).apply{addView(root)})
   .setPositiveButton("닫기"){_,_->showSettings()}.show()
 }
 private fun stopPreview(){preview?.release();preview=null}
 override fun onPause(){stopPreview();super.onPause()}
 private fun pickRingtone(){
  stopPreview()
  val intent=Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
   .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,android.media.RingtoneManager.TYPE_ALARM)
   .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false)
   .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true)
   .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,AlarmAudio.uri(this))
  try{startActivityForResult(intent,81)}catch(_:Exception){Toast.makeText(this,"기본 알람음 선택 화면을 열 수 없어요. 음악 파일을 선택해 주세요.",Toast.LENGTH_LONG).show()}
 }
 private fun pickMusic(){
  stopPreview()
  val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*")
   .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
  try{startActivityForResult(intent,82)}catch(_:Exception){Toast.makeText(this,"음악 파일 선택 화면을 열 수 없어요",Toast.LENGTH_LONG).show()}
 }
 @Deprecated("Activity result compatibility")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=RESULT_OK)return
  if(requestCode==81){
   @Suppress("DEPRECATION")
   val uri=data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?:return
   val title=android.media.RingtoneManager.getRingtone(this,uri)?.getTitle(this)?:"선택한 알람음"
   AlarmAudio.prefs(this).edit().putString("uri",uri.toString()).putString("title",title).apply();showSettings()
  }else if(requestCode==83){
   val uri=data?.data?:return
   try{previewShiftPdf(ShiftPdfImport.parse(this,uri))}
   catch(e:Exception){AlertDialog.Builder(this).setTitle("근무표를 읽지 못했어요")
    .setMessage((e.message?:"이 PDF 형식은 아직 지원되지 않아요")+"\n텍스트가 들어 있는 표형 PDF부터 지원합니다.")
    .setPositiveButton("확인",null).show()}
  }else if(requestCode==82){
   val uri=data?.data?:return
   try{
    contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
    var title="내 음악 파일"
    contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cursor->if(cursor.moveToFirst())title=cursor.getString(0)}
    AlarmAudio.prefs(this).edit().putString("uri",uri.toString()).putString("title",title).apply();showSettings()
   }catch(_:Exception){Toast.makeText(this,"파일 접근 권한을 저장하지 못했어요. 다른 파일을 선택해 주세요.",Toast.LENGTH_LONG).show()}
  }
 }
 private fun audioSettings():LinearLayout{
  val prefs=AlarmAudio.prefs(this);val box=card()
  box.addView(text("알람 소리와 진동",19,true))
  box.addView(text(prefs.getString("title","휴대폰 기본 알람음")?:"휴대폰 기본 알람음",14,false,gray),inset(0,8,0,12))
  box.addView(btn("휴대폰 알람음 선택"){pickRingtone()})
  box.addView(btn("MP3 · 내 음악 파일 선택"){pickMusic()})
  val volumeLabel=text("알람 음량 ${prefs.getInt("volume",80)}%",14,true)
  box.addView(volumeLabel,inset(0,14,0,3))
  val slider=SeekBar(this).apply{max=100;progress=prefs.getInt("volume",80)}
  slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
   override fun onProgressChanged(bar:SeekBar?,value:Int,fromUser:Boolean){
    volumeLabel.text="알람 음량 $value%";prefs.edit().putInt("volume",value).apply();preview?.setVolume(value/100f,value/100f)
   }
   override fun onStartTrackingTouch(bar:SeekBar?){}
   override fun onStopTrackingTouch(bar:SeekBar?){}
  })
  box.addView(slider)
  box.addView(text("휴대폰의 시스템 알람 음량에도 영향을 받아요.",12,false,gray))
  box.addView(btn("휴대폰 알람 음량 조절"){
   getSystemService(android.media.AudioManager::class.java).adjustStreamVolume(android.media.AudioManager.STREAM_ALARM,android.media.AudioManager.ADJUST_SAME,android.media.AudioManager.FLAG_SHOW_UI)
  })
  val vibration=Switch(this).apply{text="진동 사용";isChecked=prefs.getBoolean("vibrate",true)}
  vibration.setOnCheckedChangeListener{_,enabled->prefs.edit().putBoolean("vibrate",enabled).apply()}
  box.addView(vibration,inset(0,12,0,8))
  val row=layout(false)
  row.addView(btn("소리 미리 듣기"){stopPreview();preview=AlarmAudio.play(this);if(preview==null)Toast.makeText(this,"알람음을 재생하지 못했어요",Toast.LENGTH_LONG).show()},LinearLayout.LayoutParams(0,-2,1f))
  row.addView(btn("정지"){stopPreview()},LinearLayout.LayoutParams(0,-2,1f));box.addView(row)
  return box
 }
 private fun showSettings(){
  init("설정")
  fun section(title:String,description:String,action:()->Unit){
   val item=card()
   val line=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
   val labels=layout()
   labels.addView(text(title,18,true))
   labels.addView(text(description,12,false,gray),inset(0,5,0,0))
   line.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
   line.addView(text("›",27,true,blue))
   item.addView(line)
   item.setOnClickListener{action()}
   body.addView(item,inset(0,0,0,12))
  }
  section("알람 소리와 진동","알람음 · 음악 파일 · 음량 · 진동"){showAudioDialog()}
  section("알람 해제 미션","문제 방식 · 난이도 · 적용할 알람"){showChallengeDialog()}
  section("근무표 설정","반복 패턴 · 근무 유형 · 시작 시간"){showWorkSettingsDialog()}
  section("근무표 파일 가져오기","PDF 선택 · 이름 선택 · 미리보기"){pickShiftPdf()}
  section("권한 관리","전체 화면 · 정확한 알람 · 알림 권한"){showPermissionsDialog()}
  section("알람 관리","알람 다시 예약 (확인 후 실행)"){
   AlertDialog.Builder(this).setTitle("알람을 다시 예약할까요?")
    .setMessage("현재 근무표와 알람 규칙을 기준으로 예약된 알람을 갱신합니다. 근무표와 설정은 삭제되지 않습니다.")
    .setNegativeButton("취소",null)
    .setPositiveButton("다시 예약"){_,_->Engine.schedule(this);Toast.makeText(this,"알람 예약을 갱신했습니다.",Toast.LENGTH_LONG).show()}
    .show()
  }
 }
 private fun pickShiftPdf(){
  val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
   .setType("application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  try{startActivityForResult(intent,83)}
  catch(_:Exception){Toast.makeText(this,"PDF 파일 선택 화면을 열지 못했어요",Toast.LENGTH_LONG).show()}
 }
 private fun previewShiftPdf(result:ShiftPdfImport.Result){
  val names=result.names.toTypedArray()
  AlertDialog.Builder(this).setTitle("근무표에서 내 이름 선택")
   .setItems(names){_,index->previewPersonShifts(result,names[index])}
   .setNegativeButton("취소",null).show()
 }
 private fun previewPersonShifts(result:ShiftPdfImport.Result,person:String){
  val parsed=result.assignments.associate{assignment->
   val type=when(person){
    assignment.daytime->"주간"
    assignment.nighttime->"야간"
    else->"휴무"
   }
   assignment.date to type
  }
  val total=parsed.values.groupingBy{it}.eachCount()
  val preview=layout().apply{setPadding(dp(16),dp(8),dp(16),dp(8))}
  preview.addView(text("$person · ${result.year}년 ${result.month}월",17,true))
  preview.addView(text("주간 ${total["주간"]?:0}일 · 야간 ${total["야간"]?:0}일 · 휴무 ${total["휴무"]?:0}일",13,false,gray),inset(0,8,0,9))
  val scroll=ScrollView(this)
  val lines=layout()
  for((day,type) in parsed){
   lines.addView(text("${day.dayOfMonth}일   $type",14,false,when(type){"주간"->blue;"야간"->Color.rgb(118,81,181);else->gray}),inset(0,2,0,2))
  }
  scroll.addView(lines)
  preview.addView(scroll,LinearLayout.LayoutParams(-1,dp(280)))
  preview.addView(text("PDF에서 읽지 못한 날짜는 변경하지 않아요. 적용하기 전에 일정을 반드시 확인하세요.",12,false,gray),inset(0,9,0,0))
  AlertDialog.Builder(this).setTitle("근무표 적용 전 미리보기").setView(preview)
   .setNegativeButton("취소",null)
   .setPositiveButton("이 근무표 적용"){_,_->
    val j=Engine.data(this)
    val existing=Engine.shiftTypes(j)
    val types=j.optJSONArray("customShifts")?:JSONArray()
    for(type in listOf("주간","야간"))if(type !in existing)types.put(type)
    j.put("customShifts",types)
    val times=j.optJSONObject("shiftTimes")?:JSONObject()
    if(!times.has("주간"))times.put("주간",480)
    if(!times.has("야간"))times.put("야간",1200)
    j.put("shiftTimes",times)
    val overrides=j.getJSONObject("overrides")
    for((day,type) in parsed)overrides.put(day.toString(),type)
    Engine.save(this,j);Engine.schedule(this)
    month=YearMonth.of(result.year,result.month)
    selected=parsed.keys.firstOrNull()?:selected
    showHome()
    Toast.makeText(this,"${parsed.size}일 근무표를 반영했어요",Toast.LENGTH_LONG).show()
   }.show()
 }
 private fun showAudioDialog(){
  val scroll=ScrollView(this)
  val root=layout().apply{setPadding(dp(14),dp(8),dp(14),dp(8))}
  root.addView(audioSettings());scroll.addView(root)
  AlertDialog.Builder(this).setTitle("알람 소리와 진동").setView(scroll)
   .setPositiveButton("닫기"){_,_->stopPreview();showSettings()}.show()
 }
 private fun showWorkSettingsDialog(){
  val root=layout().apply{setPadding(dp(14),dp(12),dp(14),dp(12))}
  val dialog=AlertDialog.Builder(this).setTitle("근무표 설정").setView(root).setNegativeButton("닫기",null).create()
  root.addView(btn("반복 근무 패턴 변경"){dialog.dismiss();patternDialog()})
  root.addView(btn("근무 유형 · 시작 시간 관리"){dialog.dismiss();shiftTimeDialog()})
  dialog.show()
 }
 private fun showPermissionsDialog(){
  val root=layout().apply{setPadding(dp(14),dp(12),dp(14),dp(12))}
  root.addView(btn("전체 화면 알람 권한"){
   if(Build.VERSION.SDK_INT>=34)try{
    startActivity(Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").setData(android.net.Uri.parse("package:$packageName")))
   }catch(_:Exception){Toast.makeText(this,"휴대폰 설정에서 전체 화면 알림을 허용해 주세요",Toast.LENGTH_LONG).show()}
   else Toast.makeText(this,"이 기기는 별도 권한이 없을 수 있어요",Toast.LENGTH_SHORT).show()
  })
  root.addView(btn("정확한 알람 권한"){
   if(Build.VERSION.SDK_INT>=31)try{
    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:$packageName")))
   }catch(_:Exception){Toast.makeText(this,"시스템 설정에서 정확한 알람을 허용해 주세요",Toast.LENGTH_LONG).show()}
   else Toast.makeText(this,"별도 권한이 필요하지 않아요",Toast.LENGTH_SHORT).show()
  })
  root.addView(btn("알림 권한"){
   try{startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}
   catch(_:Exception){startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:$packageName")))}
  })
  AlertDialog.Builder(this).setTitle("권한 관리").setView(root).setPositiveButton("닫기",null).show()
 }
 private fun showChallengeDialog(){
  val options=getSharedPreferences("alarm_options",0)
  val root=layout().apply{setPadding(dp(14),dp(8),dp(14),dp(8))}
  val enabled=Switch(this).apply{
   text="문제를 풀어야 알람 종료"
   isChecked=options.getBoolean("math_to_dismiss",false)
   setOnCheckedChangeListener{_,value->options.edit().putBoolean("math_to_dismiss",value).apply()}
  }
  root.addView(enabled)
  fun pick(label:String,key:String,values:List<String>){
   val row=layout(false).apply{gravity=Gravity.CENTER_VERTICAL}
   row.addView(text(label,15,true),LinearLayout.LayoutParams(0,-2,1f))
   val spinner=Spinner(this).apply{
    adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,values)
    setSelection(options.getInt(key,0).coerceIn(0,values.lastIndex))
    onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
     override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){options.edit().putInt(key,position).apply()}
     override fun onNothingSelected(parent:AdapterView<*>?){}
    }
   }
   row.addView(spinner)
   root.addView(row,inset(0,15,0,5))
  }
  pick("정답 방식","math_mode",listOf("객관식","주관식","랜덤"))
  pick("문제 난이도","math_difficulty",listOf("쉬움","보통","어려움"))
  val rules=Engine.data(this).getJSONArray("rules")
  val entries=mutableListOf<Pair<String,String>>()
  for(k in 0 until rules.length()){
   val rule=rules.getJSONObject(k)
   entries.add(rule.getLong("id").toString() to ("${rule.optString("shift")} · ${rule.optString("name")}"))
  }
  entries.add("quick" to "빠른 알람")
  val choose=btn("적용할 알람 선택"){
   val chosen=options.getStringSet("math_alarm_keys",null)?.toMutableSet()
     ?:entries.map{it.first}.toMutableSet()
   val checked=BooleanArray(entries.size){chosen.contains(entries[it].first)}
   AlertDialog.Builder(this).setTitle("미션 적용할 알람 선택")
    .setMultiChoiceItems(entries.map{it.second}.toTypedArray(),checked){_,index,isChecked->
     if(isChecked)chosen.add(entries[index].first) else chosen.remove(entries[index].first)
    }
    .setNegativeButton("취소",null)
    .setPositiveButton("적용"){_,_->options.edit().putStringSet("math_alarm_keys",chosen).apply()}
    .show()
  }
  root.addView(choose,inset(0,17,0,6))
  root.addView(text("선택한 알람에만 문제를 적용합니다. 전체 스위치를 끄면 모든 알람이 일반 종료 방식으로 동작해요.",12,false,gray))
  val scroll=ScrollView(this).apply{addView(root)}
  AlertDialog.Builder(this).setTitle("알람 해제 미션").setView(scroll).setPositiveButton("닫기",null).show()
 }

}
