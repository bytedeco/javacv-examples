/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */
package flycapture.examples.cpp

import org.bytedeco.javacpp.FlyCapture2.Error

class FC2Exception(message: String, val error: Error) extends Exception(message: String)
