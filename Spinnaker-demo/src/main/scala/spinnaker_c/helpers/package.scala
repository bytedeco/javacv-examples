package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.{spinNodeHandle, spinNodeMapHandle}
import org.bytedeco.spinnaker.global.Spinnaker_C.*

import scala.util.boundary
import scala.util.boundary.break

package object helpers {

  private val MAX_BUFF_LEN = 256

  def toBytePointer(str: String): BytePointer = new BytePointer(str.length + 2).putString(str)

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
    var err          = spinError.SPINNAKER_ERR_SUCCESS

    val hNodeName = new spinNodeHandle()

    err = spinNodeMapGetNode(hNodeMap, new BytePointer(nodeName), hNodeName)
    check(err, s"Unable to retrieve node handle ('$nodeName').")

    val nodeIsAvailable = new BytePointer(1).putBool(false)
    err = spinNodeIsAvailable(hNodeName, nodeIsAvailable)
    check(err, s"Unable to check node availability ('$nodeName').")

    val nodeIsReadable = new BytePointer(1).putBool(false)
    err = spinNodeIsReadable(hNodeName, nodeIsReadable)
    check(err, s"Unable to check node readability ($nodeName).")

    val pBuff    = new BytePointer(MAX_BUFF_LEN)
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

  /**
   * Checks if expression evaluated without error code (anything other than SPINNAKER_ERR_SUCCESS).
   * If there was an error an exception is thrown that contains error code and the provided contextual `errorMessage`.
   *
   * @param expr         The error expression to evaluate
   * @param errorMessage Message explaining error context
   * @throws spinnaker_c.helpers.SpinnakerSDKException if error code is not `SPINNAKER_ERR_SUCCESS`
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("if error code is not `SPINNAKER_ERR_SUCCESS`")
  def check(expr: spinError, errorMessage: String): Unit = {
    if (expr.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
      throw new SpinnakerSDKException(
        "Spinnaker error type : " + expr.toString +
          "\n  Spinnaker error description: " + errorMessage,
        expr
      )
    }
  }

  /**
   * This function prints the device information of the camera from the transport
   * layer; please see NodeMapInfo_C example for more in-depth comments on
   * printing device information from the nodemap.
   */
  def printDeviceInfo(hNodeMap: spinNodeMapHandle): spinError = {
    var err = spinError.SPINNAKER_ERR_SUCCESS

    System.out.println("\n*** DEVICE INFORMATION ***\n")
    // Retrieve device information category node
    val hDeviceInformation = new spinNodeHandle
    err = spinNodeMapGetNode(hNodeMap, new BytePointer("DeviceInformation"), hDeviceInformation)
    printOnError(err, "Unable to retrieve node.")

    // Retrieve number of nodes within device information node
    val numFeatures = new SizeTPointer(1)
    if (isAvailableAndReadable(hDeviceInformation, "DeviceInformation")) {
      err = spinCategoryGetNumFeatures(hDeviceInformation, numFeatures)
      printOnError(err, "Unable to retrieve number of nodes.")
    } else {
      printRetrieveNodeFailure("node", "DeviceInformation")
      return spinError.SPINNAKER_ERR_ACCESS_DENIED
    }

    // Iterate through nodes and print information
    for (i <- 0 until numFeatures.get.toInt) {
      val hFeatureNode = new spinNodeHandle
      err = spinCategoryGetFeatureByIndex(hDeviceInformation, i, hFeatureNode)
      printOnError(err, "Unable to retrieve node.")

      // get feature node name
      val featureName    = new BytePointer(MAX_BUFF_LEN)
      val lenFeatureName = new SizeTPointer(1)
      lenFeatureName.put(MAX_BUFF_LEN)
      err = spinNodeGetName(hFeatureNode, featureName, lenFeatureName)
      if (printOnError(err, "Error retrieving node name."))
        featureName.putString("Unknown name")

      val featureType = Array(spinNodeType.UnknownNode.value)
      var skipRest    = false
      if (isAvailableAndReadable(hFeatureNode, featureName.getString)) {
        err = spinNodeGetType(hFeatureNode, featureType)
        if (printOnError(err, "Unable to retrieve node type."))
          skipRest = true
      } else {
        println(s"$featureName: Node not readable")
        skipRest = true
      }

      if (!skipRest) {
        val featureValue    = new BytePointer(MAX_BUFF_LEN)
        val lenFeatureValue = new SizeTPointer(1)
        lenFeatureValue.put(MAX_BUFF_LEN)
        err = spinNodeToString(hFeatureNode, featureValue, lenFeatureValue)
        if (printOnError(err, "spinNodeToString"))
          featureValue.putString("Unknown value")
        println(featureName.getString.trim + ": " + featureValue.getString.trim + ".")
      }
    }
    println()
    err
  }

  def isAvailable(hNode: spinNodeHandle, nodeName: String): Boolean =
    val pbAvailable = new BytePointer(1)
    val err         = spinNodeIsAvailable(hNode, pbAvailable)
    printOnError(err, "Unable to retrieve node availability (" + nodeName + " node)")
    pbAvailable.getBool

  def isReadable(hNode: spinNodeHandle, nodeName: String): Boolean =
    val pbReadable = new BytePointer(1)
    val err        = spinNodeIsReadable(hNode, pbReadable)
    printOnError(err, "Unable to retrieve node readability (" + nodeName + " node)")
    pbReadable.getBool

  def isAvailableAndReadable(hNode: spinNodeHandle, nodeName: String): Boolean = {
    val pbAvailable = new BytePointer(1)
    var err         = spinError.SPINNAKER_ERR_SUCCESS
    err = spinNodeIsAvailable(hNode, pbAvailable)
    printOnError(err, "Unable to retrieve node availability (" + nodeName + " node)")

    val pbReadable = new BytePointer(1)
    err = spinNodeIsReadable(hNode, pbReadable)
    printOnError(err, "Unable to retrieve node readability (" + nodeName + " node)")
    pbReadable.getBool && pbAvailable.getBool
  }

  /**
   * This function helps to check if a node is available and writable
   */
  def isWritable(hNode: spinNodeHandle, nodeName: String): Boolean = {
    val pbWritable = new BytePointer(1)
    val err        = spinNodeIsWritable(hNode, pbWritable)
    printOnError(err, "Unable to retrieve node writability (" + nodeName + " node).")

    pbWritable.getBool
  }

  /**
   * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
   * If it is do nothing otherwise print error information.
   *
   * @param err     error value.
   * @param message additional message to print.
   * @return 'false' if err is not SPINNAKER_ERR_SUCCESS, or 'true' for any other 'err' value.
   */
  def printOnError(err: spinError, message: String): Boolean = {
    if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
      printError(err, message)
      true
    } else {
      false
    }
  }

  def printError(err: spinError, message: String): Unit = {
    println(message)
    println(s"${err.value} ${findErrorNameByValue(err.value)}\n")
  }

  /**
   * This function handles the error prints when a node or entry is unavailable or
   * not readable/writable on the connected camera
   */
  def printRetrieveNodeFailure(node: String, name: String): Unit = {
    println("Unable to get " + node + " (" + name + " " + node + " retrieval failed).")
    println("The " + node + " may not be available on all camera models...")
    println("Please try a Blackfly S camera.\n")
  }

  def findErrorNameByValue(value: Int): String = {
    spinError.values
      .find(_.value == value)
      .map(_.name)
      .getOrElse("???")
  }

  def findImageStatusNameByValue(value: Int): String =
    boundary:
      for (v <- spinImageStatus.values) do if v.value == value then break(v.name)
      "???"

  /**
   * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
   * If it is do nothing otherwise print error description and exit.
   *
   * @param err     error value.
   * @param message additional message to print.
   */
  private[spinnaker_c] def exitOnError(err: spinError, message: String): Unit = {
    if (printOnError(err, message)) {
      System.out.println("Aborting.")
      System.exit(err.value)
    }
  }
}
