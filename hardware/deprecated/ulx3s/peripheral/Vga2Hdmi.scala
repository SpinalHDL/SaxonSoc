package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.graphic.vga._
import spinal.lib.graphic._

class Vga2Hdmi(rgbConfig: RgbConfig = RgbConfig(8, 8, 8)) extends BlackBox {
  val io = new Bundle {
    val pixclk = in Bool()
    val pixclk_x5 = in Bool()
    val vga = slave(Vga(rgbConfig))
    val hdmi = master(Hdmi())
  }

  noIoPrefix()
  setBlackBoxName("hdmi")
  io.vga.color.r.setName("red")
  io.vga.color.g.setName("green")
  io.vga.color.b.setName("blue")
  io.vga.colorEn.setName("vde")
  io.vga.hSync.setName("hSync")
  io.vga.vSync.setName("vSync")
  io.hdmi.gpdi_dp.setName("gpdi_dp")
  io.hdmi.gpdi_dn.setName("gpdi_dn")
}

