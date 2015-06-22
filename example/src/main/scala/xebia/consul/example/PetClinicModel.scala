package xebia.consul.example

import java.util.UUID

import org.joda.time.DateTime

import scala.collection.SortedMap

class PetClinicModel {

  val vetLastNameIndex = SortedMap.empty[String, Vet]
  val specialtyNameIndex = SortedMap.empty[String, Specialty]
  var ownerLastNameIndex = SortedMap.empty[String, Owner]
  var ownerIdIndex = Map.empty[UUID, Owner]
  var petNameIndex = SortedMap.empty[String, Pet]
  var petIdIndex = Map.empty[UUID, Pet]
  var petTypes = Set(Type("cat"), Type("dog"), Type("lizard"), Type("snake"), Type("bird"), Type("hamster"))

  implicit class SearchableStringMap[T](map: SortedMap[String, T]) {
    def startingWith(prefix: String): Set[T] = {
      map.range(prefix, prefix + Character.MAX_VALUE).values.toSet
    }
  }

  object Owner {
    def findByLastName(prefix: String): Set[Owner] = ownerLastNameIndex.startingWith(prefix)
    def findById(id: UUID): Option[Owner] = ownerIdIndex.get(id)
    def save(owner: Owner): Unit = {
      ownerLastNameIndex += owner.lastName -> owner
      ownerIdIndex += owner.id -> owner
    }
  }

  object Pet {
    def findPetTypes: Set[Type] = petTypes
    def findById(id: UUID): Option[Pet] = petIdIndex.get(id)
    def save(pet: Pet): Unit = {
      petNameIndex += pet.name -> pet
      petIdIndex += pet.id -> pet
    }
  }
}

case class Vet(firstName: String, lastName: String, specialties: Set[Specialty] = Set.empty)
case class Specialty(name: String)
case class Type(name: String)
case class Owner(id: UUID, firstName: String, lastName: String, address: String, city: String, telephone: String)
case class Pet(id: UUID, name: String, birthDate: DateTime, typ: Type, owner: Owner)
case class Visit(pet: Pet, visitDate: DateTime, description: String)
