import spinal.core.SpinalConfig
import spinal.core.fiber.Handle

package object saxon {
  def SpinalRtlConfig = {
    Handle.loadHandleAsync = true // For compatibility
    SpinalConfig(targetDirectory = "hardware/netlist")
  }
}
