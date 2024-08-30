package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, DoublePointer, LongPointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import spinnaker_c.NodeMapInfo_C.ReadType.{INDIVIDUAL, VALUE}
import spinnaker_c.helpers.{check, exitOnError, helpStringGetValue, isReadable}

import scala.util.Using

/**
 *  NodeMapInfo_C shows how to retrieve node map information. It
 *  relies on information provided in the Enumeration_C example. Following this,
 *  check out the Acquisition_C example if you haven't already. It explores
 *  acquiring images.
 *
 *  This example explores retrieving information from all major node types on the
 *  camera. This includes string, integer, float, boolean, command, enumeration,
 *  category, and value types. Looping through multiple child nodes is also
 *  covered. A few node types are not covered - base, port, and register - as
 *  they are not fundamental. The final node type - enumeration entry - is
 *  explored only in terms of its parent node type - enumeration.
 *
 *  Once comfortable with NodeMapInfo_C, we suggest checking out
 *  ImageFormatControl_C and Exposure_C. ImageFormatControl_C explores
 *  customizing image settings on a camera while Exposure_C introduces the
 *  standard structure of configuring a device, acquiring some images, and then
 *  returning the device to a default state.
 */
object NodeMapInfo_C {

  private val MAX_BUFF_LEN = 256
  // Use the following enum and global constant to select whether nodes are read
  // as 'value' nodes or their individual types.
  private val ChosenRead = ReadType.VALUE
  private val MAX_CHARS  = 80

  /**
   * Example entry point; please see Enumeration_C example for more in-depth
   * comments on preparing and cleaning up the system.
   */
  def main(args: Array[String]): Unit = {

    // Retrieve singleton reference to system object
    Using(new spinSystem()) { hSystem =>
      exitOnError(
        spinSystemGetInstance(hSystem),
        "Unable to retrieve system instance."
      )

      // Print out current library version
      Using(new spinLibraryVersion()) { hLibraryVersion =>
        spinSystemGetLibraryVersion(hSystem, hLibraryVersion)
        printf(
          "Spinnaker library version: %d.%d.%d.%d\n\n%n",
          hLibraryVersion.major(),
          hLibraryVersion.minor(),
          hLibraryVersion.`type`(),
          hLibraryVersion.build()
        )
      }

      // Retrieve list of cameras from the system
      Using(new spinCameraList()) { hCameraList =>
        exitOnError(
          spinCameraListCreateEmpty(hCameraList),
          "Unable to create camera list."
        )

        exitOnError(
          spinSystemGetCameras(hSystem, hCameraList),
          "Unable to retrieve camera list."
        )

        // Retrieve number of cameras
        val numCameras =
          Using.resource(new SizeTPointer(1)) { numCamerasPtr =>
            exitOnError(
              spinCameraListGetSize(hCameraList, numCamerasPtr),
              "Unable to retrieve number of cameras."
            )
            numCamerasPtr.get().toInt
          }
        println("Number of cameras detected: " + numCameras + "\n")

        for i <- 0 until numCameras do {
          println(s"\nRunning example for camera $i...\n")

          Using(new spinCamera()) { hCamera =>
            check(
              spinCameraListGet(hCameraList, i, hCamera),
              s"Unable to retrieve camera $i from list"
            )

            // Run example
            runSingleCamera(hCamera)

            check(
              spinCameraRelease(hCamera),
              s"Unable to release camera $i"
            )
          }

        }

        //
        // Clear and destroy camera list before releasing system
        //
        exitOnError(
          spinCameraListClear(hCameraList),
          "Unable to clear camera list."
        )

        exitOnError(
          spinCameraListDestroy(hCameraList),
          "Unable to destroy camera list."
        )
      }

      //
      // Release system
      //
      // *** NOTES ***
      // The system should be released, but if it is not, it will do so itself.
      // It is often at the release of the system (whether manual or automatic)
      // that unbroken references and still registered events will throw an
      // exception.
      //
      exitOnError(
        spinSystemReleaseInstance(hSystem),
        "Unable to release system instance."
      )
    }

    println("\nDone!\n")
  }

  def runSingleCamera(hCam: spinCamera): Unit = {

    val level = 0

    //
    // Retrieve TL device nodemap
    //
    // *** NOTES ***
    // The TL device nodemap is available on the transport layer. As such,
    // camera initialization is unnecessary. It provides mostly immutable
    // information fundamental to the camera such as the serial number,
    // vendor, and model.
    //
    println("\n*** PRINTING TL DEVICE NODEMAP ***\n\n")

    Using.resources(new spinNodeMapHandle(), new spinNodeHandle()) { (hNodeMapTLDevice, hTLDeviceRoot) =>
      // Retrieve nodemap from camera
      check(
        spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice),
        "Unable to print TL device nodemap (nodemap retrieval)"
      )

      // Retrieve root node from nodemap
      check(
        _spinNodeMapGetNode(hNodeMapTLDevice, "Root", hTLDeviceRoot),
        "Unable to print TL device nodemap (root node retrieval)"
      )

      // Print values recursively
      printCategoryNodeAndAllFeatures(hTLDeviceRoot, level)
    }

