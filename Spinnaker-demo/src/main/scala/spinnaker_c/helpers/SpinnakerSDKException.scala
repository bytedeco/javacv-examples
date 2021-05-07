package spinnaker_c.helpers

import org.bytedeco.spinnaker.global.Spinnaker_C._spinError

class SpinnakerSDKException(message: String, val error: _spinError) extends Exception(message)
