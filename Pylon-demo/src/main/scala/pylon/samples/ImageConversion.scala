package pylon.samples

import java.io.File

import org.bytedeco.javacpp.Pylon5._

/**
  * @author Jarek Sacha 
  */
object ImageConversion extends App {

  val imageFile = new File("src/test/data/skansen-01.jpg")
  require(imageFile.exists())

  val srcImage = new CPylonImage()

  srcImage.Load(asGCString(imageFile.getCanonicalPath))
  println("width: " + srcImage.GetWidth())
  println("height: " + srcImage.GetHeight())

  // First the image format converter class must be created.
  val converter = new CImageFormatConverter()

  // Second the converter must be parameterized.
  converter.OutputPixelFormat().put(PixelType_Mono8)
  converter.OutputBitAlignment().put(OutputBitAlignment_MsbAligned)

  // Create a target image
  val targetImage = new CPylonImage()

  // Convert the image. Note that there are more overloaded Convert methods available, e.g.
  // for converting the image from or to a user buffer.
  converter.Convert(targetImage, srcImage)

  // Save converted image
  targetImage.Save(ImageFileFormat_Png, asGCString("converted-sample-m8.png"))
}
