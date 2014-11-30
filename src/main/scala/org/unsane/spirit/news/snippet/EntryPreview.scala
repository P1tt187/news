package org.unsane.spirit.news
package snippet

import net.liftweb.http._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js._
import net.liftweb.util.Helpers._
import net.liftweb.util._

import scala.xml._

trait EntryPreview extends SpiritHelpers {

  object jsonPreview extends JsonHandler {
    def apply(in: Any): JsCmd =
      SetHtml("entry_preview", in match {
        case JsonCmd("preview", _, p: String, _) =>
          <br /> +: <hr /> +: mkXMLHeader(ActuariusApp(p))
        case x => <b>Oops.... {x}</b>
      })
  }

  def mkPreview(in: NodeSeq): NodeSeq = {
    bind("json", in,
      "script" -> Script(jsonPreview.jsCmd),
      AttrBindParam("onclick", Text(jsonPreview.call("preview", ElemById("entry")~>Value).toJsCmd), "onclick"))
  }

  /**
   * Creates the Preview Button.
   */
  def createPreviewButton = {
    <div class="lift:CRUDEntry.mkPreview">
      <json:script></json:script>
      <button json:onclick="onclick">Vorschau</button>
       <div id="entry_preview"></div>
    </div>
  }
}
