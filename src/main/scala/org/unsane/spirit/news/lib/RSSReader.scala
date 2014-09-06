package org.unsane.spirit.news.lib

import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale}

import it.sauronsoftware.feed4j.FeedParser
import net.liftweb.common.Loggable
import net.liftweb.util.Html5
import org.unsane.spirit.news.model._


/**
 * Created by
 * fabian on 05.09.14.
 *
 * this reader is needed to synchronize studip and spirit news
 */
object RSSReader extends Loggable with Config {
  def run() = {
    (new RSSImportActor).start()
  }

}
