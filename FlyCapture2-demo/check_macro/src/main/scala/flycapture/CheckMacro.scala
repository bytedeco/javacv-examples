/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture

import org.bytedeco.flycapture.FlyCapture2.Error

import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Converts FlyCapture2 error codes to exceptions (if different than `PGRERROR_OK`).
 *
 * Uses Scala macros to provide additional information about the immediate code that produced an error code.
 *
 * Based on examples provided at [[https://github.com/underscoreio/essential-macros]]
 *
 * @author Jarek Sacha 
 */
object CheckMacro {
  /**
   * Checks FlyCapture2 error code.
   * If the code is different than `PGRERROR_OK` generated description of expression that produced
   * the error code, including values of variables used in the expression.
   * Throws `FC2Exception` with the description and the value of the error code.
   *
   * Helper for using FlyCapture2 C++ API.
   *
   * Example use:
   * {{{
   *   check( busMgr.GetCameraFromIndex(0, guid) )
   * }}}
   *
   * @param expr expression producing an error code.
   */
  def check(expr: Error): Unit = macro CheckMacro.checkMacro
}

class CheckMacro(val c: blackbox.Context) {

  import c.universe._

  // Helper class used to cache information about temporary
  // variables introduced during expansion:
  //
  //  - `original` is the original expression fragment,
  //    the value of which is stored in the temporary variable.
  //
  //    Although `original` is not embedded directly anywhere
  //    in the final code, its value is stored so its printed
  //    form can be included in error messages from failed
  //    assertions.
  //
  //    Example: `a.b(c.d, e.f, g.h)`
  //
  //  - `transformed` is the transformed expression used to
  //    actually calculate the value stored in the variable.
  //
  //    This includes references to temporary variables storing
  //    values from sub-expressions from `original`.
  //
  //    Example: `temp1.b(temp2, temp3, temp4)`
  case class TempVar(original: c.Tree, transformed: c.Tree) {
    // The name of the temporary variable.
    //
    // See the following (step 10 onwards) for a discussion of name generation:
    //     https://github.com/scalamacros/macrology201/commits/part1
    //
    // Note that this will fail for certain exotic types of arguments
    // due to an owner chain corruption issue. See steps 19 to 23 above
    // for a detailed explanation and workaround.
    val name: c.universe.TermName = c.freshName(TermName("temp"))

    // An expression that references this variable,
    // used below to create the `transformed` form of
    // parent expresions:
    val ident = q"$name"

    // An statement that declares this variable:
    val decl = q"val $name = $transformed"

    // An expression that renders this variable as a string,
    // used to generate error messages for failed assertions:
    val debug = q""" ${showCode(original)} + " = " + $ident """
  }

  // The actual implementation of the assert macro:

  def checkMacro(expr: c.Tree): c.Tree = {
    // We create a mutable buffer of temporary variables
    // and recursively transform `expr`, caching the temporary
    // variables allocated as we go:
    val tempVars = new mutable.ArrayBuffer[TempVar]()

    // Helper method to create and store a temporary variable:
    def tempVar(original: c.Tree, transformed: c.Tree) = {
      val tempVar = TempVar(original, transformed)
      tempVars += tempVar
      tempVar
    }

    // Recursive method that transforms an expression, extracts
    // printable sub-expressions, and caches them in `tempVars`.
    //
    // The return value is the cached temporary variable:
    def transform(tree: c.Tree): c.Tree = tree match {
      // Method application:
      case q"$recv.$method(..$args)" =>
        val newRecv = transform(recv)
        val newArgs = args.map(transform)
        tempVar(tree, q"$newRecv.$method(..$newArgs)").ident

      // Field selection:
      case q"$recv.$field" =>
        val newRecv = transform(recv)
        tempVar(tree, q"$newRecv.$field").ident

      // Plain identifier:
      case ident: Ident =>
        tempVar(ident, ident).ident

      // Anything else is ignored:
      case other =>
        other
    }

    // Transform the whole of `expr`, storing a number of
    // temporary variables as a side-effect:
    val transformed =
      transform(expr)

    // Once all temporary variables have been calculated,
    // we can generate the final assertion code using the
    // contents of `tempVars`:
    val result =
      q"""
      ..${tempVars.map(_.decl)}
      if( $transformed.GetType() != org.bytedeco.flycapture.global.FlyCapture2.PGRERROR_OK ) {
        val errorDesc = $transformed.GetDescription().getString
        val printedCode = ${show(expr)}
        val printedValues = List(..${tempVars.map(_.debug)})
        throw new flycapture.examples.cpp.FC2Exception(
          "FlyCapture2 error type : " + $transformed.GetType() +
          "\n  FlyCapture2 error description: " + errorDesc +
          "\n  Expression: " + printedCode +
          printedValues.mkString("\n    ", "\n    ", ""),
          $transformed
        )
      }
      """

    // println(show(result))

    result
  }

}
