package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.generator._

class SubGenerator extends Generator {
  val x = Handle[Bool]
  add task {
    x.load(True)
  }
  val y = produce(job.a)
  val job = add task new Area{
    val a = False
  }

  val z = produce(CombInit(job.a))
  val job2 = add task new Area{
    val a = False
  }
}

class TopGenerator extends Generator {
  val sub = new SubGenerator()
}

object TopMain extends App{
  SpinalVerilog{
    new TopGenerator().toComponent()
  }
  println("done")
}
