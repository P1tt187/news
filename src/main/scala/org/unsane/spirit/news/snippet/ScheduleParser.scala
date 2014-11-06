package org.unsane.spirit.news.snippet

import java.util.Scanner
import java.util.regex.Pattern

import net.liftweb.util.BindHelpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.{JsRaw, Call}
import net.liftweb.http.{SHtml, S}
import org.unsane.spirit.news.lib.ScheduleParsingHelper
import net.liftweb.common.{Loggable, Full}
import net.liftweb.http.js.{JE, JsonCall, JsCmd,JsExp}
import org.unsane.spirit.news.model.ScheduleRecord
import scala.util._

import dispatch._,Defaults._
import scala.xml.NodeSeq


/**
 * Rendering view for importing Data via timetable2db.
 */
class ScheduleParser extends Loggable {


  def doParseShortCutButton()={

    val theUrl = sph.loadProps("shortcutUrl")

    Http(url(theUrl) OK as.String) onComplete{
      case Success(html:String)=>
        val allLectures = ScheduleRecord.findAll

        var lectureTitels = Map[String,String]()
        val secondTableString ="<td width=50%>"

        val scanner = new Scanner(html)

        while (scanner.hasNextLine){
         val line = scanner.nextLine().replaceAll("&nbsp;"," ")
           .replaceAll("&Auml;","Ä").replaceAll("&Ouml;","Ö").replaceAll("&Uuml;","Ü")
           .replaceAll("&auml;","ä").replaceAll("&ouml;","ö").replaceAll("&uuml;","ü")
           .replaceAll("&amp;","&")

          if(line.startsWith("<tr>")){
         val key = line.substring(line.indexOf("<b>")+ "<b>".length,line.indexOf("</b>")-2).trim
           val value = line.substring(line.indexOf(secondTableString)+secondTableString.length,line.lastIndexOf("</td>"))

           lectureTitels+= key->value
         }
        }
        scanner.close()

            allLectures.par.foreach{
              element=>
                val titleLong = lectureTitels.get(element.titleShort.value.replaceAll("\\p{javaSpaceChar}"," ")) match {
                  case Some(v)=> v
                  case None =>
                    element.titleShort.value
                }
                  element.titleLong.set(titleLong)
                  element.save

            }

      case Failure(e) =>
        logger error e
    }

    _Noop
  }

    lazy val sph = ScheduleParsingHelper()
    val schedules = Seq(("new", "Neuer Stundenplan"), ("old", "Alter Stundenplan"))
    val scheduleType = sph.loadChangeableProps("schedule")

    val (name2, js) = SHtml.ajaxCall(JE.JsRaw("this.value"),
                                     s => sph.saveProps("schedule", s)): (String, JsExp)

    val classNames = "alle" :: sph.allClassNamesAsLowercase

    def render = {

      "name=scheduleSwitch" #> SHtml.select(schedules, Full(scheduleType), x => x, "onchange" -> js.toJsCmd)

    }

  def shortCutButton = "name=parseShortcuts [onclick]" #> SHtml.ajaxInvoke(doParseShortCutButton)



}
