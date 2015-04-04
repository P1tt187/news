package org.unsane.spirit.news.snippet

import org.unsane.spirit.news.model.{Config, ScheduleRecord}

/**
 * class to render shortcut page
 *
 * @author fabian 
 *         on 23.05.14.
 */
class Shortcuts extends Config {

  val allAppointments = ScheduleRecord.findAll.filter(sr=> allClassNamesAsLowercase.contains(sr.className.get.toLowerCase) ).map(sr => (sr.titleShort.value, sr.titleLong.value)).toMap


  def render = {
    <table>
      <caption>Abkürzungsverzeichnis der Lehrveranstaltungen</caption>
      <thead>
        <tr>

        </tr>{allAppointments.keySet.toList.sorted.map {
        shortName =>
          <tr>
            <td style="text-align: right; width:30%">
              <strong>
                {shortName}
              </strong>
            </td>
            <td style="width:50%">
              {allAppointments(shortName)}
            </td>
          </tr>
      }}
      </thead>
    </table>
  }

}
