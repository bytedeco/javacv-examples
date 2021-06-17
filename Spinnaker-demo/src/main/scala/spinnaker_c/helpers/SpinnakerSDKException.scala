package spinnaker_c.helpers

import org.bytedeco.spinnaker.global.Spinnaker_C.spinError

class SpinnakerSDKException(message: String, val error: spinError) extends Exception(message)
