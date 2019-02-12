package saxon.experimental

import spinal.core._


object Keys{

  val A = new Handle[Int]
  val B = new Handle[Int]
}


class KeyAPlugin(value : Int) extends Plugin {
  locks ++= Seq(Keys.A)

  val logic = add task {
    println(s"set Key A" + value)
    Keys.A.set(value)
  }
}

class AdderPlugin(width : Int) extends Plugin{
  dependencies ++= Seq(Keys.A)
  locks ++= Seq(Keys.B)


  val logic = add task new Area{
    println(s"Build " + width)
    val a, b = in UInt (width bits)
    val result = out UInt (width bits)
    result := a + b + Keys.A.get
    Keys.B.set(42)
  }
}

class KeyBPlugin extends Plugin {
  dependencies ++= Seq(Keys.B)

  val logic = add task {
    println(s"Key B=" + Keys.B.get)

  }
}




class ComposablePlay(plugins : Seq[Plugin]) extends Component{
  val c = new Composable()
  c.plugins ++= plugins
  c.build()
}

object ComposablePlay{
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new ComposablePlay(
      List(
        new KeyBPlugin,
        new AdderPlugin(16).setName("miaou"),
        new KeyAPlugin(8),
        new AdderPlugin(8).setName("toto")
      )
    ))
  }
}