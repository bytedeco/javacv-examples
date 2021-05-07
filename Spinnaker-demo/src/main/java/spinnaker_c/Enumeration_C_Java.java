//=============================================================================
// Copyright (c) 2001-2021 FLIR Systems, Inc. All Rights Reserved.
//
// This software is the confidential and proprietary information of FLIR
// Integrated Imaging Solutions, Inc. ("Confidential Information"). You
// shall not disclose such Confidential Information and shall use it only in
// accordance with the terms of the license agreement you entered into
// with FLIR Integrated Imaging Solutions, Inc. (FLIR).
//
// FLIR MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
// SOFTWARE, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE, OR NON-INFRINGEMENT. FLIR SHALL NOT BE LIABLE FOR ANY DAMAGES
// SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
// THIS SOFTWARE OR ITS DERIVATIVES.
//=============================================================================
package spinnaker_c;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.spinnaker.Spinnaker_C.*;

import static org.bytedeco.spinnaker.global.Spinnaker_C.*;
import static spinnaker_c.Utils.exitOnError;
import static spinnaker_c.Utils.printOnError;

/**
 * Code based on C version, Enumeration_C.c, from Spinnaker SDK by FLIR.
 * <p>
 * Enumeration_C.c shows how to enumerate interfaces and cameras.
 * Knowing this is mandatory for doing anything with the Spinnaker SDK, and
 * is therefore the best place to start learning how to use the SDK.
 * <p>
 * This example introduces the preparation, use, and cleanup of the system
 * object, interface and camera lists, interfaces, and cameras. It also
 * touches on retrieving both nodes from nodemaps and information from
 * nodes.
 * <p>
 * Once comfortable with enumeration, we suggest checking out either the
 * Acquisition_C or NodeMapInfo_C examples. Acquisition_C demonstrates using a
 * camera to acquire images while NodeMapInfo_C explores retrieving
 * information from various node types.
 */
public class Enumeration_C_Java {
    private final static int MAX_BUFF_LEN = 256;

