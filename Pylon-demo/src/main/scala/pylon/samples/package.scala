package pylon

import org.bytedeco.javacpp.GenICam3.gcstring

/**
  * Helper methods for working with Pylon data structures.
  */
package object samples {

  def asGCString(s: String): gcstring = {
    val res = new gcstring()
    res.assign(s)
    res
  }

  def asString(s: gcstring): String = s.c_str().getString

}
