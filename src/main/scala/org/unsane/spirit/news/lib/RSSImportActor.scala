package org.unsane.spirit.news.lib

import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.{Calendar, Locale}

import it.sauronsoftware.feed4j.FeedParser
import net.liftweb.common.Loggable
import net.liftweb.util.Html5
import org.unsane.spirit.news.lib.RSSReader._
import org.unsane.spirit.news.model.{Tweet, Spreader, EntryCounter, Entry}

import scala.actors.Actor
import scala.actors.Actor._

/**
 * Created by fabian on 06.09.14.
 */
class RSSImportActor  extends Actor with Loggable{

  val FEED_URL = new URL("https://studip.fh-schmalkalden.de/rss.php?id=a88776e9ec68c2990f6cbb5ff8609752")
  val DOM_URL = "http://purl.org/dc/elements/1.1/"

  private lazy val dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z")
  private val df = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US)
  private val lifecycleFormat = new SimpleDateFormat("dd.MM.yyyy")

  private val tweet = loadProps("Tweet") == "yes"

  def act: Unit ={
    loopWhile(!Thread.interrupted()){
      parseFeed
      Thread.sleep(TimeUnit.MINUTES.toMillis(1))
    }
  }

  def parseFeed={
    val feed = FeedParser.parse(FEED_URL)

    val items = (0 to feed.getItemCount - 1).par.map {
      index =>
        feed.getItem(index)
    }.seq

    items.foreach {
      item =>
        val user = item.getElementValue(DOM_URL, "contributor")
        val subject = item.getTitle
        val news = item.getDescriptionAsHTML.replaceAll("<br />", "\n").replaceAll("<li>", "* ").replaceAll("</li>", "\n")


        val newsAsText = Html5.parse(news).get.text

        //println(item.getElementValue(DOM_URL,"date"))
        val pubDateString = item.getElementValue("", "pubDate").split(",")(1)

        val date = df.format(dateFormat.parse(pubDateString))
        val allEntrys = Entry.findAll
        if(Entry.findAll.find(_.news.get.trim.equalsIgnoreCase(newsAsText.trim)).isEmpty) {
          logger debug "insert new entry from rss"
          createEntry(user, date, subject, newsAsText)
        }
    }
  }

  def createEntry(user: String, date: String, subject: String, news: String) = {
    lazy val nr = if (EntryCounter.findAll.isEmpty) "0" else EntryCounter.findAll.head.counter.toString
    val CrudEntry = Entry.createRecord

    CrudEntry.date.set(date)
    CrudEntry.name.set(user)


    val changedSemester = if(subject.contains("(")){
      val parts = subject.substring(subject.indexOf("(")+1,subject.indexOf(")")).split(" ")
      val sb = new StringBuilder
      parts.foreach{
        p=>
          sb.append(allSemesterAsList4News.find(p.equalsIgnoreCase(_)).getOrElse("")).append(" ")
      }

      sb.toString()
    }else {
      ""
    }


    CrudEntry.semester.set(changedSemester)
    CrudEntry.nr.set(nr)
    CrudEntry.writer.set(user)
    CrudEntry.subject.set(subject)
    if (CrudEntry.subject.value.trim.isEmpty) {
      CrudEntry.subject.set((
        CrudEntry.news.value./:(("", 0)) { (o, i) =>
          if (o._2 > 20) o
          else (o._1 + i, o._2 + 1)
        }._1 + "...").replace("\n", " "))
      logger warn "Setting subject cause it was empty!"
    }

    val expireDate = Calendar.getInstance()
    expireDate.setTime(df.parse(date))
    expireDate.add(Calendar.DAY_OF_YEAR, 30)
    CrudEntry.lifecycle.set(lifecycleFormat.format(expireDate.getTime))
    CrudEntry.news.set(news)
    CrudEntry.save

    val count = if (EntryCounter.findAll.isEmpty) EntryCounter.createRecord else EntryCounter.findAll.head
    count.counter.set((nr.toInt + 1).toString)
    count.save

    if (tweet) {
      logger info "News should be spread via Twitter!"
      Spreader ! Tweet(CrudEntry.subject.value, changedSemester.split(" ").map(" #" + _).mkString, nr)
    }
  }

}
