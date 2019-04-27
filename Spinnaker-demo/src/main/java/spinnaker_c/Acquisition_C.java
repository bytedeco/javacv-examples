//=============================================================================
// Copyright © 2018 FLIR Integrated Imaging Solutions, Inc. All Rights Reserved.
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
//=============================================================================*/

package spinnaker_c;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.spinnaker.Spinnaker_C.*;

import java.io.File;

import static org.bytedeco.spinnaker.global.Spinnaker_C.*;
import static spinnaker_c.Utils.*;

/**
 * Code based on C version, Acquisition_C.cpp, from Spinnaker SDK by FLIR.
 * <p>
 * Acquisition_C shows how to acquire images. It relies on
 * information provided in the Enumeration_C example. Following this, check
 * out the NodeMapInfo_C example if you haven't already. It explores
 * retrieving information from various node types.
 * <p>
 * This example touches on the preparation and cleanup of a camera just
 * before and just after the acquisition of images. Image retrieval and
 * conversion, grabbing image data, and saving images are all covered.
 * <p>
 * Once comfortable with Acquisition_C and NodeMapInfo_C, we suggest checking
 * out AcquisitionMultipleCamera_C, NodeMapCallback_C, or SaveToAvi_C.
 * AcquisitionMultipleCamera_C demonstrates simultaneously acquiring images
 * from a number of cameras, NodeMapCallback_C acts as a good introduction to
 * programming with callbacks and events, and SaveToAvi_C exhibits video
 * creation.
 * <p>
 * This Java version of the example is based on the Spinnaker C API example: Acquisition_C.
 */
public class Acquisition_C {
    private final static int MAX_BUFF_LEN = 256;

