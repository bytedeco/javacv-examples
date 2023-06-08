package spinnaker_c4j;

import org.bytedeco.javacpp.*;
import org.bytedeco.spinnaker.Spinnaker_C.*;

import java.io.File;

import static org.bytedeco.spinnaker.global.Spinnaker_C.*;
import static org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR;
import static org.bytedeco.spinnaker.global.Spinnaker_C.spinError.*;
import static org.bytedeco.spinnaker.global.Spinnaker_C.spinImageFileFormat.SPINNAKER_IMAGE_FILE_FORMAT_JPEG;
import static org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR;
import static org.bytedeco.spinnaker.global.Spinnaker_C.spinPixelFormatEnums.PixelFormat_Mono8;
import static spinnaker_c4j.Utils.*;

/**
 * Code based on C version, Sequencer_C.cpp, from Spinnaker SDK by FLIR.
 * <p>
 * Sequencer_C shows how to use the sequencer to grab images with
 * various settings. It relies on information provided in the Enumeration_C,
 * Acquisition_C, and NodeMapInfo_C examples.
 * <p>
 * It can also be helpful to familiarize yourself with the
 * ImageFormatControl_C and Exposure_C examples as these provide a strong
 * introduction to camera customization.
 * <p>
 * The sequencer is another very powerful tool that can be used to create and
 * store multiple sets of customized image settings. A very useful
 * application of the sequencer is creating high dynamic range images.
 * <p>
 * This example is probably the most complex and definitely the longest. As
 * such, the configuration has been split between three functions. The first
 * prepares the camera to set the sequences, the second sets the settings for
 * a single sequence (it is run five times), and the third configures the
 * camera to use the sequencer when it acquires images.
 */
public class Sequencer_C {
    private final static int MAX_BUFF_LEN = 256;

