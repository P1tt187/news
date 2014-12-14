package org.unsane.spirit.news.lib

import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.{Calendar, Locale}

import com.overzealous.remark.{Options, Remark}
import dispatch.Defaults._
import dispatch._
import it.sauronsoftware.feed4j.FeedParser
import net.liftweb.common.{Full, Loggable}
import org.apache.commons.lang3.StringEscapeUtils
import net.liftweb.common.Loggable
import org.unsane.spirit.news.lib.RSSReader._
import org.unsane.spirit.news.model.{User, Entry}
import org.unsane.spirit.news.snippet.CRUDEntry
import com.overzealous.remark.{Options, Remark}

import scala.actors.Actor
import scala.collection.mutable
import scala.util.{Failure, Success}

/**
 * @author fabian
 *         on 06.09.14.
 *
 *         the actor will contact the rss feed every minute
 *         if there are new entrys it will create new entrys for every new item and leave the existing as they are
 */
class RSSImportActor extends Actor with Loggable {

  val FEED_URL = "https://studip.fh-schmalkalden.de/rss.php?id=a88776e9ec68c2990f6cbb5ff8609752"
  val DOM_URL = "http://purl.org/dc/elements/1.1/"

  private lazy val df = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US)
  private lazy val lifecycleFormat = new SimpleDateFormat("dd.MM.yyyy")


  def act(): Unit = {
    loop {

      react {
        case Next =>
          logger debug "try to import rss entrys"

          val tmpFile = File.createTempFile("feed", ".xml")
          //val output = new Formatter(tmpFile)

          Http(url(FEED_URL) > as.File(tmpFile)) onComplete {
            case Success(_) =>


              parseFeed(tmpFile)

              tmpFile.delete()

            case Failure(errorMessage) =>
              logger error errorMessage

              tmpFile.delete()

          }



          Thread.sleep(TimeUnit.MINUTES.toMillis(1))
          this ! Next
        case Stop =>
          logger debug "exit rssimport actor"
          exit()
      }

      Thread.sleep(TimeUnit.MINUTES.toMillis(1))
    }

  }

  private def parseFeed(tmpFile: File) = {

    val feed = FeedParser.parse(tmpFile.toURI.toURL)

    val maxResults = if (feed.getItemCount > 50) {
      50
    } else {
      feed.getItemCount
    }

    val items = (0 to maxResults - 1).map {
      index =>
        feed.getItem(index)
    }.seq.sortBy(item => df.parse(item.getElementValue("", "pubDate")))

    items.foreach {
      item =>

        def newsTextEqual(entry: Entry, news: String): Boolean = {
          entry.news.get.replaceAll("\\p{javaSpaceChar}", " ").trim.equalsIgnoreCase(news)
        }

        val user = item.getElementValue(DOM_URL, "contributor")
        val subject = item.getTitle
        //val news = item.getDescriptionAsHTML.replaceAll("mailto:", "") //.replaceAll("<br />", "\n").replaceAll("<li>", "* ").replaceAll("</li>", "\n")
        val news = transformHtml2Markdown(correctNews(item.getDescriptionAsHTML).replaceAll("\\p{javaSpaceChar}", " ").trim)

        val pubDateString = item.getElementValue("", "pubDate")
        val baseURL = item.getLink.toString

        Entry.findAll.find(_.baseUrl.get.trim.equals(baseURL.trim)) match {
          case Some(existingEntry) =>
            if (!newsTextEqual(existingEntry, news)) {
              updateNewsEntry(subject, news, pubDateString, existingEntry)
            }
          case _ =>
            logger debug "insert new entry from rss"
            createEntry(user, pubDateString, subject, news, baseURL)
        }


    }
  }

  def updateNewsEntry(subject: String, news: String, pubDateString: String, existingEntry: Entry) {
    val CrudUpdate = new CRUDEntry
    val updateEntry = CrudUpdate.CrudEntry
    updateEntry.baseUrl.set(existingEntry.baseUrl.get)
    updateEntry.nr.set(existingEntry.nr.get)

    updateEntry.subject.set("[update] " + parseSubject(subject))
    updateEntry.writer.set(existingEntry.writer.get)
    updateEntry.name.set(existingEntry.name.get)
    updateEntry.semester.set(extractSemester(subject, news))
    updateEntry.date.set(pubDateString)

    CrudUpdate.tweetUpdate = true
    val expireDate = Calendar.getInstance()
    expireDate.add(Calendar.DAY_OF_YEAR, 30)
    CrudUpdate.CrudEntry.lifecycle.set(lifecycleFormat.format(expireDate.getTime))
    CrudUpdate.CrudEntry.news.set(news)
    CrudUpdate.update()
  }

  private def transformHtml2Markdown(content: String) = {
    val options = Options.markdown()
    options.simpleLinkIds = false
    options.inlineLinks = true
    options.preserveRelativeLinks = true
    val remark = new Remark(options)

    remark.convert(content)
  }

  private def correctNews(content: String): String = {
    val parts = content.split(" ")
    val originalMails = parts.filter(_.contains("mailto:"))
    val replacements = originalMails.map {
      om =>
        var replace = om
        if (replace.endsWith(".") || replace.endsWith(",") || replace.endsWith(":")) {
          replace = replace.substring(0, replace.length - 1)
        }

        (om, "<a href='" + replace + "'>" + om.replaceAll("mailto:", "") + "</a>")
    }
    var result = content
    replacements.foreach {
      case (original, replace) =>
        result = result.replaceAll(original, replace)
    }

    result
  }

  /** search for coursenames in the title and remove it */
  def parseSubject(subject: String): String = {
    val suffix = "[,:-]?[ ]?[,:-]?"
    val prefix = "[MBA]{2}"

    val extractCoursesResult: (String, List[(String, Int)]) = extractCourses(subject)
    var result: String = extractCoursesResult._1
    val coursesWithIndexes: List[(String, Int)] = extractCoursesResult._2



    if (coursesWithIndexes.isEmpty) {
      return subject
    }

    var filterList = coursesWithIndexes.filter(_._1.matches(prefix)).sortBy(_._2)
    if (filterList.isEmpty) {
      filterList = coursesWithIndexes
    }

    val (_, firstIndex) = filterList.head
    val (lastCourse, lastIndex) = filterList.last
    val replaceString = result.substring(firstIndex, lastIndex + lastCourse.length)
    result = result.replaceAll(replaceString, "").trim
    val suffixMatcher = Pattern.compile(suffix).matcher(result)

    if (suffixMatcher.find() && suffixMatcher.start() < 2) {
      result.replaceFirst(suffix, "").trim
    } else {
      result
    }

  }

  def extractCourses(subject: String): (String, List[(String, Int)]) = {
    val searchStrings = coursesWithAlias.keySet.par.flatMap {
      course =>

        coursesWithAlias(course).par.flatMap {
          alias =>
            semesterRange.map {
              number => alias + number
            }
        }
    }.toList

    val result: String = subject.replaceAll("\\p{javaSpaceChar}[-]", "")

    val coursesWithIndexes = searchStrings.map {
      search =>
        val pattern = Pattern.compile(search, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(result)
        val index = if (matcher.find()) {
          matcher.start()
        }
        else {
          -1
        }
        (search, index)
    }.filter { case (_, index) => index != -1}.sortBy(_._2)
    (result, coursesWithIndexes)
  }

  def createEntry(user: String, date: String, subject: String, news: String, baseURL: String) = {


    val CrudEntry = new CRUDEntry

    CrudEntry.CrudEntry.baseUrl.set(baseURL)
    //logger debug baseURL
    CrudEntry.CrudEntry.date.set(date)
    CrudEntry.CrudEntry.name.set(user)
    val expireDate = Calendar.getInstance()
    expireDate.add(Calendar.MONTH, 3)
    CrudEntry.CrudEntry.lifecycle.set(lifecycleFormat.format(expireDate.getTime))

    val changedSemester: String = extractSemester(subject, news)


    CrudEntry.CrudEntry.semester.set(changedSemester.toString.trim)
    CrudEntry.CrudEntry.writer.set(user)
    CrudEntry.CrudEntry.subject.set(parseSubject(parseSubject(subject)))
    CrudEntry.CrudEntry.news.set(news)
    CrudEntry.create()
  }

  def extractSemester(subject: String, news: String): String = {

    val changedSemester = new mutable.StringBuilder

    val parts = subject.replaceAll("[():,.-]", " ").trim.toUpperCase.split(" ") ++ news.replaceAll("[():,.]", " ").trim.toUpperCase.split(" ")


    val theCourses = (parts.toSet.flatMap {
      p: String =>
        allSemesterAsList4News.filter(sem => p.equalsIgnoreCase(sem) || p.equalsIgnoreCase("BA" + sem))
    } ++ extractCourses(subject)._2.map(_._1)).toList.sorted



    changedSemester.append(theCourses.mkString(" "))
    if (changedSemester.toString.trim.isEmpty) {
      changedSemester append "semester alte_semester"
    }

   // logger debug "Extracted Semester " + changedSemester.toString()
    changedSemester.toString()
  }
}

case object Stop

case object Next