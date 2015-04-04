package org.unsane.spirit.news.snippet

import net.liftweb.common.{Full, Loggable}
import net.liftweb.http.js.{JE, JsExp}
import net.liftweb.http.{S, SHtml, SessionVar}
import org.unsane.spirit.news.model.{Config, GroupRecord}

/**
 * @author fabian 
 * @since 29.03.15.
 */
class GroupView extends Config with Loggable {

  private object classNameVar extends SessionVar[String](S.param("classname").openOr(""))

  S.param("classname").openOr("") match {
    case "" =>
    case s if (!(allClassNamesAsLowercase contains s)) => S.redirectTo("/404")
    case s => classNameVar.set(s)
  }

  classNameVar.get match {
    case s if (allClassNamesAsLowercase contains s) =>
    case "" =>
      classNameVar(allClassNamesAsLowercase.head.toLowerCase)
    case _ => S.redirectTo("/404")
  }

  def selectClassnameBox = {

    val (_, js) = SHtml.ajaxCall(JE.JsRaw("this.value"),
      s => {
        classNameVar(s)
        S.redirectTo("/schedule/displaygroups?classname=" + s)
      }): (String, JsExp)

    SHtml.select(allClassNamesAsLowercase.map(x => (x, x)), Full(classNameVar.get),
      x => x, "onchange" -> js.toJsCmd)
  }

  def render = {
    val groups = GroupRecord.findAll.filter(_.className.get.equals(classNameVar.get))
    val groupTypesSet = groups.map(_.groupType.get).toSet
    val groupTypes = groupTypesSet.toList.sortBy { gt => groups.count(g => g.groupType.get.equals(gt)) }

    val tables = groupTypes.map {
      gt =>
        val filteredGroups = groups.filter(_.groupType.get.trim.equals(gt.trim)).sortBy(_.groupIndex.get)
        val maxIndex = filteredGroups.map(_.students.get.size).max

        <table>
          <caption>
            {gt}
          </caption>
          <thead>
            <tr>
              {filteredGroups.map {
              fg =>
                <th>
                  Gruppe
                  {fg.groupIndex.get}
                </th>
            }}
            </tr>
          </thead>
          <tbody>
            {for (studentIndex <- 0 to maxIndex - 1) yield {
            <tr>
              {filteredGroups.map { fg =>
              <td class="textcenter">
                {val students = fg.students.get
              students.lift(studentIndex) match {
                case Some(student) =>
                  student.firstName + " " + student.lastName
                case None => ""
              }}
              </td>
            }}
            </tr>
          }}
          </tbody>
        </table>
    }

    <div>
      {tables}
    </div>
  }

}
