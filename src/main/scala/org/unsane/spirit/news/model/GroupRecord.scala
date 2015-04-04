package org.unsane.spirit.news.model

import net.liftweb.mongodb.record.field.ObjectIdPk
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord, field => mongoField}
import net.liftweb.record.field.{IntField, StringField}

/**
 * @author fabian 
 * @since 29.03.15.
 *
 *        This Record represents a class and contains their groups
 */
object GroupRecord extends GroupRecord with MongoMetaRecord[GroupRecord] {

}


class GroupRecord extends MongoRecord[GroupRecord] with ObjectIdPk[GroupRecord] {
  def meta = GroupRecord

  object className extends StringField(this, 100)

  object groupType extends StringField(this, 100)

  object groupIndex extends IntField(this)

  object students extends mongoField.MongoCaseClassListField[GroupRecord, student](this)

}

case class student(firstName: String, lastName: String)

/**
Data Structure:
  <code>
  {
   "_id":{
      "$oid":"5520128bd344840f1dd1a0d8"
   },
   "groupType":"2 Gruppen",
   "students":[
      {
         "firstName":"Bart",
         "lastName":"Simpson"
      },
      {
         "firstName":"Homer",
         "lastName":"Simpson"
      }
   ],
   "groupIndex":1,
   "className":"bais2"
}

  </code>
  * */