package saxon

import spinal.core._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.com.jtag.{Jtag, JtagTapInstructionCtrl}
import spinal.lib.generator.{BmbInterconnectGenerator, Dependable, Generator, Handle}
import spinal.lib.slave
import vexriscv.{VexRiscv, VexRiscvConfig}
import vexriscv.plugin.{CsrPlugin, DBusCachedPlugin, DBusSimpleBus, DBusSimplePlugin, DebugPlugin, IBusCachedPlugin, IBusSimpleBus, IBusSimplePlugin}


object VexRiscvBmbGenerator{
  val DEBUG_NONE = 0
  val DEBUG_JTAG = 1
  val DEBUG_JTAG_CTRL = 2
}

case class VexRiscvBmbGenerator()(implicit interconnect: BmbInterconnectGenerator = null) extends Generator {
  import VexRiscvBmbGenerator._

  val config = Handle[VexRiscvConfig]
  val withDebug = Handle[Int]
  val debugClockDomain = Handle[ClockDomain]
  val debugReset = Handle[Bool]
  val debugAskReset = Handle[() => Unit]
  val hardwareBreakpointCount = Handle(0)

  val iBus, dBus = product[Bmb]

  val externalInterrupt = product[Bool]
  val externalSupervisorInterrupt = product[Bool]
  val timerInterrupt = product[Bool]

  def setTimerInterrupt(that: Handle[Bool]) = Dependable(that, timerInterrupt){timerInterrupt := that}


  def disableDebug() = withDebug.load(DEBUG_NONE)

  def enableJtag(implicit clockCtrl: ClockDomainGenerator) : Unit = {
    this.debugClockDomain.merge(clockCtrl.controlClockDomain)
    debugAskReset.merge(clockCtrl.doSystemReset)
    withDebug.load(DEBUG_JTAG)
  }

  def enableJtag(debugCd : ClockDomainResetGenerator, resetCd : ClockDomainResetGenerator) : Unit = {
    this.debugClockDomain.merge(debugCd.outputClockDomain)
    val resetBridge = resetCd.asyncReset(debugReset, ResetSensitivity.HIGH)
    debugAskReset.load(null)
    withDebug.load(DEBUG_JTAG)
  }

  def enableJtagInstructionCtrl(debugCd : ClockDomainResetGenerator, resetCd : ClockDomainResetGenerator) : Unit = {
    this.debugClockDomain.merge(debugCd.outputClockDomain)
    val resetBridge = resetCd.asyncReset(debugReset, ResetSensitivity.HIGH)
    debugAskReset.load(null)
    withDebug.load(DEBUG_JTAG_CTRL)
    dependencies += jtagClockDomain
  }


  dependencies ++= List(config)
  dependencies += Dependable(withDebug) {
    if (withDebug != DEBUG_NONE) {
      dependencies ++= List(debugClockDomain, debugAskReset)
    }
  }

  val jtag = add task (withDebug.get == DEBUG_JTAG generate slave(Jtag()))
  val jtagInstructionCtrl = add task (withDebug.get == DEBUG_JTAG_CTRL generate JtagTapInstructionCtrl())
  val jtagClockDomain = Handle[ClockDomain]

  val logic = add task new Area {
    withDebug.get != DEBUG_NONE generate new Area {
      config.add(new DebugPlugin(debugClockDomain, hardwareBreakpointCount))
    }

    val cpu = new VexRiscv(config)
    for (plugin <- cpu.plugins) plugin match {
      case plugin: IBusSimplePlugin => iBus.load(plugin.iBus.toBmb())
      case plugin: DBusSimplePlugin => dBus.load(plugin.dBus.toBmb())
      case plugin: IBusCachedPlugin => iBus.load(plugin.iBus.toBmb())
      case plugin: DBusCachedPlugin => dBus.load(plugin.dBus.toBmb())
      case plugin: CsrPlugin => {
        externalInterrupt load plugin.externalInterrupt
        timerInterrupt load plugin.timerInterrupt
        if (plugin.config.supervisorGen) externalSupervisorInterrupt load plugin.externalInterruptS
      }
      case plugin: DebugPlugin => plugin.debugClockDomain {
        if(debugAskReset.get != null) when(RegNext(plugin.io.resetOut)) {
          debugAskReset.get()
        } else {
          debugReset.load(RegNext(plugin.io.resetOut))
        }

        withDebug.get match {
          case DEBUG_JTAG => jtag <> plugin.io.bus.fromJtag()
          case DEBUG_JTAG_CTRL => jtagInstructionCtrl <> plugin.io.bus.fromJtagInstructionCtrl(jtagClockDomain)
        }
      }
      case _ =>
    }
  }

  val parameterGenerator = new Generator {
    val iBusParameter, dBusParameter = product[BmbParameter]
    dependencies += config

    add task {
      for (plugin <- config.plugins) plugin match {
        case plugin: IBusSimplePlugin => iBusParameter.load(IBusSimpleBus.getBmbParameter())
        case plugin: DBusSimplePlugin => dBusParameter.load(DBusSimpleBus.getBmbParameter())
        case plugin: IBusCachedPlugin => iBusParameter.load(plugin.config.getBmbParameter())
        case plugin: DBusCachedPlugin => dBusParameter.load(plugin.config.getBmbParameter())
        case _ =>
      }
    }
  }

  if (interconnect != null) {
    interconnect.addMaster(parameterGenerator.iBusParameter, iBus)
    interconnect.addMaster(parameterGenerator.dBusParameter, dBus)
  }
}
