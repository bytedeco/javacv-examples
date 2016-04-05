Simple Video Stabilization
================

Example of using [JavaCV](https://github.com/bytedeco/javacv) for [post process video stabilization](https://cseweb.ucsd.edu/classes/fa03/cse252c/projects/skchow.pdf)

Please address queries and questions to [JavaCV discussion group](https://groups.google.com/forum/#!forum/javacv).


How to test
------------------------------
**This assumes you have a complete working setup for Android Studio and Javacv. If not, refer to [Android dev](http://developer.android.com/sdk/index.html) and [JavaCV](https://github.com/bytedeco/javacv)** . Sample tested with Android studio. Configuration is in [build.gradle](videoStablizaton/app/build.gradle)

* Obtain the `videoStabilization` code
* Import project into Android studio(File->New->Import Project)
* In the file MainActivity.java, change `private final String inputVideo = "/storage/emulated/0/video/testVid_2.avi";`
    `private final String outputVideo = "/storage/emulated/0/video/stabVideo.avi";`
to your input and output file respectively

Config used
--------------------------
This can be found in build.gradle(Module:app)
