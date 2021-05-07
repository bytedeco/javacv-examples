package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.{spinNodeHandle, spinNodeMapHandle}
import org.bytedeco.spinnaker.global.Spinnaker_C._

package object helpers {

  /**
    * Read node value assuming it is a string
    *
    * @param hNodeMap the node map where the node is
    * @param nodeName the name of the node
    * @return Node value is available and readable otherwise string "Not readable"
    * @see nodeGetStringValueOpt
    */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def nodeGetStringValue(hNodeMap: spinNodeMapHandle, nodeName: String): String =
    nodeGetStringValueOpt(hNodeMap, nodeName).getOrElse("Not readable")

  /**
    * Read node value assuming it is a string
    *
    * @param hNodeMap the node map where the node is
    * @param nodeName the name of the node
    * @return Option representing read value, option is empty if node is not available or empty
    */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def nodeGetStringValueOpt(hNodeMap: spinNodeMapHandle, nodeName: String): Option[String] = {

    //
    // *** NOTES ***
    // Grabbing node information requires first retrieving the node and
    // then retrieving its information. There are two things to keep in
    // mind. First, a node is distinguished by type, which is related
    // to its value's data type.  Second, nodes should be checked for
    // availability and readability/writability prior to making an
    // attempt to read from or write to the node.
    //


    val MAX_BUFF_LEN = 256
    var err = _spinError.SPINNAKER_ERR_SUCCESS

    val hNodeName = new spinNodeHandle()

    err = spinNodeMapGetNode(hNodeMap, new BytePointer(nodeName), hNodeName)
    check(err, s"Unable to retrieve node handle ('$nodeName').")

    val nodeIsAvailable = new BytePointer(1).putBool(false)
    err = spinNodeIsAvailable(hNodeName, nodeIsAvailable)
    check(err, s"Unable to check node availability ('$nodeName').")

    val nodeIsReadable = new BytePointer(1).putBool(false)
    err = spinNodeIsReadable(hNodeName, nodeIsReadable)
    check(err, s"Unable to check node readability ($nodeName).")

    val pBuff = new BytePointer(MAX_BUFF_LEN)
    val pBuffLen = new SizeTPointer(1).put(MAX_BUFF_LEN)
    if (nodeIsAvailable.getBool && nodeIsReadable.getBool) {
      err = spinStringGetValue(hNodeName, pBuff, pBuffLen)
      check(err, s"Unable to retrieve node value ($nodeName).")
      // Buffer is larger than the string, so we take only up to string length (minus end of string character)
      val value = pBuff.getString().take(pBuffLen.get().toInt - 1)
      Option(value)
    } else {
      None
    }
  }


  private def findErrorNameByValue(value: Int): String = {
    _spinError.values
      .find(_.value == value)
      .map(_.name)
      .getOrElse("???")
  }


  /**
    * Checks if expression evaluated without error code (anything other than SPINNAKER_ERR_SUCCESS).
    * If there was an error an exception is thrown that contains error code and the provided contextual `errorMessage`.
    *
    * @param expr         The error expression to evaluate
    * @param errorMessage Message explaining error context
    * @throws spinnaker_c.helpers.SpinnakerSDKException if error code is not `SPINNAKER_ERR_SUCCESS`
    */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("if error code is not `SPINNAKER_ERR_SUCCESS`")
  def check(expr: _spinError, errorMessage: String): Unit = {
    if (expr.value != _spinError.SPINNAKER_ERR_SUCCESS.value) {
      throw new SpinnakerSDKException(
        "Spinnaker error type : " + expr.toString +
          "\n  Spinnaker error description: " + errorMessage,
        expr
      )
    }
  }

  /**
    * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
    * If it is do nothing otherwise print error information.
    *
    * @param err     error value.
    * @param message additional message to print.
    * @return 'false' if err is not SPINNAKER_ERR_SUCCESS, or 'true' for any other 'err' value.
    */
  def printOnError(err: _spinError, message: String): Boolean = {
    if (err.value != _spinError.SPINNAKER_ERR_SUCCESS.value) {
      println(message)
      println(s"${err.value} ${findErrorNameByValue(err.value)}\n")
      true
    }
    else {
      false
    }
  }

  /**
    * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
    * If it is do nothing otherwise print error description and exit.
    *
    * @param err     error value.
    * @param message additional message to print.
    */
  private[spinnaker_c] def exitOnError(err: _spinError, message: String): Unit = {
    if (printOnError(err, message)) {
      System.out.println("Aborting.")
      System.exit(err.value)
    }
  }
}
