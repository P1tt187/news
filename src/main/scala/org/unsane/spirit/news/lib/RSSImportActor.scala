package org.unsane.spirit.news.lib

import java.net.{URLEncoder, URL}
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.{Calendar, Locale}

import it.sauronsoftware.feed4j.FeedParser
import net.liftweb.common.Loggable
import net.liftweb.util.HttpHelpers
import org.unsane.spirit.news.lib.RSSReader._
import org.unsane.spirit.news.model.Entry
import org.unsane.spirit.news.snippet.CRUDEntry

import scala.actors.Actor

/**
 * @author fabian
 *         on 06.09.14.
 *
 *         the actor will contact the rss feed every minute
 *         if there are new entrys it will create new entrys for every new item and leave the existing as they are
 */
class RSSImportActor extends Actor  with Loggable {

  val FEED_URL = new URL("https://studip.fh-schmalkalden.de/rss.php?id=a88776e9ec68c2990f6cbb5ff8609752")
  val DOM_URL = "http://purl.org/dc/elements/1.1/"

  private lazy val df = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US)
  private lazy val lifecycleFormat = new SimpleDateFormat("dd.MM.yyyy")


  def act(): Unit = {
    loop {

      react {
        case Next =>
          logger debug "try to import rss entrys"

          parseFeed()

          Thread.sleep(TimeUnit.MINUTES.toMillis(1))
          this ! Next
        case Stop =>
          logger debug "exit rssimport actor"
          exit()
      }

      Thread.sleep(TimeUnit.MINUTES.toMillis(1))
    }

  }

  def parseFeed() = {
    val feed = FeedParser.parse(FEED_URL)

    val maxResults = if (feed.getItemCount > 50) {
      50
    } else {
      feed.getItemCount
    }

    val items = (0 to maxResults - 1).par.map {
      index =>
        feed.getItem(index)
    }.seq.sortBy(item => df.parse(item.getElementValue("", "pubDate")))

    items.foreach {
      item =>
        val user = item.getElementValue(DOM_URL, "contributor")
        val subject = item.getTitle
        val news = item.getDescriptionAsHTML.replaceAll("mailto:","") //.replaceAll("<br />", "\n").replaceAll("<li>", "* ").replaceAll("</li>", "\n")

        val pubDateString = item.getElementValue("", "pubDate").split(",")(1)
        val baseURL = item.getLink.toString

        if (Entry.findAll.find(_.news.get.trim.equalsIgnoreCase(news.trim)).isEmpty) {
          logger debug "insert new entry from rss"
          createEntry(user, pubDateString, subject, news, baseURL)
        }
    }
  }

  def createEntry(user: String, date: String, subject: String, news: String, baseURL:String) = {
    val CrudEntry = new CRUDEntry

    CrudEntry.CrudEntry.baseUrl.set(URLEncoder.encode(baseURL,"UTF-8") )
    //logger debug CrudEntry.CrudEntry.baseUrl.get
    CrudEntry.CrudEntry.date.set(date)
    CrudEntry.CrudEntry.name.set(user)
    val expireDate = Calendar.getInstance()
    expireDate.add(Calendar.DAY_OF_YEAR, 30)
    CrudEntry.CrudEntry.lifecycle.set(lifecycleFormat.format(expireDate.getTime))

    val changedSemester = new StringBuilder

    val parts = subject.replaceAll("[():,.-]", " ").trim.toUpperCase.split(" ") ++ news.replaceAll("[():,.]", " ").trim.toUpperCase.split(" ")

    val theCourses = parts.toSet.flatMap {
      p: String =>
        allSemesterAsList4News.filter(sem => p.equalsIgnoreCase(sem) || p.equalsIgnoreCase("BA" + sem))
    }.toList.sorted
    changedSemester.append(theCourses.mkString(" "))
    if (changedSemester.toString.trim.isEmpty) {
      changedSemester append "semester alte_semester"
    }


    CrudEntry.CrudEntry.semester.set(changedSemester.toString.trim)
    CrudEntry.CrudEntry.writer.set(user)
    CrudEntry.CrudEntry.subject.set(subject)
    CrudEntry.CrudEntry.news.set(news)
    CrudEntry.create()
  }

}

case object Stop

case object Next