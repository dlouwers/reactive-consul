package xebia.consul.example

import java.util.UUID

import akka.actor.{Actor, Props}

class PetClinicActor extends Actor {
  
  val model = new PetClinicModel()

  def receive = {
    case PetClinicActor.Owner.FindByLastName(name) => sender ! model.Owner.findByLastName(name)
    case PetClinicActor.Owner.FindById(id) => sender ! model.Owner.findById(id)
    case PetClinicActor.Owner.Save(owner) => model.Owner.save(owner)
  }
}

object PetClinicActor {
  def props = Props(new PetClinicActor)
  object Owner {
    case class FindByLastName(lastName: String)
    case class FindById(id: UUID)
    case class Save(owner: Owner)
  }
}