package spinnaker_c;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.spinnaker.Spinnaker_C.spinNodeHandle;
import org.bytedeco.spinnaker.Spinnaker_C.spinNodeMapHandle;

import static org.bytedeco.spinnaker.global.Spinnaker_C.*;

/**
 * Created by Jarek Sacha on 10/29/2018.
 */
class Utils {

    private final static int MAX_BUFF_LEN = 256;

    /**
     * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
     * If it is do nothing otherwise print error description and exit.
     *
     * @param err     error value.
     * @param message additional message to print.
     */
    static void exitOnError(_spinError err, String message) {
        if (Utils.printOnError(err, message)) {
            System.out.println("Aborting.");
            System.exit(err.value);
        }
    }

    /**
     * Check if 'err' is 'SPINNAKER_ERR_SUCCESS'.
     * If it is do nothing otherwise print error information.
     *
     * @param err     error value.
     * @param message additional message to print.
     * @return 'false' if err is SPINNAKER_ERR_SUCCESS, or 'true' for any other 'err' value.
     */
    static boolean printOnError(_spinError err, String message) {
        if (err.value != _spinError.SPINNAKER_ERR_SUCCESS.value) {
            System.out.println(message);
            System.out.println("Error " + err.value + " " + findErrorNameByValue(err.value) + "\n");
            return true;
        } else {
            return false;
        }
    }

    /**
     * This function prints the device information of the camera from the transport
     * layer; please see NodeMapInfo_C example for more in-depth comments on
     * printing device information from the nodemap.
     */
    static _spinError printDeviceInfo(spinNodeMapHandle hNodeMap) {
        _spinError err;
        System.out.println("\n*** DEVICE INFORMATION ***\n\n");
        // Retrieve device information category node
        spinNodeHandle hDeviceInformation = new spinNodeHandle();
        err = spinNodeMapGetNode(hNodeMap, new BytePointer("DeviceInformation"), hDeviceInformation);
        Utils.printOnError(err, "Unable to retrieve node.");

        // Retrieve number of nodes within device information node
        SizeTPointer numFeatures = new SizeTPointer(1);
        if (isAvailableAndReadable(hDeviceInformation, "DeviceInformation")) {
            err = spinCategoryGetNumFeatures(hDeviceInformation, numFeatures);
            Utils.printOnError(err, "Unable to retrieve number of nodes.");
        } else {
            printRetrieveNodeFailure("node", "DeviceInformation");
            return _spinError.SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Iterate through nodes and print information
        for (int i = 0; i < numFeatures.get(); i++) {
            spinNodeHandle hFeatureNode = new spinNodeHandle();
            err = spinCategoryGetFeatureByIndex(hDeviceInformation, i, hFeatureNode);
            Utils.printOnError(err, "Unable to retrieve node.");

            // get feature node name
            BytePointer featureName = new BytePointer(MAX_BUFF_LEN);
            SizeTPointer lenFeatureName = new SizeTPointer(1);
            lenFeatureName.put(MAX_BUFF_LEN);
            err = spinNodeGetName(hFeatureNode, featureName, lenFeatureName);
            if (Utils.printOnError(err, "Error retrieving node name.")) {
                featureName.putString("Unknown name");
            }

            int[] featureType = {_spinNodeType.UnknownNode.value};
            if (isAvailableAndReadable(hFeatureNode, featureName.getString())) {
                err = spinNodeGetType(hFeatureNode, featureType);
                if (Utils.printOnError(err, "Unable to retrieve node type.")) {
                    continue;
                }
            } else {
                System.out.println(featureName + ": Node not readable");
                continue;
            }
            BytePointer featureValue = new BytePointer(MAX_BUFF_LEN);
            SizeTPointer lenFeatureValue = new SizeTPointer(1);
            lenFeatureValue.put(MAX_BUFF_LEN);
            err = spinNodeToString(hFeatureNode, featureValue, lenFeatureValue);
            if (Utils.printOnError(err, "spinNodeToString")) {
                featureValue.putString("Unknown value");
            }
            System.out.println(featureName.getString().trim() + ": " + featureValue.getString().trim() + ".");
        }
        System.out.println();
        return err;
    }

    /**
     * This function helps to check if a node is available
     */
    static boolean isAvailable(spinNodeHandle hNode) {
        BytePointer pbAvailable = new BytePointer(1);
        pbAvailable.putBool(false);
        _spinError err = spinNodeIsAvailable(hNode, pbAvailable);
        printOnError(err, "Unable to retrieve node availability (" + hNode + " node)");
        return pbAvailable.getBool();
    }

    /**
     * This function helps to check if a node is available and readable
     */
    static boolean isAvailableAndReadable(spinNodeHandle hNode, String nodeName) {
        BytePointer pbAvailable = new BytePointer(1);
        _spinError err;
        err = spinNodeIsAvailable(hNode, pbAvailable);
        Utils.printOnError(err, "Unable to retrieve node availability (" + nodeName + " node)");

        BytePointer pbReadable = new BytePointer(1);
        err = spinNodeIsReadable(hNode, pbReadable);
        Utils.printOnError(err, "Unable to retrieve node readability (" + nodeName + " node)");
        return pbReadable.getBool() && pbAvailable.getBool();
    }

    /**
     * This function helps to check if a node is available and writable
     */
    static boolean isAvailableAndWritable(spinNodeHandle hNode, String nodeName) {
        BytePointer pbAvailable = new BytePointer(1);
        _spinError err;
        err = spinNodeIsAvailable(hNode, pbAvailable);
        Utils.printOnError(err, "Unable to retrieve node availability (" + nodeName + " node).");

        BytePointer pbWritable = new BytePointer(1);
        err = spinNodeIsWritable(hNode, pbWritable);
        Utils.printOnError(err, "Unable to retrieve node writability (" + nodeName + " node).");
        return pbWritable.getBool() && pbAvailable.getBool();
    }

    /**
     * This function helps to check if a node is readable
     */
    static boolean isReadable(spinNodeHandle hNode) {
        BytePointer pbReadable = new BytePointer(1);
        pbReadable.putBool(false);
        _spinError err = spinNodeIsReadable(hNode, pbReadable);
        printOnError(err, "Unable to retrieve node availability (" + hNode + " node)");
        return pbReadable.getBool();
    }

    /**
     * This function helps to check if a node is writable
     */
    static boolean isWritable(spinNodeHandle hNode) {
        BytePointer pbWritable = new BytePointer(1);
        pbWritable.putBool(false);
        _spinError err = spinNodeIsWritable(hNode, pbWritable);
        printOnError(err, "Unable to retrieve node writability(" + hNode + " node)");
        return pbWritable.getBool();
    }

    /**
     * This function handles the error prints when a node or entry is unavailable or
     * not readable/writable on the connected camera
     */
    static void printRetrieveNodeFailure(String node, String name) {
        System.out.println("Unable to get " + node + " (" + name + " " + node + " retrieval failed).");
        System.out.println("The " + node + " may not be available on all camera models...");
        System.out.println("Please try a Blackfly S camera.\n");
    }

    /**
     * Proxy for {@code System.out.printf()}, so it can be imported as static import and simplify porting C code.
     */
    static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    private static String findErrorNameByValue(int value) {
        for (_spinError v : _spinError.values()) {
            if (v.value == value) {
                return v.name();
            }
        }
        return "???";
    }

    static String findImageStatusNameByValue(int value) {
        for (_spinImageStatus v : _spinImageStatus.values()) {
            if (v.value == value) {
                return v.name();
            }
        }
        return "???";
    }
}
