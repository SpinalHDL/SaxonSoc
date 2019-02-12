package saxon.experimental

import spinal.core._


object Keys{

  val A = new Key[Int]
  val B = new Key[Int]
}


class KeyAPlugin(value : Int) extends Plugin {
  override def locks = Seq(Keys.A)

  override lazy val logic = {
    println(s"set Key A" + value)
    Keys.A.set(value)
  }
}

class AdderPlugin(width : Int) extends Plugin{
  override def dependancies = Seq(Keys.A)
  override def locks = Seq(Keys.B)


  override lazy val logic = new Area{
    println(s"Build " + width)
    val a, b = in UInt (width bits)
    val result = out UInt (width bits)
    result := a + b + Keys.A.get
    Keys.B.set(42)
  }
}

class KeyBPlugin extends Plugin {
  override def dependancies = Seq(Keys.B)

  override lazy val logic = {
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