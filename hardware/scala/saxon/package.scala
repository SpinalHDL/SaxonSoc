import spinal.core.SpinalConfig

package object saxon {
  def wrap[T](body : => T) = body
  def SpinalRtlConfig = SpinalConfig(targetDirectory = "hardware/netlist")
}
