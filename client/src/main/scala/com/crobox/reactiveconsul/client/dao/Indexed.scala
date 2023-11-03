package com.crobox.reactiveconsul.client.dao

trait Indexed[T] {
  def index: Long
  def resource: T
}
