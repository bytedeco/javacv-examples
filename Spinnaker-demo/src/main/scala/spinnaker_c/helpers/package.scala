package spinnaker_c

import org.bytedeco.javacpp.*
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C
import org.bytedeco.spinnaker.global.Spinnaker_C.*

import scala.util.Using.resource
import scala.util.boundary.break
import scala.util.{Using, boundary}

package object helpers {

  val MAX_BUFF_LEN = 256

  def toBytePointer(str: String): BytePointer = new BytePointer(str.length + 2).putString(str)

  /**
   * Retrieves the currently selected entry node from an enum node
   * @param hEnumNode The enum node from which the current entry node is retrieved
   */
  def enumerationGetCurrentEntry(hEnumNode: spinNodeHandle): spinNodeHandle = {
    val phEntry = new spinNodeHandle()
    check(spinEnumerationGetCurrentEntry(hEnumNode, phEntry), "Call to 'spinEnumerationGetCurrentEntry' failed.")
    phEntry
  }

  def enumerationGetEntryByName(node: spinNodeHandle, name: String): spinNodeHandle = {
    val nEntry = new spinNodeHandle()
    Using.Manager { use =>
      check(
        spinEnumerationGetEntryByName(node, use(new BytePointer(name)), nEntry),
        "Could not find requested enumeration entry (" + name + ")"
      )

      if nEntry.isNull then
        nEntry.close()
        throw new SpinnakerSDKException(s"Unrecognised enum entry name '$name'", spinError.SPINNAKER_ERR_NOT_AVAILABLE)

      nEntry
    }.get
  }

  def enumerationGetEntryByIndex(node: spinNodeHandle, index: Long): spinNodeHandle = {
    val nEntry = new spinNodeHandle()
    Using.Manager { use =>
      check(
        spinEnumerationGetEntryByIndex(node, index, nEntry),
        s"Could not find requested enumeration entry ($index)"
      )

      if nEntry.isNull then
        nEntry.close()
        throw new SpinnakerSDKException(
          s"Unrecognised enum entry index '$index'",
          spinError.SPINNAKER_ERR_NOT_AVAILABLE
        )

      nEntry
    }.get
  }

  def enumerationEntryGetIntValue(enumEntry: spinNodeHandle): Long = Using.Manager { use =>
    val enumEntryIntValue = use(new LongPointer(1)).put(0)
    check(
      spinEnumerationEntryGetIntValue(enumEntry, enumEntryIntValue),
      "Failed to retrieve enumeration entry int value"
    )
    enumEntryIntValue.get
  }.get

  def enumerationEntryGetSymbolic(node: spinNodeHandle): String = Using.Manager { use =>
    val currentNodeEntry = use(enumerationGetCurrentEntry(node))
    val pStr             = use(BytePointer(MAX_BUFF_LEN))
    val lenStr           = use(new SizeTPointer(1)).put(MAX_BUFF_LEN)
    spinEnumerationEntryGetSymbolic(currentNodeEntry, pStr, lenStr)
    pStr.getString.take(lenStr.get().toInt - 1)
  }.get

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
  def nodeGetStringValueOpt(hNodeMap: spinNodeMapHandle, nodeName: String): Option[String] = Using.Manager { use =>

    //
    // *** NOTES ***
    // Grabbing node information requires first retrieving the node and
    // then retrieving its information. There are two things to keep in
    // mind. First, a node is distinguished by type, which is related
    // to its value's data type.  Second, nodes should be checked for
    // availability and readability/writability prior to making an
    // attempt to read from or write to the node.
    //

    val hNode = use(new spinNodeHandle())
    check(
      spinNodeMapGetNode(hNodeMap, use(new BytePointer(nodeName)), hNode),
      s"Unable to retrieve node handle ('$nodeName')."
    )

    val nodeIsReadable = use(new BytePointer(1)).putBool(false)
    check(spinNodeIsReadable(hNode, nodeIsReadable), s"Unable to check node readability ($nodeName).")

    if nodeIsReadable.getBool then Option(nodeGetValueAsString(hNode)) else None
  }.get

  def nodeGetValueAsString(hNode: spinNodeHandle): String =
    applyGetStringFunction(hNode, spinStringGetValue, "Value")

  /**
   * Apply a "Get*" method to retrieve a String property of a node.
   *
   * @param hNode node handle to which the function will be applied
   * @param fun function to use to read node property
   * @param name Name of the property read by `fun`, used for error messages only
   * @return String value of the read property
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("if error code is not `SPINNAKER_ERR_SUCCESS`")
  def applyGetStringFunction(
    hNode: spinNodeHandle,
    fun: (spinNodeHandle, BytePointer, SizeTPointer) => Spinnaker_C.spinError,
    name: String
  ): String =
    Using.resources(new BytePointer(MAX_BUFF_LEN), new SizeTPointer(1).put(MAX_BUFF_LEN)): (buf, bufLen) =>
      check(fun(hNode, buf, bufLen), s"Unable to retrieve property $name.")
      buf.getString().take(bufLen.get().toInt - 1)

  def nodeGetMaxLong(hNodeMap: spinNodeMapHandle, nodeName: String): Long = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsReadable(hNode, nodeName)
    val hMax = use(new LongPointer(1)).put(0)
    check(spinIntegerGetMax(hNode, hMax), s"Unable to get max '$nodeName' (max retrieval)")
    hMax.get
  }.get

  def nodeGetMinLong(hNodeMap: spinNodeMapHandle, nodeName: String): Long = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsReadable(hNode, nodeName)
    val hMin = use(new LongPointer(1)).put(0)
    check(spinIntegerGetMax(hNode, hMin), s"Unable to get min '$nodeName' (min retrieval)")
    hMin.get
  }.get

  /**
   * Retrieves a node from the NodeMap by name
   *
   * @param hNodeMap The node map where the node is
   * @param nodeName The name of the node
   * @return The node handle pointer, caller is responsible for closing the handle after use
   */
  def nodeMapGetNode(hNodeMap: spinNodeMapHandle, nodeName: String): spinNodeHandle = Using.Manager { use =>
    val hNode: spinNodeHandle = new spinNodeHandle()
    try
      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer(nodeName)), hNode),
        s"Failed to retrieve node '$nodeName' from the nodeMap'"
      )
      hNode
    catch
      case ex: Throwable =>
        hNode.close()
        throw ex
  }.get

  /**
   * Checks if expression evaluated without error code (anything other than SPINNAKER_ERR_SUCCESS).
   * If there was an error, an exception is thrown that contains error code and the provided contextual `errorMessage`.
   *
   * @param expr         The error expression to evaluate
   * @param errorMessage Message explaining error context
   * @throws spinnaker_c.helpers.SpinnakerSDKException if error code is not `SPINNAKER_ERR_SUCCESS`
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("if error code is not `SPINNAKER_ERR_SUCCESS`")
  def check(expr: spinError, errorMessage: String): Unit =
    if isError(expr) then
      throw new SpinnakerSDKException(
        "Spinnaker error type : " + expr.toString +
          "\n  Spinnaker error description: " + errorMessage,
        expr
      )

  /**
   * Return true is error code means is not an error
   * @param err error code to test
   */
  def isSuccess(err: spinError): Boolean = err.intern() == spinError.SPINNAKER_ERR_SUCCESS

  /**
   * Return true is error code means some error
   * @param err error code to test
   */
  def isError(err: spinError): Boolean = !isSuccess(err)

  @throws[SpinnakerSDKException]
  def checkIsReadable(hNode: spinNodeHandle, nodeName: String): Unit =
    if !isReadable(hNode, nodeName) then
      printRetrieveNodeFailure("node", nodeName)
      throw new SpinnakerSDKException(s"Node '$nodeName' is not readable", spinError.SPINNAKER_ERR_ACCESS_DENIED)

  def isReadable(hNode: spinNodeHandle, nodeName: String): Boolean =
    resource(new BytePointer(1)): pbReadable =>
      val err = spinNodeIsReadable(hNode, pbReadable)
      printOnError(err, "Unable to retrieve node readability (" + nodeName + " node)")
      pbReadable.getBool

  /**
   * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
   * If it is do nothing otherwise print error information.
   *
   * @param err     error value.
   * @param message additional message to print.
   * @return 'false' if err is not SPINNAKER_ERR_SUCCESS, or 'true' for any other 'err' value.
   */
  def printOnError(err: spinError, message: String): Boolean =
    if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS)
      printError(err, message)
      true
    else
      false

  def printError(err: spinError, message: String): Unit =
    println(message)
    println(s"${err.value} ${findErrorNameByValue(err.value)}\n")

  def findErrorNameByValue(value: Int): String =
    spinError.values
      .find(_.value == value)
      .map(_.name)
      .getOrElse("???")

  /**
   * This function handles the error prints when a node or entry is unavailable or
   * not readable/writable on the connected camera
   */
  def printRetrieveNodeFailure(node: String, name: String): Unit =
    println("Unable to get " + node + " (" + name + " " + node + " retrieval failed).")
    println("The " + node + " may not be available on all camera models...")
    println("Please try a Blackfly S camera.\n")

  def nodeGetIncLong(hNodeMap: spinNodeMapHandle, nodeName: String): Long = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsReadable(hNode, nodeName)
    val hInc = use(new LongPointer(1)).put(0)
    check(spinIntegerGetInc(hNode, hInc), s"Unable to get inc '$nodeName' (inc retrieval)")
    hInc.get
  }.get

  def nodeGetMaxDouble(hNodeMap: spinNodeMapHandle, nodeName: String): Double = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsReadable(hNode, nodeName)
    val hMax = use(new DoublePointer(1)).put(0)
    check(spinFloatGetMax(hNode, hMax), s"Unable to get max '$nodeName' (max retrieval)")
    hMax.get
  }.get

  def nodeGetMinDouble(hNodeMap: spinNodeMapHandle, nodeName: String): Double = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsReadable(hNode, nodeName)
    val hMin = use(new DoublePointer(1)).put(0)
    check(spinFloatGetMin(hNode, hMin), s"Unable to get min '$nodeName' (min retrieval)")
    hMin.get
  }.get

  def booleanSetValue(hNodeMap: spinNodeMapHandle, nodeName: String, value: Boolean): Unit = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsWritable(hNode, nodeName)
    val bValue = (if value then 1 else 0).toByte
    check(spinBooleanSetValue(hNode, bValue), s"Failed to set value of node '$nodeName' to $value")
  }.get

  def integerSetValue(hNodeMap: spinNodeMapHandle, nodeName: String, value: Long): Unit = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsWritable(hNode, nodeName)
    check(spinIntegerSetValue(hNode, value), s"Failed to set value of node '$nodeName' to $value")
  }.get

  def floatSetValue(hNodeMap: spinNodeMapHandle, nodeName: String, value: Double): Unit = Using.Manager { use =>
    val hNode = use(nodeMapGetNode(hNodeMap, nodeName))
    checkIsWritable(hNode, nodeName)
    check(spinFloatSetValue(hNode, value), s"Failed to set value of node '$nodeName' to $value")
  }.get

  @throws[SpinnakerSDKException]
  def checkIsWritable(hNode: spinNodeHandle, nodeName: String): Unit =
    if !isReadable(hNode, nodeName) then
      printRetrieveNodeFailure("node", nodeName)
      throw new SpinnakerSDKException(s"Node '$nodeName' is not writable", spinError.SPINNAKER_ERR_ACCESS_DENIED)

  def nodeName(hNode: spinNodeHandle): String =
    applyGetStringFunction(hNode, spinNodeGetName, "Name")

  def printDeviceInfo(hCam: spinCamera): spinError = Using.Manager { use =>
    // Retrieve nodemap
    val hNodeMapTLDevice = use(new spinNodeMapHandle())
    check(spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice), "Unable to retrieve TL device nodemap .")

    printDeviceInfo(hNodeMapTLDevice)
  }.get

  /**
   * This function prints the device information of the camera from the transport
   * layer; please see NodeMapInfo_C example for more in-depth comments on
   * printing device information from the nodemap.
   */
  def printDeviceInfo(hNodeMap: spinNodeMapHandle): spinError = Using.Manager { use =>
    var err = spinError.SPINNAKER_ERR_SUCCESS

    println("\n*** DEVICE INFORMATION ***\n")
    // Retrieve device information category node
    val hDeviceInformation = use(new spinNodeHandle)
    err = spinNodeMapGetNode(hNodeMap, new BytePointer("DeviceInformation"), hDeviceInformation)
    printOnError(err, "Unable to retrieve node.")

    // Retrieve number of nodes within device information node
    val numFeatures = use(new SizeTPointer(1))
    if (isAvailableAndReadable(hDeviceInformation, "DeviceInformation")) {
      err = spinCategoryGetNumFeatures(hDeviceInformation, numFeatures)
      printOnError(err, "Unable to retrieve number of nodes.")
    } else {
      printRetrieveNodeFailure("node", "DeviceInformation")
      return spinError.SPINNAKER_ERR_ACCESS_DENIED
    }

    // Iterate through nodes and print information
    for (i <- 0 until numFeatures.get.toInt) {
      val hFeatureNode = use(new spinNodeHandle)
      err = spinCategoryGetFeatureByIndex(hDeviceInformation, i, hFeatureNode)
      printOnError(err, "Unable to retrieve node.")

      // get feature node name
      val featureName    = use(new BytePointer(MAX_BUFF_LEN))
      val lenFeatureName = use(new SizeTPointer(1))
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
        val featureValue    = use(new BytePointer(MAX_BUFF_LEN))
        val lenFeatureValue = use(new SizeTPointer(1))
        lenFeatureValue.put(MAX_BUFF_LEN)
        err = spinNodeToString(hFeatureNode, featureValue, lenFeatureValue)
        if (printOnError(err, "spinNodeToString"))
          featureValue.putString("Unknown value")
        println(featureName.getString.trim + ": " + featureValue.getString.trim + ".")
      }
    }
    println()
    err
  }.get

  def isAvailable(hNode: spinNodeHandle, nodeName: String): Boolean =
    resource(new BytePointer(1)): pbAvailable =>
      val err = spinNodeIsAvailable(hNode, pbAvailable)
      printOnError(err, "Unable to retrieve node availability (" + nodeName + " node)")
      pbAvailable.getBool

  def isAvailableAndReadable(hNode: spinNodeHandle, nodeName: String): Boolean = Using.Manager { use =>
    val pbAvailable = use(new BytePointer(1))
    var err         = spinError.SPINNAKER_ERR_SUCCESS
    err = spinNodeIsAvailable(hNode, pbAvailable)
    printOnError(err, "Unable to retrieve node availability (" + nodeName + " node)")

    val pbReadable = use(new BytePointer(1))
    err = spinNodeIsReadable(hNode, pbReadable)
    printOnError(err, "Unable to retrieve node readability (" + nodeName + " node)")
    pbReadable.getBool && pbAvailable.getBool
  }.get

  /**
   * This function helps to check if a node is available and writable
   */
  def isWritable(hNode: spinNodeHandle, nodeName: String): Boolean =
    resource(new BytePointer(1)): pbWritable =>
      val err = spinNodeIsWritable(hNode, pbWritable)
      printOnError(err, "Unable to retrieve node writability (" + nodeName + " node).")

      pbWritable.getBool

  def printLibraryVersion(hSystem: spinSystem): Unit =
    resource(new spinLibraryVersion()): hLibraryVersion =>
      spinSystemGetLibraryVersion(hSystem, hLibraryVersion)
      printf(
        "Spinnaker library version: %d.%d.%d.%d\n\n%n",
        hLibraryVersion.major(),
        hLibraryVersion.minor(),
        hLibraryVersion.`type`(),
        hLibraryVersion.build()
      )

  def findImageStatusNameByValue(value: Int): String =
    boundary:
      for (v <- spinImageStatus.values) do if v.value == value then break(v.name)
      "???"

  /**
   * Check if 'err' is equal to 'SPINNAKER_ERR_SUCCESS'.
   * If it is, do nothing otherwise print error description and exit.
   *
   * @param err     error value.
   * @param message additional message to print.
   */
  def exitOnError(err: spinError, message: String): Unit =
    if (printOnError(err, message))
      System.out.println("Aborting.")
      System.exit(err.value)

  def setEnumerationNodeValue(hNodeMap: spinNodeMapHandle, enumNodeName: String, enumEntryName: String): Unit =
    Using.Manager { use =>
      val hNode = use(new spinNodeHandle())
      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer(enumNodeName)), hNode),
        s"Unable to get node from $hNodeMap ('$enumNodeName' node retrieval failed)."
      )
      if (!isReadable(hNode, enumNodeName))
        throw new Exception(s"Node '$enumNodeName' is not readable'")

      val hNodeEntry = use(new spinNodeHandle())
      check(
        spinEnumerationGetEntryByName(hNode, use(new BytePointer(enumEntryName)), hNodeEntry),
        s"'spinEnumerationGetEntryByName(..., '$enumEntryName')' failed."
      )
      if (!isReadable(hNodeEntry, enumEntryName))
        throw new Exception(s"Node's '$enumNodeName' entry '$enumEntryName' is not readable'")

      val enumEntryNameID = use(new LongPointer(1)).put(0)
      check(
        spinEnumerationEntryGetIntValue(hNodeEntry, enumEntryNameID),
        s"'spinEnumerationEntryGetIntValue' failed for node's '$enumNodeName' entry '$enumEntryName'"
      )

      if (!isWritable(hNode, enumNodeName))
        throw new Exception(s"Node '$enumNodeName' is not writable'")

      check(
        spinEnumerationSetIntValue(hNode, enumEntryNameID.get),
        s"spinEnumerationSetIntValue' failed for node '$enumNodeName'"
      )
    }.get

  def imageChunkDataGetFloatValue(hImage: spinImage, name: String): Double = Using.Manager { use =>
    val pValue = use(new DoublePointer(1)).put(0)
    check(
      spinImageChunkDataGetFloatValue(hImage, use(new BytePointer(name)), pValue),
      "Failed to get ImageChunkDataGetFloatValue"
    )
    pValue.get()
  }.get

  def imageChunkDataGetIntValue(hImage: spinImage, name: String): Long = Using.Manager { use =>
    val pValue = use(new LongPointer(1)).put(0)
    check(
      spinImageChunkDataGetIntValue(hImage, use(new BytePointer(name)), pValue),
      "Failed to get spinImageChunkDataGetIntValue"
    )
    pValue.get()
  }.get

}
