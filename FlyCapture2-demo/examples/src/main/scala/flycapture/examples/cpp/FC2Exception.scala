/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */
package flycapture.examples.cpp

import org.bytedeco.flycapture.FlyCapture2._

class FC2Exception(message: String, val error: Error) extends Exception(message: String)
