package saxon.board.ulx3s

import spinal.core._
import spinal.lib._
import spinal.lib.generator.Generator

case class Ulx3sNoReset() extends Component {
 val io = new Bundle {
   val wifi_gpio0 = out Bool
 }

 io.wifi_gpio0 := True
}

case class Ulx3sNoResetGenerator() extends Generator {
  val wifi_gpio0 = produceIo(logic.io.wifi_gpio0)

  val logic = add task Ulx3sNoReset()
}