    /* This function queries an interface for its cameras and then prints out
     * device information.
     */
    static public _spinError QueryInterface(spinInterface hInterface) {
        _spinError err;

        //
        // Retrieve TL nodemap from interface
        //
        // *** NOTES ***
        // Each interface has a nodemap that can be retrieved in order to access
        // information about the interface itself, any devices connected, or
        // addressing information if applicable.
        //
        spinNodeMapHandle hNodeMapInterface = new spinNodeMapHandle(); // NULL

        err = spinInterfaceGetTLNodeMap(hInterface, hNodeMapInterface);
        if (printOnError(err, "Unable to retrieve interface nodemap.")) {
            return err;
        }

        //
        // Print interface display name
        //
        // *** NOTES ***
        // Each interface has a nodemap that can be retrieved in order to access
        // information about the interface itself, any devices connected, or
        // addressing information if applicable.
        //
        spinNodeHandle hInterfaceDisplayName = new spinNodeHandle(); // NULL

        // Retrieve node
        err = spinNodeMapGetNode(hNodeMapInterface, new BytePointer("InterfaceDisplayName"), hInterfaceDisplayName);
        if (printOnError(err, "Unable to retrieve node (interface display name).")) {
            return err;
        }

        // Check availability
        BytePointer interfaceDisplayNameIsAvailable = new BytePointer(1).putBool(false);
        err = spinNodeIsAvailable(hInterfaceDisplayName, interfaceDisplayNameIsAvailable);
        if (printOnError(err, "Unable to check node availability (interface display name).")) {
            return err;
        }

        // Check readability
        BytePointer interfaceDisplayNameIsReadable = new BytePointer(1).putBool(false);
        err = spinNodeIsReadable(hInterfaceDisplayName, interfaceDisplayNameIsReadable);
        if (printOnError(err, "Unable to check node readability (interface display name).")) {
            return err;
        }

        // Print
        BytePointer interfaceDisplayName = new BytePointer(MAX_BUFF_LEN);

        SizeTPointer lenInterfaceDisplayName = new SizeTPointer(1).put(MAX_BUFF_LEN);

        if (interfaceDisplayNameIsAvailable.getBool() && interfaceDisplayNameIsReadable.getBool()) {
            err = spinStringGetValue(hInterfaceDisplayName, interfaceDisplayName, lenInterfaceDisplayName);
            if (printOnError(err, "Unable to retrieve value (interface display name).")) {
                return err;
            }
        } else {
            interfaceDisplayName = interfaceDisplayName.putString("Interface display name not readable");
        }

        System.out.println("Interface: " + interfaceDisplayName.getString().trim());

        //
        // Retrieve list of cameras from the interface
        //
        // *** NOTES ***
        // Camera lists can be retrieved from an interface or the system object.
        // Camera lists retrieved from an interface, such as this one, only return
        // cameras attached on that specific interface whereas camera lists
        // retrieved from the system will return all cameras on all interfaces.
        //
        // *** LATER ***
        // Camera lists must be cleared manually. This must be done prior to
        // releasing the system and while the camera list is still in scope.
        //
        spinCameraList hCameraList = new spinCameraList();
//    size_t numCameras = 0;

        // Create empty camera list
        err = spinCameraListCreateEmpty(hCameraList);
        if (printOnError(err, "Unable to create camera list.")) {
            return err;
        }

        // Retrieve cameras
        err = spinInterfaceGetCameras(hInterface, hCameraList);
        if (printOnError(err, "Unable to retrieve camera list.")) {
            return err;
        }

        // Retrieve number of cameras
        SizeTPointer numCameras = new SizeTPointer(1);
        err = spinCameraListGetSize(hCameraList, numCameras);
        if (printOnError(err, "Unable to retrieve number of cameras.")) {
            return err;
        }

        // Return if no cameras detected
        if (numCameras.get() == 0) {
            System.out.println("\tNo devices detected.\n\n");

            //
            // Clear and destroy camera list before losing scope
            //
            // *** NOTES ***
            // Camera lists do not automatically clean themselves up. This must be done
            // manually. The same is true of interface lists.
            //
            err = spinCameraListClear(hCameraList);
            if (printOnError(err, "Unable to clear camera list.")) {
                return err;
            }

            err = spinCameraListDestroy(hCameraList);
            if (printOnError(err, "Unable to destroy camera list.")) {
                return err;
            }

            return err;
        }

        // Print device vendor and model name for each camera on the interface
        for (int i = 0; i < numCameras.get(); i++) {
            //
            // Select camera
            //
            // *** NOTES ***
            // Each camera is retrieved from a camera list with an index. If the
            // index is out of range, an exception is thrown.
            //
            // *** LATER ***
            // Each camera handle needs to be released before losing scope or the
            // system is released.
            //
            spinCamera hCam = new spinCamera();

            err = spinCameraListGet(hCameraList, i, hCam);
            if (printOnError(err, "Unable to retrieve camera.")) {
                return err;
            }

            // Retrieve TL device nodemap; please see NodeMapInfo_C example for
            // additional comments on transport layer nodemaps.
            spinNodeMapHandle hNodeMapTLDevice = new spinNodeMapHandle();

            err = spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice);
            if (printOnError(err, "Unable to retrieve TL device nodemap.")) {
                return err;
            }

            //
            // Retrieve device vendor name
            //
            // *** NOTES ***
            // Grabbing node information requires first retrieving the node and
            // then retrieving its information. There are two things to keep in
            // mind. First, a node is distinguished by type, which is related
            // to its value's data type.  Second, nodes should be checked for
            // availability and readability/writability prior to making an
            // attempt to read from or write to the node.
            //
            spinNodeHandle hDeviceVendorName = new spinNodeHandle();
            BytePointer deviceVendorNameIsAvailable = new BytePointer(1).putBool(false);
            BytePointer deviceVendorNameIsReadable = new BytePointer(1).putBool(false);

            // Retrieve node
            err = spinNodeMapGetNode(hNodeMapTLDevice, new BytePointer("DeviceVendorName"), hDeviceVendorName);
            if (printOnError(err, "Unable to retrieve device information (vendor name node).")) {
                return err;
            }

            // Check availability
            err = spinNodeIsAvailable(hDeviceVendorName, deviceVendorNameIsAvailable);
            if (printOnError(err, "Unable to check node availability (vendor name node).")) {
                return err;
            }

            // Check readability
            err = spinNodeIsReadable(hDeviceVendorName, deviceVendorNameIsReadable);
            if (printOnError(err, "Unable to check node readability (vendor name node).")) {
                return err;
            }

            //
            // Retrieve device model name
            //
            // *** NOTES ***
            // Because C has no try-catch blocks, each function returns an error
            // code to suggest whether an error has occurred. Errors can be
            // sufficiently handled with these return codes. Checking availability
            // and readability/writability makes for safer and more complete code;
            // however, keeping in mind example conciseness and legibility, only
            // this example and NodeMapInfo_C demonstrate checking node
            // availability and readability/writability while other examples
            // handle errors with error codes alone.
            //
            spinNodeHandle hDeviceModelName = new spinNodeHandle();
            BytePointer deviceModelNameIsAvailable = new BytePointer(1).putBool(false);
            BytePointer deviceModelNameIsReadable = new BytePointer(1).putBool(false);

            err = spinNodeMapGetNode(hNodeMapTLDevice, new BytePointer("DeviceModelName"), hDeviceModelName);
            if (printOnError(err, "Unable to retrieve device information (model name node).")) {
                return err;
            }

            err = spinNodeIsAvailable(hDeviceModelName, deviceModelNameIsAvailable);
            if (printOnError(err, "Unable to check node availability (model name node).")) {
                return err;
            }

            err = spinNodeIsReadable(hDeviceModelName, deviceModelNameIsReadable);
            if (printOnError(err, "Unable to check node readability (model name node).")) {
                return err;
            }

            //
            // Print device vendor and model names
            //
            // *** NOTES ***
            // Generally it is best to check readability when it is required to read
            // information from a node and writability when it is required to write
            // to a node. For most nodes, writability implies readability while
            // readability does not imply writability.
            //
            BytePointer deviceVendorName = new BytePointer(MAX_BUFF_LEN);
            SizeTPointer lenDeviceVendorName = new SizeTPointer(1).put(MAX_BUFF_LEN);
            BytePointer deviceModelName = new BytePointer(MAX_BUFF_LEN);
            SizeTPointer lenDeviceModelName = new SizeTPointer(1).put(MAX_BUFF_LEN);

            // Print device vendor name
            if (deviceVendorNameIsAvailable.getBool() && deviceVendorNameIsReadable.getBool()) {
                err = spinStringGetValue(hDeviceVendorName, deviceVendorName, lenDeviceVendorName);
                if (printOnError(err, "Unable to retrieve device information (vendor name value).")) {
                    return err;
                }
            } else {
                deviceVendorName = deviceVendorName.putString("Not readable");
            }

            // Print device model name
            if (deviceModelNameIsAvailable.getBool() && deviceModelNameIsReadable.getBool()) {
                err = spinStringGetValue(hDeviceModelName, deviceModelName, lenDeviceModelName);
                if (printOnError(err, "Unable to retrieve device information (model name value).")) {
                    return err;
                }
            } else {
                deviceModelName = deviceModelName.putString("Not readable");
            }

            System.out.printf("\tDevice %d / %s / %s\n\n", i, deviceVendorName.getString().trim(), deviceModelName.getString().trim());

            //
            // Release camera before losing scope
            //
            // *** NOTES ***
            // Every handle that is created for a camera must be released before
            // the system is released or an exception will be thrown.
            //
            err = spinCameraRelease(hCam);
            if (printOnError(err, "\"Unable to release camera.")) {
                return err;
            }
        }

