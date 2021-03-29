package saxon.board.radiona.ulx3s

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}

// Define a SoC abstract enough to be used for simulation
class Ulx3sMinimalAbstract extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val gpioA = BmbGpioGenerator(0x00000)
  val uartA = BmbUartGenerator(0x10000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.ctrl),
    cpu.dBus -> List(ramA.ctrl, peripheralBridge.input)
  )
}

class Ulx3sMinimal extends Component{
  // Define the clock domains used by the SoC
  val debugCdCtrl = ClockDomainResetGenerator()
  debugCdCtrl.holdDuration.load(4095)
  debugCdCtrl.enablePowerOnReset()
  debugCdCtrl.makeExternal(FixedFrequency(25 MHz))

  val systemCdCtrl = ClockDomainResetGenerator()
  systemCdCtrl.holdDuration.load(63)
  systemCdCtrl.asyncReset(debugCdCtrl)
  systemCdCtrl.setInput(
    debugCdCtrl.outputClockDomain,
    omitReset = true
  )

  val debugCd  = debugCdCtrl.outputClockDomain
  val systemCd = systemCdCtrl.outputClockDomain

  val system = systemCd on new Ulx3sMinimalAbstract(){
    cpu.enableJtag(debugCdCtrl, systemCdCtrl)
  }
}

object Ulx3sMinimalAbstract{
  def default(g : Ulx3sMinimalAbstract) = g.rework {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}

object Ulx3sMinimal {
  //Function used to configure the SoC
  def default(g : Ulx3sMinimal) = g.rework {
    import g._
    Ulx3sMinimalAbstract.default(system)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sMinimal())))
    BspGenerator("Ulx3sMinimal", report.toplevel, report.toplevel.system.cpu.dBus)
  }
}

