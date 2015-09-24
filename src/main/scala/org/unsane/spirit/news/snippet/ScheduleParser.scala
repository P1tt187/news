package org.unsane.spirit.news.snippet

import java.net.{HttpURLConnection, URL}
import java.util.Scanner
import java.util.regex.Pattern

import dispatch.Defaults._
import dispatch._
import net.liftweb.common.{Full, Loggable}
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.{JE, JsExp}
import net.liftweb.util.BindHelpers._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.unsane.spirit.news.lib.ScheduleParsingHelper
import org.unsane.spirit.news.model.{GroupRecord, ScheduleRecord, student}
import org.xml.sax.InputSource

import scala.annotation.tailrec
import scala.util._
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Node, NodeSeq}


/**
 * Rendering view for importing Data via timetable2db.
 */
class ScheduleParser extends Loggable {

  lazy val adapter = new NoBindingFactoryAdapter
  lazy val parser = (new SAXFactoryImpl).newSAXParser

  private def replaceHTMLExtraSymbols(str: String) = {
    str.replaceAll("&nbsp;", " ")
      .replaceAll("&Auml;", "Ä").replaceAll("&Ouml;", "Ö").replaceAll("&Uuml;", "Ü")
      .replaceAll("&auml;", "ä").replaceAll("&ouml;", "ö").replaceAll("&uuml;", "ü")
      .replaceAll("&amp;", "&").replaceAll("&szlig;", "ß")
  }

  def doParseShortCutButton() = {

    val theUrl = sph.loadProps("shortcutUrl")

    Http(url(theUrl) OK as.String) onComplete {
      case Success(html: String) =>
        val allLectures = ScheduleRecord.findAll

        var lectureTitles = Map[String, String]()
        val secondTableString = "<td width=50%>"

        val scanner = new Scanner(html)

        while (scanner.hasNextLine) {
          val line = replaceHTMLExtraSymbols(scanner.nextLine())

          if (line.startsWith("<tr>")) {
            val key = line.substring(line.indexOf("<b>") + "<b>".length, line.indexOf("</b>") - 2).trim
            val value = line.substring(line.indexOf(secondTableString) + secondTableString.length, line.lastIndexOf("</td>"))

            lectureTitles += key -> value
          }
        }
        scanner.close()

        allLectures.par.foreach {
          element =>
            val titleLong = lectureTitles.get(element.titleShort.value.replaceAll("\\p{javaSpaceChar}", " ")) match {
              case Some(v) => v
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

  private def createCourseGrouptypes(elements: NodeSeq) = {

    val pattern = Pattern.compile("^[bm]a*", Pattern.CASE_INSENSITIVE)

    val buf = scala.collection.mutable.ListBuffer.empty[(String, String)]

    @tailrec
    def doPrivate(elements: NodeSeq): List[(String, String)] = {
      //logger debug elements
      if (elements.isEmpty) {
        buf.toList
      } else {
        val element = elements.head

        if (pattern.matcher(element.text).find()) {

          val textLower = element.text.toLowerCase
          val text = element.text
          val theClassList = sph.allClassNamesAsLowercase.filter(className => textLower.contains(className))

          if(theClassList.nonEmpty) {
            val theClass = theClassList.head
            val groupType = text.substring(text.indexOf('(') + 1, text.indexOf(')'))

            val append = (theClass, groupType)


            buf += append
          }
        }
        doPrivate(elements.tail)

      }
    }

    doPrivate(elements)
  }

  def doParseGroupsButton() = {
    val theUrl =  sph.loadProps("groupUrl")
    val parsedHtml = load(new URL(theUrl))
    val body = parsedHtml \ "body"
    /** first: create a list of tuples with course and grouptype */
    val groupTypes = createCourseGrouptypes(((body \\ "p") \ "font") \ "a")
    /** second: collect all data from grouptyes */
    val studentStructure = (body \\ "table").map {
      t =>
        (t \\ "td").map {
          td =>
            td.toString.replaceAll("<td valign=\"middle\" rowspan=\"1\" colspan=\"1\" align=\"left\">", "").replaceAll("</td>", "").replaceAll("<br clear=\"none\"/>", ";").split(";").map {
              studentString =>
                val split = studentString.split(",")
                (split.lift(1).getOrElse("").trim, split(0).trim)
            }
        }
    }
    GroupRecord.findAll.foreach(_.delete_!)

    /** third: combine all data and save it */
    for (groupIndex <- 0 to groupTypes.size - 1) {
      val (theClass, groupType) = groupTypes(groupIndex)

      var i = 0
      studentStructure(groupIndex).foreach {
        groupList =>
          i += 1
          val groupRecord = GroupRecord.createRecord
          groupRecord.className.set(theClass)
          groupRecord.groupType.set(groupType)
          groupRecord.groupIndex.set(i)

          val studentList = groupList.map {
            case (firstName, lastName) =>
              student(firstName, lastName)
          }.toList
          groupRecord.students.set(studentList)
          groupRecord.save
      }
    }

    //logger debug groupTypes.toString()

    _Noop
  }

  def load(url: URL, headers: Map[String, String] = Map.empty): Node = {
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    for ((k, v) <- headers)
      conn.setRequestProperty(k, v)
    val source = new InputSource(conn.getInputStream)
    adapter.loadXML(source, parser)
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

  def parseGroupsButton = "name=parseGroups [onclick]" #> SHtml.ajaxInvoke(doParseGroupsButton)

}
