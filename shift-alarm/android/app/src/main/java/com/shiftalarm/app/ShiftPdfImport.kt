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
   val words=mutableListOf<Word>()
   // PDFBox coordinates are screen-style y positions. Group characters by baseline.
   val lines=mutableListOf<MutableList<Glyph>>()
   for(g in glyphs.sortedWith(compareBy<Glyph>{it.y}.thenBy{it.x})){
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
   val match=Regex("(20\\d{2})년\\s*(\\d{1,2})월").find(plain)
      ?:error("연도와 월을 찾을 수 없습니다")
   val year=match.groupValues[1].toInt();val month=match.groupValues[2].toInt()
   val maxDay=LocalDate.of(year,month,1).lengthOfMonth()
   // Only date cells within the top timetable region; ignore footer contact numbers.
   val days=words.filter{w->val n=w.text.toIntOrNull();n!=null&&n in 1..maxDay&&w.y<document.getPage(0).mediaBox.height*0.7f}
   fun nameFor(day:Word,above:Boolean):String?{
    val target=day.y+(if(above)-12f else 14f)
    return words.filter{w->
     abs(w.y-target)<9f && (w.x-day.x) in 22f..76f &&
      w.text.matches(Regex("[가-힣]{2,5}"))
    }.minByOrNull{abs(it.y-target)}?.text
   }
   val assignments=days.mapNotNull{w->
    val n=w.text.toIntOrNull()?:return@mapNotNull null
    Assignment(LocalDate.of(year,month,n),nameFor(w,true),nameFor(w,false))
   }.distinctBy{it.date}.sortedBy{it.date}
   require(assignments.size>=7&&assignments.any{it.daytime!=null||it.nighttime!=null}){"이 PDF 근무표 배치를 인식하지 못했습니다"}
   val names=assignments.flatMap{listOfNotNull(it.daytime,it.nighttime)}.distinct().sorted()
   require(names.isNotEmpty()){"직원 이름을 찾을 수 없습니다"}
   Result(assignments,names,year,month)
  }}
 }
}