        //
        // Clear and destroy camera list before losing scope
        //
        // *** NOTES ***
        // Camera lists do not automatically clean themselves up. This must be done
        // manually. The same is true of interface lists.
        //
        err = spinCameraListClear(hCameraList);
        if (printOnError(err, "Unable to clear camera list.")) {
            return err;
        }

        err = spinCameraListDestroy(hCameraList);
        if (printOnError(err, "Unable to destroy camera list.")) {
            return err;
        }

        return err;
    }

    /**
     * Example entry point; this function sets up the system and retrieves
     * interfaces for the example.
     */
    public static void main(String[] args) {
        _spinError err;

        //
        // Retrieve singleton reference to system object
        //
        // *** NOTES ***
        // Everything originates with the system object. It is important to notice
        // that it has a singleton implementation, so it is impossible to have
        // multiple system objects at the same time.
        //
        // *** LATER ***
        // The system object should be cleared prior to program completion.  If not
        // released explicitly, it will be released automatically.
        //
        spinSystem hSystem = new spinSystem();
        err = spinSystemGetInstance(hSystem);
        exitOnError(err, "Unable to retrieve system instance.");

        // Print out current library version
        spinLibraryVersion hLibraryVersion = new spinLibraryVersion();

        spinSystemGetLibraryVersion(hSystem, hLibraryVersion);
        System.out.printf("Spinnaker library version: %d.%d.%d.%d\n\n%n",
                hLibraryVersion.major(),
                hLibraryVersion.minor(),
                hLibraryVersion.type(),
                hLibraryVersion.build());

        //
        // Retrieve list of interfaces from the system
        //
        // *** NOTES ***
        // Interface lists are retrieved from the system object.
        //
        // *** LATER ***
        // Interface lists must be cleared and destroyed manually. This must be
        // done prior to releasing the system and while the interface list is still
        // in scope.
        //
        spinInterfaceList hInterfaceList = new spinInterfaceList();
        SizeTPointer numInterfaces = new SizeTPointer(1);

        // Create empty interface list
        err = spinInterfaceListCreateEmpty(hInterfaceList);
        if (printOnError(err, "Unable to create empty interface list")) {
            return;
        }

        // Retrieve interfaces from system
        err = spinSystemGetInterfaces(hSystem, hInterfaceList);
        if (printOnError(err, "Unable to retrieve interface list.")) {
            return;
        }

        // Retrieve number of interfaces
        err = spinInterfaceListGetSize(hInterfaceList, numInterfaces);
        if (printOnError(err, "Unable to retrieve number of interfaces.")) {
            return;
        }

        System.out.println("Number of interfaces detected: " + numInterfaces.get() + "\n");

        //
        // Retrieve list of cameras from the system
        //
        // *** NOTES ***
        // Camera lists can be retrieved from an interface or the system object.
        // Camera lists retrieved from the system, such as this one, return all
        // cameras available on the system.
        //
        // *** LATER ***
        // Camera lists must be cleared and destroyed manually. This must be done
        // prior to releasing the system and while the camera list is still in
        // scope.
        //
        spinCameraList hCameraList = new spinCameraList();
        err = spinCameraListCreateEmpty(hCameraList);
        exitOnError(err, "Unable to create camera list.");

        err = spinSystemGetCameras(hSystem, hCameraList);
        exitOnError(err, "Unable to retrieve camera list.");

        // Retrieve number of cameras
        SizeTPointer numCameras = new SizeTPointer(1);
        err = spinCameraListGetSize(hCameraList, numCameras);
        exitOnError(err, "Unable to retrieve number of cameras.");
        System.out.println("Number of cameras detected: " + numCameras.get() + "\n");

        // Finish if there are no cameras
        if (numCameras.get() == 0 || numInterfaces.get() == 0) {
            // Clear and destroy camera list before releasing system
            err = spinCameraListClear(hCameraList);
            exitOnError(err, "Unable to clear camera list.");

            err = spinCameraListDestroy(hCameraList);
            exitOnError(err, "Unable to destroy camera list.");

            // Clear and destroy interface list before releasing system
            err = spinInterfaceListClear(hInterfaceList);
            exitOnError(err, "Unable to clear interface list.");

            err = spinInterfaceListDestroy(hInterfaceList);
            exitOnError(err, "Unable to destroy interface list.");

            // Release system
            err = spinSystemReleaseInstance(hSystem);
            exitOnError(err, "Unable to release system instance.");

            System.out.println("Not enough cameras/interfaces!");
            return;
        }


        System.out.println("\n*** QUERYING INTERFACES ***\n");

        //
        // Run example on each interface
        //
        // *** NOTES ***
        // In order to run all interfaces in a loop, each interface needs to
        // retrieved using its index.
        //
        for (int i = 0; i < numInterfaces.get(); i++) {
            // Select interface
            spinInterface hInterface = new spinInterface();

            err = spinInterfaceListGet(hInterfaceList, i, hInterface);
            if (printOnError(err, "Unable to retrieve interface from list.")) {
                continue;
            }

            // Run example
            err = QueryInterface(hInterface);

            // Release interface
            err = spinInterfaceRelease(hInterface);
        }

        //
        // Clear and destroy camera list before releasing system
        //
        // *** NOTES ***
        // Camera lists are not shared pointers and do not automatically clean
        // themselves up and break their own references. Therefore, this must be
        // done manually. The same is true of interface lists.
        //
        err = spinCameraListClear(hCameraList);
        exitOnError(err, "Unable to clear camera list.");

        err = spinCameraListDestroy(hCameraList);
        exitOnError(err, "Unable to destroy camera list.");

        //
        // Clear and destroy interface list before releasing system
        //
        // *** NOTES ***
        // Interface lists are not shared pointers and do not automatically clean
        // themselves up and break their own references. Therefore, this must be
        // done manually. The same is true of camera lists.
        //
        // Clear and destroy interface list before releasing system
        err = spinInterfaceListClear(hInterfaceList);
        exitOnError(err, "Unable to clear interface list.");

        err = spinInterfaceListDestroy(hInterfaceList);
        exitOnError(err, "Unable to destroy interface list.");

        //
        // Release system
        //
        // *** NOTES ***
        // The system should be released, but if it is not, it will do so itself.
        // It is often at the release of the system (whether manual or automatic)
        // that unbroken references and still registered events will throw an
        // exception.
        //
        err = spinSystemReleaseInstance(hSystem);
        exitOnError(err, "Unable to release system instance.");

        System.out.println("\nDone!\n");
    }

}