    //
    // Retrieve TL stream nodemap
    //
    // *** NOTES ***
    // The TL stream nodemap is also available on the transport layer. Camera
    // initialization is again unnecessary. As you can probably guess, it
    // provides information on the camera's streaming performance at any
    // given moment. Having this information available on the transport
    // layer allows the information to be retrieved without affecting camera
    // performance.
    //
    println("*** PRINTING TL STREAM NODEMAP ***\n")

    Using.resources(new spinNodeMapHandle(), new spinNodeHandle()) { (hNodeMapStream, hStreamRoot) =>
      check(
        spinCameraGetTLStreamNodeMap(hCam, hNodeMapStream),
        "Unable to print TL stream nodemap (nodemap retrieval)."
      )

      // Retrieve root node from nodemap
      check(
        _spinNodeMapGetNode(hNodeMapStream, "Root", hStreamRoot),
        "Failed to call 'spinNodeMapGetNode'"
      )

      printCategoryNodeAndAllFeatures(hStreamRoot, level)
    }

    //
    // Initialize camera
    //
    // *** NOTES ***
    // The camera becomes connected upon initialization. This provides
    // access to configurable options and additional information, accessible
    // through the GenICam nodemap.
    //
    // *** LATER ***
    // Cameras should be deinitialized when no longer needed.
    //
    check(
      spinCameraInit(hCam),
      "Unable to initialize camera."
    )

    //
    // Retrieve GenICam nodemap
    //
    // *** NOTES ***
    // The GenICam nodemap is the primary gateway to customizing and
    // configuring the camera to suit your needs. Configuration options such
    // as image height and width, trigger mode enabling and disabling, and the
    // sequencer are found on this nodemap.
    //
    println("*** PRINTING GENICAM NODEMAP ***\n")

    Using.resources(new spinNodeMapHandle(), new spinNodeHandle()) { (hNodeMap, hRoot) =>

      // Retrieve nodemap from camera
      check(
        spinCameraGetNodeMap(hCam, hNodeMap),
        "Unable to print GenICam nodemap (nodemap retrieval)."
      )

      // Retrieve root node from nodemap
      check(
        _spinNodeMapGetNode(hNodeMap, "Root", hRoot),
        "Unable to print GenICam nodemap (root node retrieval)."
      )

      // Print values recursively
      printCategoryNodeAndAllFeatures(hRoot, level)
    }

