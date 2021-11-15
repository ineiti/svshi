package ch.epfl.core.compiler.groupAddressAssigner

import ch.epfl.core.models.physical.{GroupAddress, PhysicalStructure}
import ch.epfl.core.models.prototypical.AppPrototypicalStructure

trait GroupAddressAssignerTrait {
  def assignGroupAddressesToPhysical(physStructure: PhysicalStructure): Map[Int, GroupAddress]

}
object GroupAddressAssigner extends GroupAddressAssignerTrait {
  override def assignGroupAddressesToPhysical(physStructure: PhysicalStructure): Map[Int, GroupAddress] = ???
}