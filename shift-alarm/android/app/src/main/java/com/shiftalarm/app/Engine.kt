package com.shiftalarm.app

import android.app.*
import android.content.*
import android.os.Build
import org.json.*
import java.time.*
import java.time.format.DateTimeFormatter

object Engine {
    const val CHANNEL="shift_alarm_channel_v2"
    val types = listOf("주간", "야간", "휴무", "특근")
    fun shiftTypes(j:JSONObject):List<String>{
        val result=types.toMutableList()
        val extra=j.optJSONArray("customShifts")
        if(extra!=null)for(i in 0 until extra.length()){
            val name=extra.optString(i).trim()
            if(name.isNotEmpty()&&!result.contains(name))result.add(name)
        }
        return result
    }
    fun data(c:Context): JSONObject {
        val raw=c.getSharedPreferences("shift",0).getString("data",null)
        if(raw!=null) return try { JSONObject(raw) } catch(_:Exception){ defaultData() }
        return defaultData().also{save(c,it)}
    }
    fun defaultData():JSONObject {
        val rules=JSONArray()
        fun add(n:String,s:String,m:String,min:Int){rules.put(JSONObject().put("id",System.nanoTime()+rules.length()).put("name",n).put("shift",s).put("mode",m).put("minute",min).put("enabled",true))}
        add("출근 준비","주간","relative",-60);add("오전 순찰","주간","relative",120);add("휴식 종료","주간","relative",300)
        add("출근 준비","야간","relative",-60);add("야간 순찰","야간","relative",240);add("휴식 종료","야간","relative",480)
        return JSONObject().put("shiftTimes",JSONObject().put("주간",480).put("야간",1080).put("특근",540)).put("anchor",LocalDate.now().toString()).put("pattern",JSONArray(listOf("주간","야간","휴무","휴무"))).put("overrides",JSONObject()).put("rules",rules)
    }
    fun save(c:Context,j:JSONObject){c.getSharedPreferences("shift",0).edit().putString("data",j.toString()).apply()}
    fun shift(j:JSONObject,d:LocalDate):String {
        val over=j.getJSONObject("overrides");if(over.has(d.toString()))return over.getString(d.toString())
        val p=j.getJSONArray("pattern");if(p.length()==0)return "휴무"
        val diff=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(j.getString("anchor")),d).toInt()
        return p.getString(Math.floorMod(diff,p.length()))
    }
    fun startMinutes(j:JSONObject,shift:String)=j.optJSONObject("shiftTimes")?.optInt(shift,when(shift){"주간"->480;"야간"->1080;"특근"->540;else->0})?:0
    data class Event(val id:Long,val name:String,val time:ZonedDateTime,val shift:String)
    fun events(j:JSONObject,date:LocalDate):List<Event>{
        val typ=shift(j,date);val a=j.getJSONArray("rules");val out=mutableListOf<Event>()
        for(i in 0 until a.length()) {val r=a.getJSONObject(i)
            if(!r.optBoolean("enabled",true)||r.optString("shift")!=typ)continue
            val min=r.getInt("minute")+(if(r.getString("mode")=="relative")startMinutes(j,typ) else 0)
            val time=date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(min.toLong())
            out.add(Event(r.getLong("id"),r.getString("name"),time,typ))
        };return out.sortedBy{it.time.toInstant()}
    }
    fun channel(c:Context){val n=c.getSystemService(NotificationManager::class.java);val ch=NotificationChannel(CHANNEL,"근무 알람",NotificationManager.IMPORTANCE_HIGH).apply { description="근무 일정과 휴식 종료 알람";enableVibration(false);setSound(null,null);lockscreenVisibility=Notification.VISIBILITY_PUBLIC };n.createNotificationChannel(ch)}
    fun schedule(c:Context){
        channel(c); val manager=c.getSystemService(AlarmManager::class.java);val prefs=c.getSharedPreferences("shift",0)
        // Cancel old pending alarms including entries deleted or changed.
        val old=prefs.getString("scheduled_ids","").orEmpty().split(',').filter{it.isNotEmpty()}
        for(id in old) {val intent=Intent(c,AlarmReceiver::class.java).setAction("com.shiftalarm.app.ALARM");val pi=PendingIntent.getBroadcast(c,id.toInt(),intent,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE);if(pi!=null){manager.cancel(pi);pi.cancel()}}
        val used=mutableListOf<String>();val now=ZonedDateTime.now();val j=data(c)
        for(day in -1..59)for(e in events(j,LocalDate.now().plusDays(day.toLong()))) {
            if(!e.time.isAfter(now))continue
            val code=(e.time.toInstant().toEpochMilli().toString()+e.id).hashCode()
            val intent=Intent(c,AlarmReceiver::class.java).setAction("com.shiftalarm.app.ALARM").putExtra("title",e.name).putExtra("time",e.time.toInstant().toEpochMilli()).putExtra("code",code).putExtra("alarm_key",e.id.toString())
            val pi=PendingIntent.getBroadcast(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {if(Build.VERSION.SDK_INT>=31&&manager.canScheduleExactAlarms())manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.time.toInstant().toEpochMilli(),pi)
            else if(Build.VERSION.SDK_INT<31)manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.time.toInstant().toEpochMilli(),pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.time.toInstant().toEpochMilli(),pi)
            used.add(code.toString())
            }catch(_:SecurityException){manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.time.toInstant().toEpochMilli(),pi);used.add(code.toString())}
        }
        prefs.edit().putString("scheduled_ids",used.joinToString(",")).apply()
    }
    fun time(t:ZonedDateTime)=t.format(DateTimeFormatter.ofPattern("HH:mm"))
}
