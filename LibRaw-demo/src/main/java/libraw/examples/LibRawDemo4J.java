/*
 * LibRaw Examples
 * Copyright (C) 2022 Jarek Sacha
 * Author's email: jpsacha at gmail dot com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at https://github.com/ij-plugins/ijp-dcraw
 */

package libraw.examples;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.libraw.LibRaw;
import org.bytedeco.libraw.libraw_output_params_t;

import java.io.File;

import static org.bytedeco.libraw.global.LibRaw.LibRaw_errors;
import static org.bytedeco.libraw.global.LibRaw.libraw_strerror;

public class LibRawDemo4J {
    public static String libRawVersion() {
        try (BytePointer version = LibRaw.version()) {
            return version.getString();
        }
    }

    public static void handleError(int err, String message) {
        if (err != LibRaw_errors.LIBRAW_SUCCESS.value) {
            final String msg;
            try (BytePointer e = libraw_strerror(err)) {
                msg = e.getString();
            }
            System.err.println(message + " : " + msg);
            System.exit(err);
        }
    }

    public static void main(String[] args) {
        System.out.println("");
        System.out.println("LibRaw.version(): " + libRawVersion());

        String srcFile = (args.length > 0) ? args[0] : "data/IMG_5604.CR2";

        try (LibRaw rawProcessor = new LibRaw()) {

            // Set some processing parameters
            libraw_output_params_t params = rawProcessor.imgdata().params();
            params.half_size(1); // Create half size image
            params.output_tiff(1); // Save as TIFF

            System.out.println("Reading raw image: " + srcFile);
            int err = rawProcessor.open_file(srcFile);
            handleError(err, "Cannot open " + srcFile);

            System.out.println("Unpacking");
            err = rawProcessor.unpack();
            handleError(err, "Cannot unpack " + srcFile);

            System.out.println("Processing");
            err = rawProcessor.dcraw_process();
            handleError(err, "Cannot process" + srcFile);

            System.out.println("Decoded image info");
            System.out.println(" width : " + rawProcessor.imgdata().sizes().width());
            System.out.println(" height: " + rawProcessor.imgdata().sizes().height());

            // Prepare output file
            File dstDir = new File("tmp");
            if (!dstDir.exists()) {
                if (!dstDir.mkdirs()) {
                    System.err.println("Cannot create output directory: " + dstDir.getAbsolutePath());
                    System.exit(-1);
                }
            }
            String ext = params.output_tiff() == 0 ? "ppm" : "tif";
            File dstFile = new File(dstDir, "output." + ext);

            // Write image to the output file
            System.out.println("Writing processed image: " + dstFile.getPath());
            err = rawProcessor.dcraw_ppm_tiff_writer(dstFile.getAbsolutePath());
            handleError(err, "Cannot write " + dstFile.getAbsolutePath());

            System.out.println("Cleaning up");
            rawProcessor.recycle();
        }


        System.out.println("Done");
        System.exit(LibRaw_errors.LIBRAW_SUCCESS.value);
    }
}
