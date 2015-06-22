package xebia.consul.example

import java.util.UUID

import org.joda.time.DateTime
import org.scalatest.{Matchers, FunSuite}

class PetClinicModelSpec extends FunSuite with Matchers {

  test("owners can be saved and retrieved") {
    val sut = new PetClinicModel
    val owner = Owner(UUID.randomUUID(), "Firstname", "Lastname", "Addresss 42", "Town", "+31611111111")
    sut.Owner.save(owner)
    val found = sut.Owner.findById(owner.id)
    found should contain (owner)
  }

  test("owners can be found by last name") {
    val sut = new PetClinicModel
    val owner1 = Owner(UUID.randomUUID(), "Firstname", "Lastname", "Addresss 42", "Town", "+31611111111")
    val owner2 = Owner(UUID.randomUUID(), "Firstname", "Lastnameson", "Addresss 42", "Town", "+31611111111")
    val owner3 = Owner(UUID.randomUUID(), "Firstname", "Lastnam", "Addresss 42", "Town", "+31611111111")

    sut.Owner.save(owner1)
    sut.Owner.save(owner2)
    sut.Owner.save(owner3)
    val found = sut.Owner.findByLastName("Lastname")
    found should contain allOf(owner1, owner2)
  }

  test("pet types can be found") {
    val sut = new PetClinicModel
    sut.Pet.findPetTypes should have size 6
  }

  test("pets can be saved and retrieved") {
    val sut = new PetClinicModel
    val owner = Owner(UUID.randomUUID(), "Firstname", "Lastname", "Addresss 42", "Town", "+31611111111")
    val pet = Pet(UUID.randomUUID(), "Fido", new DateTime("2007-1-1"), Type("dog"), owner)
    sut.Pet.save(pet)
    sut.Pet.findById(pet.id) should contain(pet)
  }
}
