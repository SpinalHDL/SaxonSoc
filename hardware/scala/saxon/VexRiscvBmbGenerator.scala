package saxon

import spinal.core._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.com.jtag.Jtag
import spinal.lib.generator.{BmbInterconnectGenerator, Dependable, Generator, Handle}
import spinal.lib.slave
import vexriscv.{VexRiscv, VexRiscvConfig}
import vexriscv.plugin.{CsrPlugin, DBusCachedPlugin, DBusSimpleBus, DBusSimplePlugin, DebugPlugin, IBusCachedPlugin, IBusSimpleBus, IBusSimplePlugin}


object VexRiscvBmbGenerator {
  def apply(withJtag: Handle[Boolean],
            debugClockDomain: Handle[ClockDomain],
            debugAskReset: Handle[() => Unit])
           (implicit interconnect: BmbInterconnectGenerator): VexRiscvBmbGenerator = {
    val g = VexRiscvBmbGenerator()
    g.withJtag.merge(withJtag)
    g.debugClockDomain.merge(debugClockDomain)
    g.debugAskReset.merge(debugAskReset)
    g
  }
}

case class VexRiscvBmbGenerator(/*debugAskReset : Handle[() => Unit] = Unset,
                                withJtag : Handle[Boolean] = Unset,
                                debugClockDomain : Handle[ClockDomain] = Unset*/)(implicit interconnect: BmbInterconnectGenerator = null) extends Generator {
  val config = Handle[VexRiscvConfig]
  val withJtag = Handle[Boolean]
  val debugClockDomain = Handle[ClockDomain]
  val debugAskReset = Handle[() => Unit]

  val iBus, dBus = product[Bmb]
  val externalInterrupt, timerInterrupt = product[Bool]

  def setExternalInterrupt(that: Handle[Bool]) = externalInterrupt.merge(that)

  def setTimerInterrupt(that: Handle[Bool]) = timerInterrupt.merge(that)

  dependencies ++= List(config)
  dependencies += Dependable(withJtag) {
    if (withJtag) {
      dependencies ++= List(debugClockDomain, debugAskReset)
    }
  }
  dependencies += Dependable(config) {
    if (config.plugins.exists(_.isInstanceOf[CsrPlugin])) {
      dependencies ++= List(externalInterrupt, timerInterrupt)
    }
  }

  val jtag = add task (withJtag.get generate slave(Jtag()))
  val logic = add task new Area {
    withJtag.get generate new Area {
      config.add(new DebugPlugin(debugClockDomain, 2))
    }

    val cpu = new VexRiscv(config)
    for (plugin <- cpu.plugins) plugin match {
      case plugin: IBusSimplePlugin => iBus.load(plugin.iBus.toBmb())
      case plugin: DBusSimplePlugin => dBus.load(plugin.dBus.toBmb())
      case plugin: IBusCachedPlugin => iBus.load(plugin.iBus.toBmb())
      case plugin: DBusCachedPlugin => dBus.load(plugin.dBus.toBmb())
      case plugin: CsrPlugin => {
        externalInterrupt <> (plugin.externalInterrupt)
        timerInterrupt <> (plugin.timerInterrupt)
        if (plugin.config.supervisorGen) plugin.externalInterruptS := False
      }
      case plugin: DebugPlugin => plugin.debugClockDomain {
        when(RegNext(plugin.io.resetOut)) {
          debugAskReset.get()
        }
        jtag.value <> plugin.io.bus.fromJtag()
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
