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
  val header=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};header.addView(text("근무 캘린더",19,true),LinearLayout.LayoutParams(0,-2,1f));header.addView(btn("패턴 설정") {patternDialog()});body.addView(header,inset(0,0,0,10))
  val cal=card();val bar=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};bar.addView(btn("‹"){month=month.minusMonths(1);showHome()});bar.addView(text(month.format(DateTimeFormatter.ofPattern("yyyy년 M월")),16,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f));bar.addView(btn("›"){month=month.plusMonths(1);showHome()});cal.addView(bar)
  val grid=GridLayout(this).apply{columnCount=7};listOf("일","월","화","수","목","금","토").forEach{grid.addView(text(it,12,true,gray).apply{gravity=Gravity.CENTER},GridLayout.LayoutParams().apply{width=0;height=dp(35);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})}
  val first=month.atDay(1).dayOfWeek.value%7;val slots=((month.lengthOfMonth()+first+6)/7)*7
  for(i in 0 until slots){val date=month.atDay(1).plusDays((i-first).toLong());val shift=Engine.shift(j,date)
   val cell=layout().apply{gravity=Gravity.CENTER;background=if(date==selected)shape(Color.rgb(232,240,255),11)else shape(Color.TRANSPARENT,11);setOnClickListener{selected=date;shiftDialog(date)}}
   cell.addView(text(date.dayOfMonth.toString(),12,date==selected,if(date.monthValue==month.monthValue)navy else Color.LTGRAY).apply{gravity=Gravity.CENTER})
   cell.addView(text(if(date.monthValue==month.monthValue)shift else "",10,true,when(shift){"주간"->blue;"야간"->Color.rgb(118,81,181);else->gray}).apply{gravity=Gravity.CENTER},inset(0,4,0,0))
   grid.addView(cell,GridLayout.LayoutParams().apply{width=0;height=dp(54);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(1),dp(1),dp(1),dp(1))})
  };cal.addView(grid);body.addView(cal,inset(0,0,0,25))
  val row=layout(false).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(text("${selected.monthValue}월 ${selected.dayOfMonth}일 알람",19,true),LinearLayout.LayoutParams(0,-2,1f));row.addView(btn("규칙 관리") {showRules()});body.addView(row,inset(0,0,0,10))
  val list=card();val events=Engine.events(j,selected);if(events.isEmpty())list.addView(text("등록된 알람이 없어요.",13,false,gray))
  for(e in events){val line=layout(false).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(9),0,dp(9))};line.addView(text("◷",23,true,blue),LinearLayout.LayoutParams(dp(43),-2));line.addView(text(e.name,15,true),LinearLayout.LayoutParams(0,-2,1f));line.addView(text(Engine.time(e.time),15,true));list.addView(line)}
  body.addView(list)
 }
 private fun shiftDialog(date:LocalDate){val choices=Engine.types.toTypedArray();val current=Engine.shift(Engine.data(this),date);AlertDialog.Builder(this).setTitle("${date} 근무 지정").setSingleChoiceItems(choices,choices.indexOf(current)){dialog,index->val j=Engine.data(this);j.getJSONObject("overrides").put(date.toString(),choices[index]);Engine.save(this,j);Engine.schedule(this);dialog.dismiss();showHome()}.setNeutralButton("반복 패턴 복원"){_,_->val j=Engine.data(this);j.getJSONObject("overrides").remove(date.toString());Engine.save(this,j);Engine.schedule(this);showHome()}.setNegativeButton("취소",null).show()}
 private fun patternDialog(){val j=Engine.data(this);val view=layout().apply{setPadding(dp(20),dp(12),dp(20),0)};val anchor=EditText(this).apply{setText(j.getString("anchor"));hint="2026-10-18"};val pattern=EditText(this).apply{setText((0 until j.getJSONArray("pattern").length()).joinToString(", "){j.getJSONArray("pattern").getString(it)});hint="주간, 야간, 휴무, 휴무"};view.addView(text("기준 날짜 (YYYY-MM-DD)",13));view.addView(anchor);view.addView(text("반복 순서 (쉼표 구분)",13));view.addView(pattern)
  AlertDialog.Builder(this).setTitle("근무 반복 패턴").setView(view).setNegativeButton("취소",null).setPositiveButton("저장"){_,_->try{LocalDate.parse(anchor.text.toString());val arr=pattern.text.toString().split(",").map{it.trim()};require(arr.isNotEmpty()&&arr.all{it in Engine.types});j.put("anchor",anchor.text.toString());j.put("pattern",JSONArray(arr));Engine.save(this,j);Engine.schedule(this);showHome()}catch(_:Exception){Toast.makeText(this,"날짜와 근무 이름을 확인해 주세요",Toast.LENGTH_LONG).show()}}.show()
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
   adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,Engine.types)
   setSelection(Engine.types.indexOf(original?.optString("shift")?:"야간").coerceAtLeast(0))
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
     r.put("name",str).put("shift",Engine.types[shift.selectedItemPosition]).put("mode",if(fixed)"fixed" else "relative").put("minute",min)
     if(original==null)arr.put(r)
     Engine.save(this,j);Engine.schedule(this);dlg.dismiss();showRules()
    }catch(_:Exception){Toast.makeText(this,"이름과 시간을 확인해 주세요 (근무 기준 전후 48시간 이내)",Toast.LENGTH_LONG).show()}
   }
  }
  dlg.show()
 }
 private fun shiftTimeDialog(){
  val j=Engine.data(this);val root=layout().apply{setPadding(dp(18),dp(10),dp(18),0)}
  val times=JSONObject((j.optJSONObject("shiftTimes")?:JSONObject()).toString())
  for((shift,def) in listOf("주간" to 480,"야간" to 1080)){
   root.addView(text("$shift 시작 시각",13,true,gray),inset(0,10,0,2))
   var total=times.optInt(shift,def)
   val button=Button(this).apply{
    text="%02d:%02d".format(total/60,total%60)
    setOnClickListener{TimePickerDialog(this@MainActivity,{_,h,m->total=h*60+m;times.put(shift,total);text="%02d:%02d".format(h,m)},total/60,total%60,true).show()}
   }
   root.addView(button)
  }
  AlertDialog.Builder(this).setTitle("근무 시작 시간").setView(root).setNegativeButton("취소",null).setPositiveButton("저장"){_,_->
   j.put("shiftTimes",times);Engine.save(this,j);Engine.schedule(this);showSettings()
  }.show()
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
 private fun showSettings(){init("설정");body.addView(audioSettings(),inset(0,0,0,16));val card=card();card.addView(btn("반복 패턴 변경") {patternDialog()},inset(0,0,0,8));card.addView(btn("근무 시작 시간 변경") {shiftTimeDialog()},inset(0,0,0,8));card.addView(btn("정확한 알람 권한 열기") {if(Build.VERSION.SDK_INT>=31)try{startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:$packageName")))}catch(_:Exception){Toast.makeText(this,"시스템 설정에서 알람 권한을 허용해 주세요",Toast.LENGTH_LONG).show()}else Toast.makeText(this,"이 버전에서는 별도 권한이 필요하지 않아요",Toast.LENGTH_SHORT).show()},inset(0,0,0,8));card.addView(btn("알람 다시 예약") {Engine.schedule(this);Toast.makeText(this,"알람을 다시 예약했어요",Toast.LENGTH_LONG).show()});body.addView(card);body.addView(text("알림 및 정확한 알람 권한을 허용해야 안정적으로 울립니다. 휴대폰 제조사별 절전 정책, 전체 화면 알림 정책의 영향을 받을 수 있으니 실제 기기로 테스트하세요.",13,false,gray),inset(2,16,2,0))}
}
