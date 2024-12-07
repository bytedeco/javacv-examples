/*
 * Image/J Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
 * Author's email: jpsacha at gmail [dot] com
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
 * Latest release available at https://github.com/ij-plugins/ijp-DeBayer2SX
 */

package libraw.benchmark

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class BenchmarkHelper(val testIter: Int) {
  private val _results = mutable.LinkedHashMap.empty[String, ListBuffer[Double]]

  def results: immutable.Map[String, List[Double]] = {
    _results.map { case (k, v) => (k, v.toList) }.toMap
  }

  def measure[A, R](tag: String, arg: A, op: (A) => R): Unit = {
    val t0 = System.currentTimeMillis()
    for (_ <- 0 until testIter) {
      op(arg)
    }
    val delta = (System.currentTimeMillis() - t0) / testIter.toDouble
    _results.getOrElseUpdate(tag, ListBuffer.empty[Double]) += delta
    println(f"$tag: $delta%7.2f ms.")
  }

  def printResults(): Unit = {
    val r = results
    val maxTagLength = r.keys.map(_.length).max
    r.foreach { case (tag, values) =>
      val p = maxTagLength - tag.length
      val t = tag + " " * p
      val minVal = values.min
      println(f"  $t: $minVal%7.2f ms.")
    }
  }
}
