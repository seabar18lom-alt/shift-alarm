package com.shiftalarm.app

import android.app.*
import android.content.*
import android.os.Build
import org.json.*
import java.time.*
import java.time.format.DateTimeFormatter

object Engine {
    const val CHANNEL="shift_alarm_channel_v2"
    fun shiftTypes(j:JSONObject):List<String>{
        val result=mutableListOf("휴무")
        val registered=j.optJSONArray("customShifts")
        if(registered!=null)for(i in 0 until registered.length()){
            val n=registered.optString(i).trim()
            if(n.isNotEmpty()&&n!="휴무"&&n !in result)result.add(n)
        }
        return result
    }
    private fun migrate(j:JSONObject):JSONObject {
        if(j.has("shiftTypesMigrated"))return j
        val names=linkedSetOf<String>()
        fun add(n:String){if(n.isNotBlank()&&n!="휴무")names.add(n)}
        val saved=j.optJSONArray("customShifts")
        if(saved!=null)for(i in 0 until saved.length())add(saved.optString(i))
        val pattern=j.optJSONArray("pattern")
        if(pattern!=null)for(i in 0 until pattern.length())add(pattern.optString(i))
        val overrides=j.optJSONObject("overrides")
        if(overrides!=null){val keys=overrides.keys();while(keys.hasNext()){add(overrides.optString(keys.next()))}}
        val rules=j.optJSONArray("rules")
        if(rules!=null)for(i in 0 until rules.length())add(rules.optJSONObject(i)?.optString("shift")?:"")
        // Keep existing user time definitions, but do not preserve unused legacy special duty.
        val times=j.optJSONObject("shiftTimes")
        if(times!=null){val keys=times.keys();while(keys.hasNext()){
            val n=keys.next()
            if(n!="특근"||n in names)add(n)
        }}
        j.put("customShifts",JSONArray(names.toList()))
        j.put("shiftTypesMigrated",true)
        return j
    }
    fun data(c:Context): JSONObject {
        val raw=c.getSharedPreferences("shift",0).getString("data",null)
        if(raw!=null) return try { migrate(JSONObject(raw)).also{save(c,it)} } catch(_:Exception){ defaultData() }
        return defaultData().also{save(c,it)}
    }
    fun defaultData():JSONObject {
        return JSONObject()
            .put("shiftTypesMigrated",true)
            .put("customShifts",JSONArray())
            .put("shiftTimes",JSONObject())
            .put("anchor",LocalDate.now().toString())
            .put("pattern",JSONArray(listOf("휴무")))
            .put("overrides",JSONObject())
            .put("rules",JSONArray())
    }
    fun save(c:Context,j:JSONObject){c.getSharedPreferences("shift",0).edit().putString("data",j.toString()).apply()}
    fun shift(j:JSONObject,d:LocalDate):String {
        val over=j.getJSONObject("overrides");if(over.has(d.toString()))return over.getString(d.toString())
        val p=j.getJSONArray("pattern");if(p.length()==0)return "휴무"
        val diff=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(j.getString("anchor")),d).toInt()
        return p.getString(Math.floorMod(diff,p.length()))
    }
    fun startMinutes(j:JSONObject,shift:String)=j.optJSONObject("shiftTimes")?.optInt(shift,0)?:0
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