    // This function acquires and saves 10 images from a device.
    private static _spinError acquireImages(spinCamera hCam, spinNodeMapHandle hNodeMap, spinNodeMapHandle hNodeMapTLDevice) {
        System.out.println("\n*** IMAGE ACQUISITION ***\n");
        _spinError err;
        //
        // Set acquisition mode to continuous
        //
        // *** NOTES ***
        // Because the example acquires and saves 10 images, setting acquisition
        // mode to continuous lets the example finish. If set to single frame
        // or multiframe (at a lower number of images), the example would just
        // hang. This would happen because the example has been written to acquire
        // 10 images while the camera would have been programmed to retrieve
        // less than that.
        //
        // Setting the value of an enumeration node is slightly more complicated
        // than other node types, and especially so in C. It can roughly be broken
        // down into four steps: first, the enumeration node is retrieved from the
        // nodemap; second, the entry node is retrieved from the enumeration node;
        // third, an integer is retrieved from the entry node; and finally, the
        // integer is set as the new value of the enumeration node.
        //
        // It is important to note that there are two sets of functions that might
        // produce erroneous results if they were to be mixed up. The first two
        // functions, spinEnumerationSetIntValue() and
        // spinEnumerationEntryGetIntValue(), use the integer values stored on each
        // individual cameras. The second two, spinEnumerationSetEnumValue() and
        // spinEnumerationEntryGetEnumValue(), use enum values defined in the
        // Spinnaker library. The int and enum values will most likely be
        // different from another.
        //

        // Retrieve enumeration node from nodemap
        spinNodeHandle hAcquisitionMode = new spinNodeHandle(); //NULL
        err = spinNodeMapGetNode(hNodeMap, new BytePointer("AcquisitionMode"), hAcquisitionMode);
        if (Utils.printOnError(err, "Unable to set acquisition mode to continuous (node retrieval).")) {
            return err;
        }

        // Retrieve entry node from enumeration node
        spinNodeHandle hAcquisitionModeContinuous = new spinNodeHandle(); // NULL
        if (isAvailableAndReadable(hAcquisitionMode, "AcquisitionMode")) {
            err = spinEnumerationGetEntryByName(hAcquisitionMode, new BytePointer("Continuous"), hAcquisitionModeContinuous);
            if (Utils.printOnError(err, "Unable to set acquisition mode to continuous (entry 'continuous' retrieval).")) {
                return err;
            }
        } else {
            printRetrieveNodeFailure("entry", "AcquisitionMode");
            return _spinError.SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Retrieve integer from entry node
        LongPointer acquisitionModeContinuous = new LongPointer(1);
        if (isAvailableAndReadable(hAcquisitionModeContinuous, "AcquisitionModeContinuous")) {
            err = spinEnumerationEntryGetIntValue(hAcquisitionModeContinuous, acquisitionModeContinuous);

            if (Utils.printOnError(err, "Unable to set acquisition mode to continuous (entry int value retrieval).")) {
                return err;
            }
        } else {
            printRetrieveNodeFailure("entry", "AcquisitionMode 'Continuous'");
            return _spinError.SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Set integer as new value of enumeration node
        if (isAvailableAndWritable(hAcquisitionMode, "AcquisitionMode")) {
            err = spinEnumerationSetIntValue(hAcquisitionMode, acquisitionModeContinuous.get());
            if (Utils.printOnError(err, "Unable to set acquisition mode to continuous (entry int value setting).")) {
                return err;
            }
        } else {
            printRetrieveNodeFailure("entry", "AcquisitionMode");
            return _spinError.SPINNAKER_ERR_ACCESS_DENIED;
        }

        System.out.println("Acquisition mode set to continuous...");

        //
        // Begin acquiring images
        //
        // *** NOTES ***
        // What happens when the camera begins acquiring images depends on the
        // acquisition mode. Single frame captures only a single image, multi
        // frame catures a set number of images, and continuous captures a
        // continuous stream of images. Because the example calls for the retrieval
        // of 10 images, continuous mode has been set.
        //
        // *** LATER ***
        // Image acquisition must be ended when no more images are needed.
        //
        err = spinCameraBeginAcquisition(hCam);
        if (Utils.printOnError(err, "Unable to begin image acquisition.")) {
            return err;
        }

        System.out.println("Acquiring images...");

        //
        // Retrieve device serial number for filename
        //
        // *** NOTES ***
        // The device serial number is retrieved in order to keep cameras from
        // overwriting one another. Grabbing image IDs could also accomplish this.
        //
        spinNodeHandle hDeviceSerialNumber = new spinNodeHandle(); // NULL;
        BytePointer deviceSerialNumber = new BytePointer(MAX_BUFF_LEN);
        SizeTPointer lenDeviceSerialNumber = new SizeTPointer(1);
        lenDeviceSerialNumber.put(MAX_BUFF_LEN);
        err = spinNodeMapGetNode(hNodeMapTLDevice, new BytePointer("DeviceSerialNumber"), hDeviceSerialNumber);
        if (Utils.printOnError(err, "")) {
            deviceSerialNumber.putString("");
            lenDeviceSerialNumber.put(0);
        } else {
            if (isAvailableAndReadable(hDeviceSerialNumber, "DeviceSerialNumber")) {
                err = spinStringGetValue(hDeviceSerialNumber, deviceSerialNumber, lenDeviceSerialNumber);
                if (Utils.printOnError(err, "")) {
                    deviceSerialNumber.putString("");
                    lenDeviceSerialNumber.put(0);
                }
            } else {
                deviceSerialNumber.putString("");
                lenDeviceSerialNumber.put(0);
                printRetrieveNodeFailure("node", "DeviceSerialNumber");
            }
            System.out.println("Device serial number retrieved as " + deviceSerialNumber.getString().trim() + "...");
        }
        System.out.println();

        // Retrieve, convert, and save images
        final int k_numImages = 10;
        for (int imageCnt = 0; imageCnt < k_numImages; imageCnt++) {
            //
            // Retrieve next received image
            //
            // *** NOTES ***
            // Capturing an image houses images on the camera buffer. Trying to
            // capture an image that does not exist will hang the camera.
            //
            // *** LATER ***
            // Once an image from the buffer is saved and/or no longer needed, the
            // image must be released in orer to keep the buffer from filling up.
            //
            spinImage hResultImage = new spinImage(); //NULL;
            err = spinCameraGetNextImage(hCam, hResultImage);
            if (Utils.printOnError(err, "Unable to get next image. Non-fatal error.")) {
                continue;
            }
            //
            // Ensure image completion
            //
            // *** NOTES ***
            // Images can easily be checked for completion. This should be done
            // whenever a complete image is expected or required. Further, check
            // image status for a little more insight into why an image is
            // incomplete.
            //
            BytePointer isIncomplete = new BytePointer(1);
            boolean hasFailed = false;
            err = spinImageIsIncomplete(hResultImage, isIncomplete);
            if (Utils.printOnError(err, "Unable to determine image completion. Non-fatal error.")) {
                hasFailed = true;
            }
            // Check image for completion
            if (isIncomplete.getBool()) {
                IntPointer imageStatus = new IntPointer(1); //_spinImageStatus.IMAGE_NO_ERROR;
                err = spinImageGetStatus(hResultImage, imageStatus);
                if (!Utils.printOnError(err,
                        "Unable to retrieve image status. Non-fatal error. " + findImageStatusNameByValue(imageStatus.get()))) {
                    System.out.println(
                            "Image incomplete with image status " + findImageStatusNameByValue(imageStatus.get()) +
                                    "...");
                }
                hasFailed = true;
            }
            // Release incomplete or failed image
            if (hasFailed) {
                err = spinImageRelease(hResultImage);
                Utils.printOnError(err, "Unable to release image. Non-fatal error.");
                continue;
            }
            //
            // Print image information; height and width recorded in pixels
            //
            // *** NOTES ***
            // Images have quite a bit of available metadata including things such
            // as CRC, image status, and offset values, to name a few.
            //
            System.out.println("Grabbed image " + imageCnt);

            // Retrieve image width
            SizeTPointer width = new SizeTPointer(1);
            err = spinImageGetWidth(hResultImage, width);
            if (Utils.printOnError(err, "spinImageGetWidth()")) {
                System.out.println("width  = unknown");
            } else {
                System.out.println("width  = " + width.get());
            }

            // Retrieve image height
            SizeTPointer height = new SizeTPointer(1);
            err = spinImageGetHeight(hResultImage, height);
            if (Utils.printOnError(err, "spinImageGetHeight()")) {
                System.out.println("height = unknown");
            } else {
                System.out.println("height = " + height.get());
            }


            //
            // Convert image to mono 8
            //
            // *** NOTES ***
            // Images not gotten from a camera directly must be created and
            // destroyed. This includes any image copies, conversions, or
            // otherwise. Basically, if the image was gotten, it should be
            // released, if it was created, it needs to be destroyed.
            //
            // Images can be converted between pixel formats by using the
            // appropriate enumeration value. Unlike the original image, the
            // converted one does not need to be released as it does not affect the
            // camera buffer.
            //
            // Optionally, the color processing algorithm can also be set using
            // the alternate spinImageConvertEx() function.
            //
            // *** LATER ***
            // The converted image was created, so it must be destroyed to avoid
            // memory leaks.
            //
            spinImage hConvertedImage = new spinImage(); //NULL;
            err = spinImageCreateEmpty(hConvertedImage);
            if (Utils.printOnError(err, "Unable to create image. Non-fatal error.")) {
                hasFailed = true;
            }
            err = spinImageConvert(hResultImage, _spinPixelFormatEnums.PixelFormat_Mono8.value, hConvertedImage);
            if (Utils.printOnError(err, "\"Unable to convert image. Non-fatal error.")) {
                hasFailed = true;
            }

            if (!hasFailed) {
                // Create a unique filename
                String filename = lenDeviceSerialNumber.get() == 0
                        ? ("Acquisition-C-" + imageCnt + ".jpg")
                        : ("Acquisition-C-" + deviceSerialNumber.getString().trim() + "-" + imageCnt + ".jpg");

                //
                // Save image
                //
                // *** NOTES ***
                // The standard practice of the examples is to use device serial
                // numbers to keep images of one device from overwriting those of
                // another.
                //
                err = spinImageSave(hConvertedImage, new BytePointer(filename), _spinImageFileFormat.JPEG.value);
                if (!Utils.printOnError(err, "Unable to save image. Non-fatal error.")) {
                    System.out.println("Image saved at " + filename + "\n");
                }
            }

            //
            // Destroy converted image
            //
            // *** NOTES ***
            // Images that are created must be destroyed in order to avoid memory
            // leaks.
            //
            err = spinImageDestroy(hConvertedImage);
            Utils.printOnError(err, "Unable to destroy image. Non-fatal error.");
            //
            // Release image from camera
            //
            // *** NOTES ***
            // Images retrieved directly from the camera (i.e. non-converted
            // images) need to be released in order to keep from filling the
            // buffer.
            //
            err = spinImageRelease(hResultImage);
            Utils.printOnError(err, "Unable to release image. Non-fatal error.");
        }
        //
        // End acquisition
        //
        // *** NOTES ***
        // Ending acquisition appropriately helps ensure that devices clean up
        // properly and do not need to be power-cycled to maintain integrity.
        //
        err = spinCameraEndAcquisition(hCam);
        Utils.printOnError(err, "Unable to end acquisition.");
        return err;
    }


    /**
     * This function acts as the body of the example; please see NodeMapInfo_C
     * example for more in-depth comments on setting up cameras.
     */
    private static _spinError runSingleCamera(spinCamera hCam) {
        _spinError err;
        // Retrieve TL device nodemap and print device information
        spinNodeMapHandle hNodeMapTLDevice = new spinNodeMapHandle();
        err = spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice);
        if (!Utils.printOnError(err, "Unable to retrieve TL device nodemap .")) {
            err = printDeviceInfo(hNodeMapTLDevice);
        }

        // Initialize camera
        err = spinCameraInit(hCam);
        if (Utils.printOnError(err, "Unable to initialize camera.")) {
            return err;
        }

        // Retrieve GenICam nodemap
        spinNodeMapHandle hNodeMap = new spinNodeMapHandle();
        err = spinCameraGetNodeMap(hCam, hNodeMap);
        if (Utils.printOnError(err, "Unable to retrieve GenICam nodemap.")) {
            return err;
        }

        // Acquire images
        err = acquireImages(hCam, hNodeMap, hNodeMapTLDevice);
        if (Utils.printOnError(err, "acquireImages")) {
            return err;
        }

        // Deinitialize camera
        err = spinCameraDeInit(hCam);
        if (Utils.printOnError(err, "Unable to deinitialize camera.")) {
            return err;
        }
        return err;
    }


    /**
     * Example entry point; please see Enumeration_C example for more in-depth
     * comments on preparing and cleaning up the system.
     */
    public static void main(String[] args) {


        _spinError err;

        // Since this application saves images in the current folder
        // we must ensure that we have permission to write to this folder.
        // If we do not have permission, fail right away.
        if (!new File(".").canWrite()) {
            System.out.println("Failed to create file in current folder.  Please check permissions.");
            return;
        }

        // Retrieve singleton reference to system object
        spinSystem hSystem = new spinSystem();
        err = spinSystemGetInstance(hSystem);
        exitOnError(err, "Unable to retrieve system instance.");

        // Retrieve list of cameras from the system
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
        if (numCameras.get() == 0) {
            // Clear and destroy camera list before releasing system
            err = spinCameraListClear(hCameraList);
            exitOnError(err, "Unable to clear camera list.");

            err = spinCameraListDestroy(hCameraList);
            exitOnError(err, "Unable to destroy camera list.");

            // Release system
            err = spinSystemReleaseInstance(hSystem);
            exitOnError(err, "Unable to release system instance.");

            System.out.println("Not enough cameras!");
            return;
        }

        // Run example on each camera
        for (int i = 0; i < numCameras.get(); i++) {
            System.out.println("\nRunning example for camera " + i + "...");
            // Select camera
            spinCamera hCamera = new spinCamera();
            err = spinCameraListGet(hCameraList, i, hCamera);
            if (!Utils.printOnError(err, "Unable to retrieve camera from list.")) {
                // Run example
                err = runSingleCamera(hCamera);
                Utils.printOnError(err, "RunSingleCamera");
            }
            // Release camera
            err = spinCameraRelease(hCamera);
            Utils.printOnError(err, "Error releasing camera.");
            System.out.println("Camera " + i + " example complete...\n");
        }
        // Clear and destroy camera list before releasing system
        err = spinCameraListClear(hCameraList);
        exitOnError(err, "Unable to clear camera list.");

        err = spinCameraListDestroy(hCameraList);
        exitOnError(err, "Unable to destroy camera list.");

        // Release system
        err = spinSystemReleaseInstance(hSystem);
        exitOnError(err, "Unable to release system instance.");
    }
}
