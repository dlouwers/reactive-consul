package com.crobox.reactiveconsul.client.dao

import java.util

case class BinaryData(data: Array[Byte]) {

  override def equals(other: Any): Boolean = {
    if (this.canEqual(other)) {
      util.Arrays.equals(this.data, other.asInstanceOf[BinaryData].data)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    util.Arrays.hashCode(this.data)
  }

  override def toString: String = {
    "[ " + this.data.map(_.toString).mkString(", ") + " ]"
  }
}
