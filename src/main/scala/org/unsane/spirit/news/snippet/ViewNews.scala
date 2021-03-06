/**
 * Copyright (c) 2010 spirit-fhs
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the author nor the names of his contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.unsane.spirit.news
package snippet


import net.liftweb.common.{Full, Loggable}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.{JE, JsCmd, JsExp}
import net.liftweb.http.{S, SHtml}
import net.liftweb.json.JsonDSL._
import net.liftweb.markdown._
import net.liftweb.util.Helpers._
import org.unsane.spirit.news.model.{Config, Entry, LoginUtil}

import scala.xml._

/**
 * @author Marcus Denison
 */
object ViewNews {
  /** variable is used to show if fsi office is in use */
  private var fsiOfficeIsInUse = false
}

class ViewNews extends SpiritHelpers with Loggable with Config {


  /**
   * Pattern Matching if a search has begun.
   * If not, all News will be returned.
   */
  lazy val news: List[Entry] = S.param("search") match {

    case Full(s) =>
      logger info ("Searching for " + s + "!")
      val validSearch =
        loadSemesters("BaI") :: loadSemesters("BaWI") ::
          loadSemesters("BaITS") :: loadSemesters("BaMuMa") :: loadSemesters("BaMC") ::
          loadSemesters("Ma") :: loadSemesters("Other") :: Nil

      if (validSearch.flatten.contains(s)) {
        Entry.findAll.filter { entry =>
          entry.contains(s) || entry.contains("semester")
        }.sortWith(
            (entry1, entry2) => (entry1 > entry2)
          )
      } else {
        Entry.find("nr" -> s) match {
          case Full(x) => List(x)
          case _ => Entry.findAll.sortWith(
            (entry1, entry2) => (entry1 > entry2)
          )
        }
      }

    case _ =>
      Entry.findAll.sortWith(
        (entry1, entry2) => (entry1 > entry2)
      )
  }

  /**
   * Adding a dropdown menu to the ViewNews in order
   * to achieve a better user experience when searching
   * for news.
   */
  def classNameChooser() = {

    val classNames =
      "alle" :: allSemesterAsList4News zip "Alle" :: allClassNamesAsLowercase

    val (name2, js) = SHtml.ajaxCall(JE.JsRaw("this.value"),
      s => S.redirectTo("/semsearch/" + s)): (String, JsExp)

    SHtml.select(classNames.toSeq, Full(S.param("search").openOr("Alle")), x => x, "onchange" -> js.toJsCmd)

  }

  def render = {
    fsiOfficePart

    ".entry" #> news.map(entry =>
      ".writer" #> entry.writer.value.toString &
        ".subject" #> <a href={"/entry/" + entry.nr.value.toString}>
          {mkXMLHeader(ActuariusApp(entry.subject.value.toString))}
        </a> &
        ".nr" #> entry.nr.value.toString &
        ".lifecycle" #> entry.lifecycle.value.toString &
        ".date" #> Text(entry.date.value.replaceAll("[+]0100", "").trim) &
        ".semester" #> sem2link(semesterChanger(entry.semester.value.toString).split(" ")) &
        ".news" #> mkXMLHeader(ActuariusApp(mkNewsText(entry)))
      //".news"      #> TextileParser.toHtml(entry.news.value.toString)
    )

  }

  def fsiOfficePart = {
    if (ViewNews.fsiOfficeIsInUse) {
      S.notice("FSI-Büro besetzt")
    } else {
      S.error("FSI-Büro nicht besetzt")
    }
  }

  def displayIsInUseForm() = {
    if (LoginUtil.isLogged) {
      <div data-lift="ViewNews.toggleFsiOfficeEvent">
        {if (ViewNews.fsiOfficeIsInUse) {
        <input type="checkbox" name="toggleFsiOfficeIsInUse" id="toggleFsiOfficeIsInUse" checked="checked"></input>
      } else {
        <input type="checkbox" name="toggleFsiOfficeIsInUse" id="toggleFsiOfficeIsInUse"></input>
      }}
        FSI-Büro besetzt
      </div>
    } else {
      <div></div>
    }
  }

  def toggleFsiOffice: JsCmd = {
    if (ViewNews.fsiOfficeIsInUse) {
      ViewNews.fsiOfficeIsInUse = false
    } else {
      ViewNews.fsiOfficeIsInUse = true
    }
    fsiOfficePart

    _Noop
  }

  def toggleFsiOfficeEvent = {
    "name=toggleFsiOfficeIsInUse [onclick]" #> SHtml.ajaxInvoke(() => toggleFsiOffice)
  }


  def mkNewsText(entry: Entry): String = {
    val newsText = new StringBuilder
    newsText append entry.news.value
    if (entry.baseUrl.toString().nonEmpty) {
      newsText append "\n\n[Quelle]("
      newsText append entry.baseUrl
      newsText append ")"
    }
    newsText.toString()
  }

}
