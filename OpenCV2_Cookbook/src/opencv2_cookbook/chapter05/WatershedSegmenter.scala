/*
 * Copyright (c) 2011 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter05

import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_core._


/**
 * Helper class for section "Segmenting images using watersheds".
 */
class WatershedSegmenter {

    private var _markers: IplImage = _


    def setMarkers(markerImage: IplImage) {
        _markers = cvCreateImage(cvGetSize(markerImage), IPL_DEPTH_32S, 1 /* channels */)
        cvConvertScale(markerImage, _markers, 1 /* scale */ , 0 /* shift */)
    }


    def process(image: IplImage): IplImage = {
        cvWatershed(image, _markers)
        _markers
    }


    def getSegmentation: IplImage = {
        // all segment with label higher than 255
        // will be assigned value 255
        val result = cvCreateImage(cvGetSize(_markers), IPL_DEPTH_8U, 1 /* channels */)
        cvConvertScale(_markers, result, 1 /* scale */ , 0 /* shift */)
        result
    }


    def getWatersheds: IplImage = {
        val result = cvCreateImage(cvGetSize(_markers), IPL_DEPTH_8U, 1 /* channels */)
        cvConvertScale(_markers, result, 255 /* scale */ , 255 /* shift */)
        result
    }
}