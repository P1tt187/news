package org.unsane.spirit.news.model

import net.liftweb.mongodb.record
import net.liftweb.mongodb.record.{field => mongoField, MongoRecord, MongoMetaRecord}
import record.field.ObjectIdPk
import net.liftweb.record.field.{IntField, StringField}
import net.liftweb.mongodb.record.field.MongoListField

/**
 * @author fabian 
 * @since 29.03.15.
 *
 *        This Record represents a class and contains their groups
 */
object GroupRecord extends GroupRecord with MongoMetaRecord[GroupRecord]{

}


class GroupRecord extends MongoRecord[GroupRecord] with ObjectIdPk[GroupRecord]{
  def meta = GroupRecord

  object className extends StringField(this, 100)
  object groupType extends StringField(this,100)
  object groupIndex extends IntField(this)

  object students extends mongoField.MongoCaseClassListField[GroupRecord,student](this)
}

case class student(firstName:String,lastName:String)