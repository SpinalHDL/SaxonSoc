package saxon

import spinal.core._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.com.jtag.Jtag
import spinal.lib.generator.{BmbInterconnectGenerator, Dependable, Generator, Handle}
import spinal.lib.slave
import vexriscv.{VexRiscv, VexRiscvConfig}
import vexriscv.plugin.{CsrPlugin, DBusCachedPlugin, DBusSimpleBus, DBusSimplePlugin, DebugPlugin, IBusCachedPlugin, IBusSimpleBus, IBusSimplePlugin}




case class VexRiscvBmbGenerator()(implicit interconnect: BmbInterconnectGenerator = null) extends Generator {
  val config = Handle[VexRiscvConfig]
  val withJtag = Handle[Boolean]
  val debugClockDomain = Handle[ClockDomain]
  val debugAskReset = Handle[() => Unit]
  val hardwareBreakpointCount = Handle(0)

  val iBus, dBus = product[Bmb]
  val externalInterrupt, externalSupervisorInterrupt, timerInterrupt = product[Bool]

  def setTimerInterrupt(that: Handle[Bool]) = Dependable(that, timerInterrupt){timerInterrupt := that}

  def enableJtag(implicit clockCtrl: ClockDomainGenerator) : Unit = {
    this.debugClockDomain.merge(clockCtrl.controlClockDomain())
    debugAskReset.merge(clockCtrl.doSystemReset)
    withJtag.load(true)
  }

  dependencies ++= List(config)
  dependencies += Dependable(withJtag) {
    if (withJtag) {
      dependencies ++= List(debugClockDomain, debugAskReset)
    }
  }

  val jtag = add task (withJtag.get generate slave(Jtag()))
  val logic = add task new Area {
    withJtag.get generate new Area {
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
        when(RegNext(plugin.io.resetOut)) {
          debugAskReset.get()
        }
        jtag <> plugin.io.bus.fromJtag()
      }
      case _ =>
    }
  }

  val parameterGenerator = wrap(new Generator {
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
  })

  if (interconnect != null) {
    interconnect.addMaster(parameterGenerator.iBusParameter, iBus)
    interconnect.addMaster(parameterGenerator.dBusParameter, dBus)
  }
}
