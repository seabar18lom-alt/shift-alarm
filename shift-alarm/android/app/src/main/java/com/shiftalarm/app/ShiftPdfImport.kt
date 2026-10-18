package com.shiftalarm.app

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.time.LocalDate
import kotlin.math.abs

object ShiftPdfImport {
 data class Assignment(val date:LocalDate,val daytime:String?,val nighttime:String?)
 data class Result(val assignments:List<Assignment>,val names:List<String>,val year:Int,val month:Int)
 private data class Glyph(val text:String,val x:Float,val y:Float,val width:Float)
 private data class Word(val text:String,val x:Float,val y:Float)
 fun parse(context:Context,uri:Uri):Result {
  PDFBoxResourceLoader.init(context.applicationContext)
  val input=context.contentResolver.openInputStream(uri)?:error("PDF를 열 수 없습니다")
  return input.use{stream->PDDocument.load(stream).use{document->
   require(document.numberOfPages>0){"PDF 페이지가 없습니다"}
   val glyphs=mutableListOf<Glyph>()
   val stripper=object:PDFTextStripper(){
    override fun writeString(text:String?,textPositions:MutableList<TextPosition>?){
     textPositions?.forEach{p->if(p.unicode.isNotBlank())glyphs.add(Glyph(p.unicode,p.xDirAdj,p.yDirAdj,p.widthDirAdj))}
    }
   }
   stripper.sortByPosition=true
   stripper.startPage=1;stripper.endPage=1;stripper.getText(document)
   // Text in this PDF is drawn twice at the same position. Keep only one
   // occurrence of each overlapping glyph, while preserving real repeated letters.
   val uniqueGlyphs=glyphs.distinctBy { g ->
    "${g.text}:${(g.x*2).toInt()}:${(g.y*2).toInt()}"
   }
   val words=mutableListOf<Word>()
   // PDFBox coordinates are screen-style y positions. Group characters by baseline.
   val lines=mutableListOf<MutableList<Glyph>>()
   for(g in uniqueGlyphs.sortedWith(compareBy<Glyph>{it.y}.thenBy{it.x})){
    val line=lines.lastOrNull()
    if(line==null||abs(line.first().y-g.y)>3f)lines.add(mutableListOf(g)) else line.add(g)
   }
   for(line in lines){
    var token="";var tokenX=0f;var right=0f
    for(g in line.sortedBy{it.x}){
     if(token.isNotEmpty()&&g.x-right>7f){words.add(Word(token,tokenX,line.first().y));token=""}
     if(token.isEmpty())tokenX=g.x
     token+=g.text;right=g.x+g.width
    }
    if(token.isNotEmpty())words.add(Word(token,tokenX,line.first().y))
   }
   val plain=stripper.getText(document)
   // Prefer coordinates over extraction order for split heading glyphs.
   val heading=uniqueGlyphs.filter{it.y<110f}.sortedWith(compareBy<Glyph>{it.y}.thenBy{it.x}).joinToString(""){it.text}
   val normalized=heading.replace(Regex("\\s+"),"")
   val first=Regex("(20\\d{2})년(1[0-2]|0?[1-9])월").find(normalized)
      ?:Regex("(20\\d{2}).{0,8}?(1[0-2]|0?[1-9])월").find(normalized)
      ?:Regex("(20\\d{2})년\\s*(\\d{1,2})월").find(plain)
   val year=first?.groupValues?.get(1)?.toIntOrNull()
      ?:Regex("20\\d{2}").find(normalized)?.value?.toIntOrNull()
      ?:error("연도를 찾지 못했어요. 제목 추출: "+normalized.take(65))
   val month=first?.groupValues?.get(2)?.toIntOrNull()
      ?:Regex("(1[0-2]|0?[1-9])월").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
      ?:error("월을 찾지 못했어요. 제목 추출: "+normalized.take(65))
   val maxDay=LocalDate.of(year,month,1).lengthOfMonth()
   // Only date cells within the top timetable region; ignore footer contact numbers.
   val days=words.filter{w->val n=w.text.toIntOrNull();n!=null&&n in 1..maxDay&&w.y<document.getPage(0).mediaBox.height*0.6f}
   fun nameFor(day:Word,above:Boolean):String?{
    val target=day.y+(if(above)-14f else 10f)
    return words.filter{w->
     abs(w.y-target)<9f && (w.x-day.x) in 25f..85f &&
      w.text.matches(Regex("[가-힣]{2,5}"))
    }.minByOrNull{abs(it.y-target)}?.text
   }
   val assignments=days.mapNotNull{w->
    val n=w.text.toIntOrNull()?:return@mapNotNull null
    Assignment(LocalDate.of(year,month,n),nameFor(w,true),nameFor(w,false))
   }.distinctBy{it.date}.sortedBy{it.date}
   require(assignments.size>=20&&assignments.count{it.daytime!=null&&it.nighttime!=null}>=15){"이 PDF 근무표 배치를 인식하지 못했습니다"}
   val names=assignments.flatMap{listOfNotNull(it.daytime,it.nighttime)}.distinct().sorted()
   require(names.isNotEmpty()){"직원 이름을 찾을 수 없습니다"}
   Result(assignments,names,year,month)
  }}
 }
}
