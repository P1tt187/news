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
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the author nor the names of his contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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
package rest

import dispatch.Defaults._
import dispatch._
import net.liftweb.common.{Full, Loggable}
import net.liftweb.http._
import net.liftweb.http.rest.RestHelper
import net.liftweb.json
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser
import net.liftweb.util.Helpers._
import org.unsane.spirit.news.lib.Schedule
import org.unsane.spirit.news.model.Config

import scala.concurrent.Await

object RestApi extends RestHelper with Loggable with Config{

  logger info "Rest is now online."

  serve {

    /**
     * Rest Interface in order to put the JSON schedule into the DB.
     */
    case "scheduleapi" :: "fileupload" ::  _ JsonPut jsonfile -> _ => {
      logger.warn("Fileupload is used. Please check if the Schedule is fine!")
      Schedule.import2DatabaseQueue(json.compact(json.render(jsonfile))) match {
        case true => JsonResponse(("upload" -> "true!"), Nil, Nil, 200)
        case false => JsonResponse(("upload" -> "false!"), Nil, Nil, 200)
      }

    }

     /**
      * Get all News as JSON.
      * Maybe preview ? => ?preview=true
      * date filter, only if preview=true
      * /rest/1.0/news
      * @version 1.0
      */
    case "rest" :: "1.0" :: "news" :: Nil Get req => {
      if(req.param("preview").equals("true")) {
        JsonResponse(Response.getPreviewNews(req.params), Nil, Nil, 200)
      } else {
        JsonResponse(Response.getAllNews(req.params), Nil, Nil, 200)
      }
    }

    /**
     * Get one News as JSON.
     * /rest/1.0/news/<nr>
     * @version 1.0
     */
    case "rest" :: "1.0" :: "news" :: AsLong(id) :: Nil Get req => {
      val response = Response.getOneNews(id.toString())

      response match {
        case Full(x) => JsonResponse(x, Nil, Nil, 200)
        case _ => JsonResponse("exception" -> "this id is not valid", Nil, Nil, 404)
      }

    }

    /**
     * Get schedule for a given className and week.
     * /rest/1.0/schedule?classname=<classname>&week=<week>
     * @version 1.0
     */
    case "rest" :: "1.0" :: "schedule" :: Nil Get req => {
      val className = S.param("classname").openOr("").toLowerCase

      val week = S.param("week").openOr("").toLowerCase match {
        case "u" => "g"
        case "g" => "u"
        case _ => ""
      }

      JsonResponse(Response.getSchedule(className,week), Nil, Nil, 200)
    }

    /**
     * Get all News.
     * /news
     * @deprecated
     */
    case "news" :: Nil Get req => {
      JsonResponse(Response.getAllNews(req.params), Nil, Nil, 200)
    }

    /**
     * Get one News.
     * /news/<nr>
     * @deprecated
     */
    case "news" :: AsLong(id) :: Nil Get req => {
      val response = Response.getOneNews(id.toString())

      response match {
        case Full(x) => JsonResponse(x, Nil, Nil, 200)
        case _ => JsonResponse("exception" -> "this id is not valid", Nil, Nil, 404)
      }

    }

    /**
     * Get all News as JSON.
     * /rest/news
     * @deprecated
     */
    case "rest" :: "news" :: Nil Get req => {
      JsonResponse(Response.getAllNews(req.params), Nil, Nil, 200)
    }

    /**
     * Get one News as JSON.
     * /rest/news/<nr>
     * @deprecated
     */
    case "rest" :: "news" :: AsLong(id) :: Nil Get req => {
      val response = Response.getOneNews(id.toString())

      response match {
        case Full(x) => JsonResponse(x, Nil, Nil, 200)
        case _ => JsonResponse("exception" -> "this id is not valid", Nil, Nil, 404)
      }

    }

    /**
     * Get schedule for a given className and week.
     * /rest/schedule?classname=<classname>&week=<week>
     * @deprecated
     */
    case "rest" :: "schedule" :: Nil Get req => {
      val className = S.param("classname").openOr("").toLowerCase

      val week = S.param("week").openOr("").toLowerCase match {
        case "u" => "g"
        case "g" => "u"
        case _ => ""
      }

      JsonResponse(Response.getSchedule(className,week), Nil, Nil, 200)
    }

      /**
       * like count from socialmedia
       * /sharrif/facebook-like
       */
    case "sharrif" :: "facebook-like" :: Nil Get req =>{

      import net.liftweb.json.JsonDSL._

import scala.concurrent.duration._

      case class FacebookGraphResponse(likes: Int, id: String)

      val fbPagename = loadProps("fbPagename", "fhs.spirit")

      logger warn fbPagename

      val param = Map("fields"->"likes")
      val request = url("https://graph.facebook.com/"+ fbPagename) <<? param

      val json = Await.result(Http( request OK as.String ),  Duration(10, SECONDS))
      logger warn json
      val likeCount = JsonParser.parse(json).extract[FacebookGraphResponse]
      JsonResponse( "facebooklike" -> likeCount.likes ,Nil,Nil,200)

    }

  }

}