    //
    // Deinitialize camera
    //
    // *** NOTES ***
    // Camera deinitialization helps ensure that devices clean up properly
    // and do not need to be power-cycled to maintain integrity.
    //
    check(
      spinCameraDeInit(hCam),
      "Unable to deinitialize camera."
    )

  }

  private def _spinNodeMapGetNode(hNodeMap: spinNodeMapHandle, name: String, phNode: spinNodeHandle): spinError =
    Using.resource(new BytePointer(name)): pName =>
      spinNodeMapGetNode(hNodeMap, pName, phNode)

  private def printCategoryNodeAndAllFeatures(hCategoryNode: spinNodeHandle, level: Int): Unit = {

    // Retrieve display name
    val displayName = helpStringGetValue(hCategoryNode, spinNodeGetDisplayName, "DisplayName")

    indent(level)
    println(displayName)

    //
    // Retrieve number of children
    //
    // *** NOTES ***
    // The two nodes that typically have children are category nodes and
    // enumeration nodes. Throughout the examples, the children of category
    // nodes are referred to as features while the children of enumeration
    // nodes are referred to as entries. Further, it might be important to
    // note that enumeration nodes can be cast as category nodes, but
    // category nodes cannot be cast as enumeration nodes.
    //

    Using(new SizeTPointer(1).put(0)) { numberOfFeatures =>

      check(
        spinCategoryGetNumFeatures(hCategoryNode, numberOfFeatures),
        "Failed to call 'spinCategoryGetNumFeatures'"
      )

      //
      // Iterate through all children
      //
      // *** NOTES ***
      // It is important to note that the children of an enumeration nodes
      // may be of any node type.
      //
      for i <- 0 until numberOfFeatures.get().toInt do {

        Using(new spinNodeHandle()) { hFeatureNode =>
          // Retrieve child
          check(
            spinCategoryGetFeatureByIndex(hCategoryNode, i, hFeatureNode),
            "Failed to call 'spinCategoryGetFeatureByIndex'"
          )

          if isReadable(hFeatureNode, "?FeatureNode?") then {
            val nodeType = Array(spinNodeType.UnknownNode.value)
            check(
              spinNodeGetType(hFeatureNode, nodeType),
              "Failed to call 'spinNodeGetType'"
            )

            // Category nodes must be dealt with separately in order to
            // retrieve subnodes recursively.
            if nodeType(0) == spinNodeType.CategoryNode.value then
              printCategoryNodeAndAllFeatures(hFeatureNode, level + 1)
            else
              ChosenRead match
                case VALUE =>
                  printValueNode(hFeatureNode, level + 1)
                case INDIVIDUAL =>
                  nodeType(0) match
                    case spinNodeType.StringNode.value =>
                      printStringNode(hFeatureNode, level + 1)

                    case spinNodeType.IntegerNode.value =>
                      printIntegerNode(hFeatureNode, level + 1)

                    case spinNodeType.FloatNode.value =>
                      printFloatNode(hFeatureNode, level + 1)

                    case spinNodeType.BooleanNode.value =>
                      printBooleanNode(hFeatureNode, level + 1)

                    case spinNodeType.CommandNode.value =>
                      printCommandNode(hFeatureNode, level + 1)

                    case spinNodeType.EnumerationNode.value =>
                      printEnumerationNodeAndCurrentEntry(hFeatureNode, level + 1)

                    //              case ValueNode =>
                    //              case BaseNode =>
                    //              case RegisterNode =>
                    //              case EnumEntryNode =>
                    //              case CategoryNode =>
                    //              case PortNode =>
                    //              case UnknownNode =>
                    case x =>
                      println(s"Node type: $x not supported")
          }
        }
      }
    }
    println()
  }

  /**
   * This function retrieves and prints the display names of an enumeration node
   * and its current entry (which is actually housed in another node unto itself)
   */
  private def printEnumerationNodeAndCurrentEntry(hEnumerationNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hEnumerationNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve current entry node
    //
    // *** NOTES ***
    // Returning the current entry of an enumeration node delivers the entry
    // node rather than the integer value or symbolic. The current entry's
    // integer and symbolic need to be retrieved from the entry node because
    // they cannot be directly accessed through the enumeration node in C.
    //
    Using(new spinNodeHandle()) { hCurrentEntryNode =>

      check(
        spinEnumerationGetCurrentEntry(hEnumerationNode, hCurrentEntryNode),
        "Failed to call 'spinEnumerationGetCurrentEntry'"
      )

      //
      // Retrieve current symbolic
      //
      // *** NOTES ***
      // Rather than retrieving the current entry node and then retrieving its
      // symbolic, this could have been taken care of in one step by using the
      // enumeration node's ToString() method.
      //
      val currentEntrySymbolic =
        Using.resources(new BytePointer(MAX_BUFF_LEN), new SizeTPointer(1).put(MAX_BUFF_LEN)) {
          (buf, bufLen) =>
            spinEnumerationEntryGetSymbolic(hCurrentEntryNode, buf, bufLen)

            buf.getString().take(bufLen.get().toInt - 1)
        }

      // Print current entry symbolic
      indent(level)
      println(s"$displayName: $currentEntrySymbolic")
    }
  }

  /**
   * Retrieves and prints the display name and tooltip of a command
   * node, limiting the number of printed characters to a macro-defined maximum.
   * The tooltip is printed below as command nodes do not have an intelligible
   * value.
   */
  private def printCommandNode(hNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve float node value
    //
    // *** NOTES ***
    // Please take note that floating point numbers in the Spinnaker SDK are
    // almost always represented by the larger data type double rather than
    // float.
    //
    Using.resources(new BytePointer(MAX_BUFF_LEN), new SizeTPointer(1).put(MAX_BUFF_LEN)) {
      (buf, bufLen) =>

        check(
          spinNodeGetToolTip(hNode, buf, bufLen),
          "Failed to call 'spinNodeGetToolTip'"
        )

        indent(level)
        print(displayName + ": ")

        val v = buf.getString().take(bufLen.get().toInt - 1)
        if (bufLen.get() > MAX_CHARS)
          println(v.take(MAX_CHARS) + "...")
        else
          println(v)
    }

  }

  /**
   * Retrieves and prints the display name and value of a boolean,
   * printing "true" for true and "false" for false rather than the corresponding
   * integer value ('1' and '0', respectively).
   */
  private def printBooleanNode(hNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    Using(new BytePointer()) { ptr =>
      check(
        spinBooleanGetValue(hNode, ptr),
        "Failed to call 'spinBooleanGetValue'"
      )

      // Print value
      indent(level)
      println(s"$displayName: ${ptr.getBool}")
    }
  }

  /**
   * Retrieves and prints the display name and value of an float node.
   */
  private def printFloatNode(hNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve float node value
    //
    // *** NOTES ***
    // Please take note that floating point numbers in the Spinnaker SDK are
    // almost always represented by the larger data type double rather than
    // float.
    //

    Using(new DoublePointer()) { ptr =>
      check(
        spinFloatGetValue(hNode, ptr),
        "Failed to call 'spinFloatGetValue'"
      )

      // Print value
      indent(level)
      println(s"$displayName: ${ptr.get()}")
    }
  }

  /**
   * Retrieves and prints the display name and value of an integer node.
   */
  private def printIntegerNode(hNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve integer node value
    //
    // *** NOTES ***
    // Keep in mind that the data type of an integer node value is an
    // int64_t as opposed to a standard int. While it is true that the two
    // are often interchangeable, it is recommended to use the int64_t
    // to avoid the introduction of bugs into software built with the
    // Spinnaker SDK.
    //

    Using(new LongPointer()) { integerValue =>
      check(
        spinIntegerGetValue(hNode, integerValue),
        "Failed to call 'spinIntegerGetValue'"
      )

      // Print value
      indent(level)
      println(s"$displayName: ${integerValue.get()}")
    }
  }

  /**
   * Retrieves and prints the display name and value of a string
   * node, limiting the number of printed characters to a maximum defined
   * by MAX_CHARS.
   */
  private def printStringNode(hNode: spinNodeHandle, level: Int): Unit = {
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve string node value
    //
    // *** NOTES ***
    // The Spinnaker SDK requires a character array to hold the string and
    // an integer for the number of characters. Ensure that the size of the
    // character array is large enough to hold the entire string.
    //
    // Throughout the examples in C, 256 is typically used as the size of a
    // character array. This will typically be sufficient, but not always.
    // For instance, a lookup table register node (which is not explored in
    // this example) may be much larger.
    //
    Using.resources(new BytePointer(MAX_BUFF_LEN), new SizeTPointer(1).put(MAX_BUFF_LEN)) {
      (buf, bufLen) =>

        // Ensure allocated buffer is large enough for storing the string
        check(
          spinStringGetValue(hNode, null.asInstanceOf[BytePointer], bufLen),
          "Failed to call 'spinStringGetValue'"
        )

        val k_maxChars = MAX_CHARS
        if bufLen.get() <= k_maxChars then {
          check(
            spinNodeToString(hNode, buf, bufLen),
            "Failed to call 'spinNodeToString'"
          )
          buf.getString().take(bufLen.get().toInt - 1)
        }

        indent(level)
        print(displayName + ": ")
        // Ensure that the value length is not excessive for printing
        val v = buf.getString().take(bufLen.get().toInt - 1)
        if (bufLen.get() > k_maxChars)
          //          println(v.take(k_maxChars) + "...")
          println("...")
        else
          println(v)

    }
  }

  private def printValueNode(hNode: spinNodeHandle, level: Int): Unit = {
    //
    // Retrieve display name
    //
    // *** NOTES ***
    // A node's 'display name' is generally more appropriate for output and
    // user interaction whereas its 'name' is what the camera understands.
    // Generally, its name is the same as its display namebut without
    // spaces - for instance, the name of the node that houses a camera's
    // serial number is 'DeviceSerialNumber' while its display name is
    // 'Device Serial Number'.
    //
    val displayName = helpStringGetValue(hNode, spinNodeGetDisplayName, "DisplayName")

    //
    // Retrieve value of any node type as string
    //
    // *** NOTES ***
    // Because value nodes return any node type as a string, it can be much
    // easier to deal with nodes as value nodes rather than their actual
    // individual types.
    //
    Using.resources(new BytePointer(MAX_BUFF_LEN), new SizeTPointer(1).put(MAX_BUFF_LEN)) {
      (buf, bufLen) =>
        // Ensure allocated buffer is large enough for storing the string
        check(
          spinNodeToString(hNode, null.asInstanceOf[BytePointer], bufLen),
          "Failed to call 'spinNodeToString'"
        )

        val v = if bufLen.get() <= MAX_CHARS then {
          check(
            spinNodeToString(hNode, buf, bufLen),
            "Failed to call 'spinNodeToString'"
          )
          buf.getString().take(bufLen.get().toInt - 1)
        } else ""

        indent(level)
        print(displayName + ": ")
        // Ensure that the value length is not excessive for printing
        if (bufLen.get() > MAX_CHARS)
//          println(v.take(k_maxChars) + "...")
          println("...")
        else
          println(v)
    }
  }

  // This helper function deals with output indentation, of which there is a lot.
  private def indent(level: Int): Unit = {
    for (_ <- 0 until level) do print("   ");
  }

  enum ReadType:
    case VALUE, INDIVIDUAL

}