    /**
     * This function prepares the sequencer to accept custom configurations by
     * ensuring sequencer mode is off (this is a requirement to the enabling of
     * sequencer configuration mode), disabling automatic gain and exposure, and
     * turning sequencer configuration mode on.
     */
    private static spinError configureSequencerPartOne(spinNodeMapHandle hNodeMap) {
        spinError err;

        System.out.println("\n\n*** SEQUENCER CONFIGURATION ***\n\n");

        //
        // Ensure sequencer is off for configuration
        //
        // *** NOTES ***
        // In order to set a new sequencer configuration, sequencer mode must
        // be disabled and sequencer configuration mode must be enabled. In
        // order to manually disable sequencer mode, the sequencer configuration
        // must be valid; otherwise, we know that sequencer mode is off, but an
        // exception will be raised when we try to manually disable it.
        //
        // Therefore, in order to ensure that sequencer mode is off, we first
        // check whether the current sequencer configuration is valid. If it
        // isn't, then we know that sequencer mode is off and we can move on;
        // however, if it is, then we know it is safe to manually disable
        // sequencer mode.
        //
        // Also note that sequencer configuration mode needs to be off in order
        // to manually disable sequencer mode. It should be off by default, so
        // the example skips checking this.
        //
        spinNodeHandle hSequencerConfigurationValid = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationValidCurrent = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationValidYes = new spinNodeHandle();
        spinNodeHandle hSequencerMode = new spinNodeHandle();
        spinNodeHandle hSequencerModeOff = new spinNodeHandle();
        LongPointer sequencerModeOff = new LongPointer(1);
        sequencerModeOff.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerConfigurationValid"), hSequencerConfigurationValid);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "SequencerConfigurationValid");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }


        if (!isReadable(hSequencerConfigurationValid, "hSequencerConfigurationValid")) {
            printRetrieveNodeFailure("node", "SequencerConfigurationValid");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetCurrentEntry(hSequencerConfigurationValid, hSequencerConfigurationValidCurrent);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid current");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationValidCurrent, "hSequencerConfigurationValidCurrent")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid current");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerConfigurationValid, new BytePointer("Yes"), hSequencerConfigurationValidYes);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid 'Yes'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationValidYes, "hSequencerConfigurationValidYes")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid 'Yes'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // If valid, disable sequencer mode; otherwise, do nothing
        if (hSequencerConfigurationValidCurrent.equals(hSequencerConfigurationValidYes)) {
            err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerMode"), hSequencerMode);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printRetrieveNodeFailure("node", "SequencerMode");
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            if (!isReadable(hSequencerMode, "hSequencerMode")) {
                printRetrieveNodeFailure("node", "SequencerMode");
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            err = spinEnumerationGetEntryByName(hSequencerMode, new BytePointer("Off"), hSequencerModeOff);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printRetrieveNodeFailure("entry", "SequencerMode 'Off'");
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            if (!isReadable(hSequencerModeOff, "hSequencerModeOff")) {
                printRetrieveNodeFailure("entry", "SequencerMode 'Off'");
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            err = spinEnumerationEntryGetIntValue(hSequencerModeOff, sequencerModeOff);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                System.out.printf("Unable to disable sequencer mode (entry int value retrieval). Aborting with error %d...\n\n", err.value);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            if (!isWritable(hSequencerMode, "hSequencerMode")) {
                printRetrieveNodeFailure("node", "SequencerMode");
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            err = spinEnumerationSetIntValue(hSequencerMode, sequencerModeOff.get());
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                System.out.printf("Unable to disable sequencer mode (entry int value setting). Aborting with error %d...\n\n", err.value);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }
        }

        System.out.print("Sequencer mode disabled...\n");

        //
        // Turn off automatic exposure mode
        //
        // *** NOTES ***
        // Automatic exposure prevents the manual configuration of exposure
        // times and needs to be turned off for this example.
        //
        // *** LATER ***
        // If exposure time is not being manually set for a specific reason, it
        // is best to let the camera take care of exposure time automatically.
        //
        spinNodeHandle hExposureAuto = new spinNodeHandle();
        spinNodeHandle hExposureAutoOff = new spinNodeHandle();
        LongPointer exposureAutoOff = new LongPointer(1);
        exposureAutoOff.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("ExposureAuto"), hExposureAuto);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "ExposureAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hExposureAuto, "hExposureAuto")) {
            printRetrieveNodeFailure("node", "ExposureAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hExposureAuto, new BytePointer("Off"), hExposureAutoOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "ExposureAuto 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hExposureAutoOff, "hExposureAutoOff")) {
            printRetrieveNodeFailure("entry", "ExposureAuto 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hExposureAutoOff, exposureAutoOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable automatic exposure (entry int value retrieval). Aborting with error %d...\n\n", err.value);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hExposureAuto, "hExposureAuto")) {
            printRetrieveNodeFailure("node", "ExposureAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hExposureAuto, exposureAutoOff.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable automatic exposure (entry int value setting). Aborting with error %d...\n\n", err.value);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Automatic exposure disabled...\n");

        //
        // Turn off automatic gain
        //
        // *** NOTES ***
        // Automatic gain prevents the manual configuration of gain and needs to
        // be turned off for this example.
        //
        // *** LATER ***
        // If gain is not being manually set for a specific reason, it is best
        // to let the camera take care of gain automatically.
        //
        spinNodeHandle hGainAuto = new spinNodeHandle();
        spinNodeHandle hGainAutoOff = new spinNodeHandle();
        LongPointer gainAutoOff = new LongPointer(1);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("GainAuto"), hGainAuto);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "GainAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hGainAuto, "hGainAuto")) {
            printRetrieveNodeFailure("node", "GainAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hGainAuto, new BytePointer("Off"), hGainAutoOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure(" entry", "GainAuto 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hGainAutoOff, "hGainAutoOff")) {
            printRetrieveNodeFailure(" entry", "GainAuto 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hGainAutoOff, gainAutoOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hGainAuto, "hGainAuto")) {
            printRetrieveNodeFailure("node", "GainAuto");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hGainAuto, gainAutoOff.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Automatic gain disabled...\n");

        //
        // Turn configuration mode on
        //
        // *** NOTES ***
        // Once sequencer mode is off, enabling sequencer configuration mode
        // allows for the setting of individual sequences.
        //
        // *** LATER ***
        // Before sequencer mode is turned back on, sequencer configuration
        // mode must be turned off.
        //
        spinNodeHandle hSequencerConfigurationMode = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationModeOn = new spinNodeHandle();
        LongPointer sequencerConfigurationModeOn = new LongPointer(1);
        sequencerConfigurationModeOn.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerConfigurationMode"), hSequencerConfigurationMode);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "SequencerConfigurationMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationMode, "hSequencerConfigurationMode")) {
            printRetrieveNodeFailure("node", "SequencerConfigurationMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerConfigurationMode, new BytePointer("On"), hSequencerConfigurationModeOn);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationMode 'On'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationModeOn, "hSequencerConfigurationModeOn")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationMode 'On'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerConfigurationModeOn, sequencerConfigurationModeOn);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer configuration mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerConfigurationMode, "hSequencerConfigurationMode")) {
            printRetrieveNodeFailure("node", "SequencerConfigurationMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hSequencerConfigurationMode, sequencerConfigurationModeOn.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer configuration mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Sequencer configuration mode enabled...\n\n");

        return SPINNAKER_ERR_SUCCESS;
    }


    /**
     * This function sets a single state. It sets the sequence number, applies
     * custom settings, selects the trigger type and next state number, and saves
     * the state. The custom values that are applied are all calculated in the
     * function that calls this one, RunSingleCamera().
     */
    private static spinError setSingleState(spinNodeMapHandle hNodeMap, int sequenceNumber, long widthToSet, long heightToSet, double exposureTimeToSet, double gainToSet) {
        spinError err;

        //
        // Select the sequence number
        //
        // *** NOTES ***
        // Select the index of the state to be set.
        //
        // *** LATER ***
        // The next state - i.e. the state to be linked to -
        // also needs to be set before saving the current state.
        //
        spinNodeHandle hSequencerSetSelector = new spinNodeHandle();

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerSetSelector"), hSequencerSetSelector);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to select current sequence. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerSetSelector, "hSequencerSetSelector")) {
            printf("Unable to select current sequence. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinIntegerSetValue(hSequencerSetSelector, sequenceNumber);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to select current sequence. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Customizing sequence %d...\n", sequenceNumber);

        //
        // Set desired settings for the current state
        //
        // *** NOTES ***
        // Width, height, exposure time, and gain are set in this example. If
        // the sequencer isn't working properly, it may be important to ensure
        // that each feature is enabled on the sequencer. Features are enabled
        // by default, so this is not explored in this example.
        //
        // Changing the height and width for the sequencer is not available
        // for all camera models.
        //
        // Set width; width recorded in pixels
        spinNodeHandle hWidth = new spinNodeHandle();
        LongPointer widthInc = new LongPointer(1);
        widthInc.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Width"), hWidth);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set width. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (isReadable(hWidth, "hWidth") && isWritable(hWidth, "hWidth")) {
            err = spinIntegerGetInc(hWidth, widthInc);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to set width. Aborting with error %d...\n\n", err);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            if (widthToSet % widthInc.get() != 0) {
                widthToSet = (widthToSet / widthInc.get()) * widthInc.get();
            }

            err = spinIntegerSetValue(hWidth, widthToSet);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to set width. Aborting with error %d...\n\n", err);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            printf("\tWidth set to %d...\n", (int) widthToSet);
        } else {
            printf("\tUnable to get or set width; width for sequencer not readable/writable on all camera models...\n");
        }

        // Set height; height recorded in pixels
        spinNodeHandle hHeight = new spinNodeHandle();
        LongPointer heightInc = new LongPointer(1);
        heightInc.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Height"), hHeight);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set height. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (isReadable(hHeight, "hHeight") && isWritable(hHeight, "hHeight")) {
            err = spinIntegerGetInc(hHeight, heightInc);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to set height. Aborting with error %d...\n\n", err);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            if (heightToSet % heightInc.get() != 0) {
                heightToSet = (heightToSet / heightInc.get()) * heightInc.get();
            }

            err = spinIntegerSetValue(hHeight, heightToSet);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to set height. Aborting with error %d...\n\n", err);
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            printf("\tHeight set to %d...\n", (int) heightToSet);
        } else {
            printf("\tUnable to set height; height for sequencer not readable/writable on all camera models...\n");
        }

        // Set exposure time; exposure time recorded in microseconds
        spinNodeHandle hExposureTime = new spinNodeHandle();

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("ExposureTime"), hExposureTime);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hExposureTime, "hExposureTime")) {
            printf("Unable to set exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinFloatSetValue(hExposureTime, exposureTimeToSet);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("\tExposure time set to %f...\n", exposureTimeToSet);

        // Set gain; gain recorded in decibels
        spinNodeHandle hGain = new spinNodeHandle();

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Gain"), hGain);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hGain, "hGain")) {
            printf("Unable to set gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinFloatSetValue(hGain, gainToSet);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("\tGain set to %f...\n", gainToSet);

        //
        // Set the trigger type for the current sequence
        //
        // *** NOTES ***
        // It is a requirement of every state to have its trigger source set.
        // The trigger source refers to the moment when the sequencer changes
        // from one state to the next.
        //
        spinNodeHandle hSequencerTriggerSource = new spinNodeHandle();
        spinNodeHandle hSequencerTriggerSourceFrameStart = new spinNodeHandle();
        LongPointer sequencerTriggerSourceFrameStart = new LongPointer(1);
        sequencerTriggerSourceFrameStart.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerTriggerSource"), hSequencerTriggerSource);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerTriggerSource, "hSequencerTriggerSource")) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerTriggerSource, new BytePointer("FrameStart"), hSequencerTriggerSourceFrameStart);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isAvailableAndReadable(hSequencerTriggerSourceFrameStart, "hSequencerTriggerSourceFrameStart")) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerTriggerSourceFrameStart, sequencerTriggerSourceFrameStart);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerTriggerSource, "hSequencerTriggerSource")) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hSequencerTriggerSource, sequencerTriggerSourceFrameStart.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set trigger source. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("\tTrigger source set to start of frame...\n");

        //
        // Set the next state in the sequence
        //
        // *** NOTES ***
        // When setting the next state in the sequence, ensure it does not
        // exceed the maximum and that the states loop appropriately.
        //
        spinNodeHandle hSequencerSetNext = new spinNodeHandle();
        final int finalSequenceIndex = 4;
        int nextSequence = 0;

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerSetNext"), hSequencerSetNext);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set next sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerSetNext, "hSequencerSetNext")) {
            printf("Unable to set next sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (sequenceNumber != finalSequenceIndex) {
            nextSequence = sequenceNumber + 1;
        }

        err = spinIntegerSetValue(hSequencerSetNext, nextSequence);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set next sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("\tNext sequence set to %d...\n", nextSequence);

        //
        // Save current state
        //
        // *** NOTES ***
        // Once all appropriate settings have been configured, make sure to
        // save the state to the sequence. Notice that these settings will be
        // lost when the camera is power-cycled.
        //
        spinNodeHandle hSequencerSetSave = new spinNodeHandle();

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerSetSave"), hSequencerSetSave);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to save sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerSetSave, "hSequencerSetSave")) {
            printf("Unable to save sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinCommandExecute(hSequencerSetSave);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to save sequence. Aborting with err %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("\tSequence %d saved...\n\n", sequenceNumber);

        return SPINNAKER_ERR_SUCCESS;
    }

    /**
     * Now that the states have all been set, this function readies the camera
     * to use the sequencer during image acquisition.
     */
    private static spinError ConfigureSequencerPartTwo(spinNodeMapHandle hNodeMap) {
        spinError err;

        //
        // Turn configuration mode off
        //
        // *** NOTES ***
        // Once all desired states have been set, turn sequencer
        // configuration mode off in order to turn sequencer mode on.
        //
        spinNodeHandle hSequencerConfigurationMode = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationModeOff = new spinNodeHandle();
        LongPointer sequencerConfigurationModeOff = new LongPointer(1);
        sequencerConfigurationModeOff.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerConfigurationMode"), hSequencerConfigurationMode);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "SequencerConfigurationMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerConfigurationMode, "hSequencerConfigurationMode")) {
            printRetrieveNodeFailure("node", "SequencerConfigurationMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerConfigurationMode, new BytePointer("Off"), hSequencerConfigurationModeOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationMode 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationModeOff, "hSequencerConfigurationModeOff")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationMode 'Off'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerConfigurationModeOff, sequencerConfigurationModeOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable sequencer configuration mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hSequencerConfigurationMode, sequencerConfigurationModeOff.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to disable sequencer configuration mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Sequencer configuration mode disabled...\n");

        //
        // Turn sequencer mode on
        //
        // *** NOTES ***
        // Once sequencer mode is turned on, the camera will begin using the
        // saved states in the order that they were set.
        //
        // *** LATER ***
        // Once all images have been captured, disable the sequencer in order
        // to restore the camera to its initial state.
        //
        spinNodeHandle hSequencerMode = new spinNodeHandle();
        spinNodeHandle hSequencerModeOn = new spinNodeHandle();
        LongPointer sequencerModeOn = new LongPointer(1);
        sequencerModeOn.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerMode"), hSequencerMode);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "SequencerMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerMode, "hSequencerMode")) {
            printRetrieveNodeFailure("node", "SequencerMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerMode, new BytePointer("On"), hSequencerModeOn);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerMode 'On'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerModeOn, "hSequencerModeOn")) {
            printRetrieveNodeFailure("entry", "SequencerMode 'On'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerModeOn, sequencerModeOn);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerMode, "hSequencerMode")) {
            printRetrieveNodeFailure("node", "SequencerMode");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hSequencerMode, sequencerModeOn.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Sequencer mode enabled...\n");

        //
        // Validate sequencer settings
        //
        // *** NOTES ***
        // Once all states have been set, it is a good idea to
        // validate them. Although this node cannot ensure that the states
        // have been set up correctly, it does ensure that the states have
        // been set up in such a way that the camera can function.
        //
        spinNodeHandle hSequencerConfigurationValid = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationValidCurrent = new spinNodeHandle();
        spinNodeHandle hSequencerConfigurationValidYes = new spinNodeHandle();
        LongPointer sequencerConfigurationValidCurrent = new LongPointer(1);
        sequencerConfigurationValidCurrent.put(0);
        LongPointer sequencerConfigurationValidYes = new LongPointer(1);
        sequencerConfigurationValidYes.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerConfigurationValid"), hSequencerConfigurationValid);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("node", "SequencerConfigurationValid");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationValid, "hSequencerConfigurationValid")) {
            printRetrieveNodeFailure("node", "SequencerConfigurationValid");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetCurrentEntry(hSequencerConfigurationValid, hSequencerConfigurationValidCurrent);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid current");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationValidCurrent, "hSequencerConfigurationValidCurrent")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid current");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerConfigurationValid, new BytePointer("Yes"), hSequencerConfigurationValidYes);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid 'Yes'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerConfigurationValidYes, "hSequencerConfigurationValidYes")) {
            printRetrieveNodeFailure("entry", "SequencerConfigurationValid 'Yes'");
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerConfigurationValidCurrent, sequencerConfigurationValidCurrent);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to validate sequencer configuration ('current' value retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerConfigurationValidYes, sequencerConfigurationValidYes);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to validate sequencer configuration ('yes' value retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (sequencerConfigurationValidCurrent.get() != sequencerConfigurationValidYes.get()) {
            err = SPINNAKER_ERR_ERROR;
            printf("Sequencer configuration not valid. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Sequencer configuration valid...\n\n");

        return SPINNAKER_ERR_SUCCESS;
    }

    /**
     * This function restores the camera to its default state by turning sequencer
     * mode off and re-enabling automatic exposure and gain.
     */
    private static spinError resetSequencer(spinNodeMapHandle hNodeMap) {
        spinError err;

        //
        // Turn sequencer mode back off
        //
        // *** NOTES ***
        // The sequencer is turned off in order to return the camera to its default
        // state.
        //
        spinNodeHandle hSequencerMode = new spinNodeHandle();
        spinNodeHandle hSequencerModeOff = new spinNodeHandle();
        LongPointer sequencerModeOff = new LongPointer(1);
        sequencerModeOff.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("SequencerMode"), hSequencerMode);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerMode, "hSequencerMode")) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hSequencerMode, new BytePointer("Off"), hSequencerModeOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hSequencerMode, "hSequencerMode")) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hSequencerModeOff, sequencerModeOff);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hSequencerMode, "hSequencerMode")) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hSequencerMode, sequencerModeOff.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable sequencer mode. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Sequencer mode disabled...\n");

        //
        // Turn automatic exposure back on
        //
        // *** NOTES ***
        // Automatic exposure is turned on in order to return the camera to its
        // default state.
        //
        spinNodeHandle hExposureAuto = new spinNodeHandle();
        spinNodeHandle hExposureAutoContinuous = new spinNodeHandle();
        LongPointer exposureAutoContinuous = new LongPointer(1);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("ExposureAuto"), hExposureAuto);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hExposureAuto, "hExposureAuto")) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hExposureAuto, new BytePointer("Continuous"), hExposureAutoContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hExposureAutoContinuous, "hExposureAutoContinuous")) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hExposureAutoContinuous, exposureAutoContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hExposureAuto, "hExposureAuto")) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hExposureAuto, exposureAutoContinuous.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic exposure. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Automatic exposure enabled...\n");

        //
        // Turn automatic gain back on
        //
        // *** NOTES ***
        // Automatic gain is turned on in order to return the camera to its
        // default state.
        //
        spinNodeHandle hGainAuto = new spinNodeHandle();
        spinNodeHandle hGainAutoContinuous = new spinNodeHandle();
        LongPointer gainAutoContinuous = new LongPointer(1);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("GainAuto"), hGainAuto);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hGainAuto, "hGainAuto")) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hGainAuto, new BytePointer("Continuous"), hGainAutoContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hGainAutoContinuous, "hGainAutoContinuous")) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hGainAutoContinuous, gainAutoContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hGainAuto, "hGainAuto")) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hGainAuto, gainAutoContinuous.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to enable automatic gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Automatic gain enabled...\n\n");

        return SPINNAKER_ERR_SUCCESS;
    }

    //
    // This function acquires and saves 10 images from a device; please see
    // Acquisition_C example for more in-depth comments on the acquisition of
    // images.
    private static spinError acquireImages(spinCamera hCam,
                                           spinNodeMapHandle hNodeMap,
                                           spinNodeMapHandle hNodeMapTLDevice,
                                           int timeout) {
        spinError err;

        printf("\n*** IMAGE ACQUISITION ***\n\n");

        // Set acquisition mode to continuous
        spinNodeHandle hAcquisitionMode = new spinNodeHandle();
        spinNodeHandle hAcquisitionModeContinuous = new spinNodeHandle();
        LongPointer acquisitionModeContinuous = new LongPointer(1);
        acquisitionModeContinuous.put(0);


        err = spinNodeMapGetNode(hNodeMap, new BytePointer("AcquisitionMode"), hAcquisitionMode);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set acquisition mode to continuous (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hAcquisitionMode, "hAcquisitionMode")) {
            printf("Unable to set acquisition mode to continuous (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationGetEntryByName(hAcquisitionMode, new BytePointer("Continuous"), hAcquisitionModeContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set acquisition mode to continuous (entry 'continuous' retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hAcquisitionModeContinuous, "hAcquisitionModeContinuous")) {
            printf("Unable to set acquisition mode to continuous (entry 'continuous' retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationEntryGetIntValue(hAcquisitionModeContinuous, acquisitionModeContinuous);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set acquisition mode to continuous (entry int value retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isWritable(hAcquisitionMode, "hAcquisitionMode")) {
            printf("Unable to set acquisition mode to continuous (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinEnumerationSetIntValue(hAcquisitionMode, acquisitionModeContinuous.get());
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to set acquisition mode to continuous (entry int value setting). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Acquisition mode set to continuous...\n");

        // Begin acquiring images
        err = spinCameraBeginAcquisition(hCam);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to begin image acquisition. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        printf("Acquiring images...\n");

        // Retrieve device serial number for filename
        spinNodeHandle hDeviceSerialNumber = new spinNodeHandle();
        BytePointer deviceSerialNumber = new BytePointer(MAX_BUFF_LEN);
        SizeTPointer lenDeviceSerialNumber = new SizeTPointer(1);
        lenDeviceSerialNumber.put(MAX_BUFF_LEN);

        err = spinNodeMapGetNode(hNodeMapTLDevice, new BytePointer("DeviceSerialNumber"), hDeviceSerialNumber);
        if (printOnError(err, "")) {
            deviceSerialNumber.putString("");
            lenDeviceSerialNumber.put(0);
        } else {
            if (isReadable(hDeviceSerialNumber, "hDeviceSerialNumber")) {
                err = spinStringGetValue(hDeviceSerialNumber, deviceSerialNumber, lenDeviceSerialNumber);
                if (printOnError(err, "")) {
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

        //
        // Create Image Processor context for post processing images
        //
        spinImageProcessor hImageProcessor = new spinImageProcessor();
        err = spinImageProcessorCreate(hImageProcessor);
        printOnError(err, "Unable to create image processor. Non-fatal error.");

        //
        // Set default image processor color processing method
        //
        // *** NOTES ***
        // By default, if no specific color processing algorithm is set, the image
        // processor will default to NEAREST_NEIGHBOR method.
        //
        err = spinImageProcessorSetColorProcessing(hImageProcessor, SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR);
        printOnError(err, "Unable to set image processor color processing method. Non-fatal error.");

        // Retrieve, convert, and save images
        final int k_numImages = 10;
        for (int imageCnt = 0; imageCnt < k_numImages; imageCnt++) {

            // Retrieve next received image
            spinImage hResultImage = new spinImage();

            err = spinCameraGetNextImageEx(hCam, timeout, hResultImage);
            if (printOnError(err, "Unable to get next image. Non-fatal error.")) {
                continue;
            }

            // Ensure image completion
            BytePointer isIncomplete = new BytePointer(1);
            boolean hasFailed = false;

            err = spinImageIsIncomplete(hResultImage, isIncomplete);
            if (printOnError(err, "Unable to determine image completion. Non-fatal error.")) {
                hasFailed = true;
            }

            // Check image for completion
            if (isIncomplete.getBool()) {
                IntPointer imageStatus = new IntPointer(1).put(SPINNAKER_IMAGE_STATUS_NO_ERROR.value);
                err = spinImageGetStatus(hResultImage, imageStatus);
                if (!printOnError(err,
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
                if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                    printf("Unable to release image. Non-fatal error %d...\n\n", err);
                }

                continue;
            }

            // Retrieve image width
            SizeTPointer width = new SizeTPointer(1);
            err = spinImageGetWidth(hResultImage, width);
            if (printOnError(err, "spinImageGetWidth()")) {
                System.out.println("width  = unknown");
            } else {
                System.out.println("width  = " + width.get());
            }

            // Retrieve image height
            SizeTPointer height = new SizeTPointer(1);
            err = spinImageGetHeight(hResultImage, height);
            if (printOnError(err, "spinImageGetHeight()")) {
                System.out.println("height = unknown");
            } else {
                System.out.println("height = " + height.get());
            }

            // Convert image to mono 8
            spinImage hConvertedImage = new spinImage();

            err = spinImageCreateEmpty(hConvertedImage);
            if (printOnError(err, "Unable to create image. Non-fatal error.")) {
                hasFailed = true;
            }

            err = spinImageProcessorConvert(hImageProcessor, hResultImage, hConvertedImage, PixelFormat_Mono8);
            if (printOnError(err, "\"Unable to convert image. Non-fatal error.")) {
                hasFailed = true;
            }

            // Create unique file name
            String filename = (lenDeviceSerialNumber.get() == 0)
                    ? ("Sequencer-C-" + imageCnt + ".jpg")
                    : ("Sequencer-C-" + deviceSerialNumber.getString().trim() + "-" + imageCnt + ".jpg");

            // Save image
            err = spinImageSave(hConvertedImage, new BytePointer(filename), SPINNAKER_IMAGE_FILE_FORMAT_JPEG);
            if (!printOnError(err, "Unable to save image. Non-fatal error.")) {
                System.out.println("Image saved at " + filename + "\n");
            }

            // Destroy converted image
            err = spinImageDestroy(hConvertedImage);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to destroy image. Non-fatal error %d...\n\n", err);
            }

            // Release image
            err = spinImageRelease(hResultImage);
            if (err.intern() != SPINNAKER_ERR_SUCCESS) {
                printf("Unable to release image. Non-fatal error %d...\n\n", err);
            }
        }

        //
        // Destroy Image Processor context
        //
        // *** NOTES ***
        // Image processor context needs to be destroyed after all image processing
        // are complete to avoid memory leaks.
        //
        err = spinImageProcessorDestroy(hImageProcessor);
        printOnError(err, "Unable to destroy image processor. Non-fatal error.");

        // End acquisition
        err = spinCameraEndAcquisition(hCam);
        printOnError(err, "Unable to end acquisition. Non-fatal error.");

        return SPINNAKER_ERR_SUCCESS;
    }

    /**
     * This function acts very similarly to the RunSingleCamera() functions of other
     * examples, except that the values for the sequences are also calculated here;
     * please see NodeMapInfo example for additional information on the steps in
     * this function.
     */
    private static spinError runSingleCamera(spinCamera hCam) {
        spinError err;

        // Retrieve TL device nodemap and print device information
        spinNodeMapHandle hNodeMapTLDevice = new spinNodeMapHandle();
        err = spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice);
        if (!printOnError(err, "Unable to retrieve TL device nodemap .")) {
            err = printDeviceInfo(hNodeMapTLDevice);
        }

        // Initialize camera
        err = spinCameraInit(hCam);
        if (printOnError(err, "Unable to initialize camera.")) {
            return err;
        }

        // Retrieve GenICam nodemap
        spinNodeMapHandle hNodeMap = new spinNodeMapHandle();
        err = spinCameraGetNodeMap(hCam, hNodeMap);
        if (printOnError(err, "Unable to retrieve GenICam nodemap.")) {
            return err;
        }

        // Configure sequencer to be ready to set sequences
        if (configureSequencerPartOne(hNodeMap).intern() != SPINNAKER_ERR_SUCCESS) {
            return SPINNAKER_ERR_ACCESS_DENIED;
        }


        //
        // Set sequences
        //
        // *** NOTES ***
        // In the following section, the sequencer values are calculated. This
        // section does not appear in the configuration, as the values
        // calculated are somewhat arbitrary: width and height are both set to
        // 25% of their maximum values, incrementing by 10%; exposure time is
        // set to its minimum, also incrementing by 10% of its maximum; and gain
        // is set to its minimum, incrementing by 2% of its maximum.
        //
        final int k_numSequences = 5;

        // Retrieve maximum width; width recorded in pixels
        spinNodeHandle hWidth = new spinNodeHandle();
        LongPointer widthMax = new LongPointer(1);
        widthMax.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Width"), hWidth);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to get max width (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hWidth, "hWidth")) {
            printf("Unable to get max width (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinIntegerGetMax(hWidth, widthMax);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to get max width (max retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Retrieve maximum height; height recorded in pixels
        spinNodeHandle hHeight = new spinNodeHandle();
        LongPointer heightMax = new LongPointer(1);
        heightMax.put(1);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Height"), hHeight);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to get max height (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hHeight, "hHeight")) {
            printf("Unable to get max height (node retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinIntegerGetMax(hHeight, heightMax);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to get max height (max retrieval). Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Retrieve maximum exposure time; exposure time recorded in microseconds
        spinNodeHandle hExposureTime = new spinNodeHandle();
        final double exposureTimeMaxToSet = 2000000.0;
        DoublePointer exposureTimeMax = new DoublePointer(1);
        exposureTimeMax.put(0);
        DoublePointer exposureTimeMin = new DoublePointer(1);
        exposureTimeMin.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("ExposureTime"), hExposureTime);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve exposure time node. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hHeight, "hHeight")) {
            printf("Unable to retrieve exposure time node. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinFloatGetMax(hExposureTime, exposureTimeMax);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve maximum exposure time. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (exposureTimeMax.get() > exposureTimeMaxToSet) {
            exposureTimeMax.put(exposureTimeMaxToSet);
        }

        err = spinFloatGetMin(hExposureTime, exposureTimeMin);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve minimum exposure time. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Retrieve maximum and minimum gain; gain recorded in decibels
        spinNodeHandle hGain = new spinNodeHandle();
        DoublePointer gainMax = new DoublePointer(1);
        gainMax.put(0);
        DoublePointer gainMin = new DoublePointer(1);
        gainMin.put(0);

        err = spinNodeMapGetNode(hNodeMap, new BytePointer("Gain"), hGain);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve gain node. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        if (!isReadable(hGain, "hGain")) {
            printf("Unable to retrieve gain node. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinFloatGetMax(hGain, gainMax);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve maximum gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        err = spinFloatGetMin(hGain, gainMin);
        if (err.intern() != SPINNAKER_ERR_SUCCESS) {
            printf("Unable to retrieve minimum gain. Aborting with error %d...\n\n", err);
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Set individual sequences
        long widthToSet = widthMax.get() / 4;
        long heightToSet = heightMax.get() / 4;
        double exposureTimeToSet = exposureTimeMin.get();
        double gainToSet = gainMin.get();

        for (int sequenceNumber = 0; sequenceNumber < k_numSequences; sequenceNumber++) {
            if (setSingleState(hNodeMap, sequenceNumber, widthToSet, heightToSet, exposureTimeToSet, gainToSet).intern() != SPINNAKER_ERR_SUCCESS) {
                return SPINNAKER_ERR_ACCESS_DENIED;
            }

            widthToSet += widthMax.get() / 10;
            heightToSet += heightMax.get() / 10;
            exposureTimeToSet += exposureTimeMax.get() / 10.0;
            gainToSet += gainMax.get() / 50.0;
        }

        // Calculate appropriate acquisition grab timeout window based on exposure time
        // Note: exposureTimeToSet is in microseconds and needs to be converted to milliseconds
        int timeout = (int) ((exposureTimeToSet / 1000) + 1000);

        // Configure sequencer to acquire images
        if (ConfigureSequencerPartTwo(hNodeMap).intern() != SPINNAKER_ERR_SUCCESS) {
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Acquire images
        if (acquireImages(hCam, hNodeMap, hNodeMapTLDevice, timeout).intern() != SPINNAKER_ERR_SUCCESS) {
            return SPINNAKER_ERR_ACCESS_DENIED;
        }

        // Reset sequencer
        if (resetSequencer(hNodeMap).intern() != SPINNAKER_ERR_SUCCESS) {
            return SPINNAKER_ERR_ACCESS_DENIED;
        }


        // Deinitialize camera
        err = spinCameraDeInit(hCam);
        if (printOnError(err, "Unable to deinitialize camera.")) {
            return err;
        }

        return err;
    }

    /**
     * Example entry point; please see Enumeration_C example for more in-depth
     * comments on preparing and cleaning up the system.
     */
    public static void main(String[] args) {
        int errReturn = 0;
        spinError err;

        // Since this application saves images in the current folder
        // we must ensure that we have permission to write to this folder.
        // If we do not have permission, fail right away.
        if (!new File(".").canWrite()) {
            System.out.println("Failed to create file in current folder.  Please check permissions.");
            System.exit(-1);
        }

        // Retrieve singleton reference to system object
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
            System.exit(-1);
        }

        // Run example on each camera
        for (int i = 0; i < numCameras.get(); i++) {
            System.out.println("\nRunning example for camera " + i + "...");

            // Select camera
            spinCamera hCamera = new spinCamera();
            err = spinCameraListGet(hCameraList, i, hCamera);

            if (!printOnError(err, "Unable to retrieve camera from list.")) {

                //
                // Run example
                //
                spinError ret = runSingleCamera(hCamera);
                if (ret.intern() != SPINNAKER_ERR_SUCCESS) {
                    errReturn = -1;
                }
                printOnError(err, "RunSingleCamera");
            }

            // Release camera
            err = spinCameraRelease(hCamera);
            printOnError(err, "Error releasing camera.");
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

        System.out.println("\nDone.");

        System.exit(errReturn);
    }

}
