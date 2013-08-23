package metascala
package full

//import metascala.{UncaughtVmException, Gen, Util}
import org.scalatest.FreeSpec
import metascala.Util._

import collection.GenSeq
import metascala.features.Bull

object MetacircularTest{

  def sqrtFinder = {
    val x = new metascala.VM(memorySize = 1024)
    x.invoke("metascala/features/controlflow/Loops", "sqrtFinder", Seq(5.0))
  }

  def fibonacci = {
    val x = new metascala.VM()
    x.invoke("metascala/features/methods/Statics", "fibonacci", Seq(12))
  }

  def doInheritance = {
    val b = new Bull
    b.mooTwice
  }
  def inheritance = {
    val x = new metascala.VM()
    x.invoke("metascala/full/MetacircularTest", "doInheritance", Nil)
  }

  def bubbleSort = {
    val x = new metascala.VM()
    x.invoke("metascala/features/ArrayTest", "bubbleSort", Seq(Array(6, 5, 2, 7, 3, 4, 9, 1, 8)))
     .cast[Array[Int]]
     .toSeq
  }
  def getAndSet = {
    val x = new metascala.VM()
    x.invoke("metascala/features/arrays/MultiDimArrays", "getAndSet")
  }
  def multiCatch = {
    val x = new metascala.VM()
    x.invoke("metascala/features/exceptions/Exceptions", "multiCatch", Seq(2))
  }

  def doubleMetaOne = {
    val x = new metascala.VM()
    x.invoke("metascala.full.MetacircularTest", "doubleMetaTwo")
  }
  def doubleMetaTwo = {
    val x = new metascala.VM()
    x.invoke("metascala.features.controlflow.Loops", "sqrtFinder", Seq(5.0))

  }
  def helloWorld = {
    println("Hello Scala!")
  }
}

class MetacircularTest extends FreeSpec {
  import Util._

  val buffer = new BufferLog(1900)
  var count = 0



//  "helloWorld" in {
//    tester.run("helloWorld")
//  }
/*
  "sqrtFinder" in {
    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 256 * 1024)
    for(i <- 0 to 2)tester.run("sqrtFinder")

  }

  "fibonacci" in {
    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 3 * 1024 * 1024)
    for(i <- 0 to 2) tester.run("fibonacci")
  }

  "inheritance" in {
    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 8 * 1014 * 1024)
    tester.run("inheritance")
  }

  "bubbleSort" in {
    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 6 * 1014 * 1024)
    tester.run("bubbleSort")
  }

  "getAndSet" in {
    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 15 * 1014 * 1024)
    tester.run("getAndSet")
  }

  "multiCatch" in {

    val tester = new Tester("metascala.full.MetacircularTest", memorySize = 16 * 1014 * 1024)
    tester.run("multiCatch")
  }*/
  /*"doubleMetaOne" in {
    tester.run("doubleMetaOne")
    println(tester.svm.threads(0).getOpCount)
  }*/
}

