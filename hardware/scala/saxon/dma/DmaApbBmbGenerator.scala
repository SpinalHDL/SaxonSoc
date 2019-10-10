//package saxon.dma
//
//import saxon.{Apb3DecoderGenerator, InterruptCtrl}
//import spinal.core._
//import spinal.lib.bus.bmb.BmbParameter
//import spinal.lib.generator._
//
//
//
//case class  DmaApbBmbGenerator(apbOffset : BigInt)
//                             (implicit decoder: Apb3DecoderGenerator, interconnect: BmbInterconnectGenerator) extends Generator{
//  val parameter = createDependency[Dma.Parameter]
//  val apb = produce(logic.io.config)
//  val bmb = produce(logic.io.mem.toBmb())
//  val bmbRequirements = parameter produce parameter.memConfig.toBmbConfig()
//
//  decoder.addSlave(apb, apbOffset)
//  interconnect.addMaster(bmbRequirements, bmb)
//
//  val logic = add task Dma.Dma(parameter)
//}
