package com.crobox.reactiveconsul.client.discovery

trait ConnectionProviderFactory {
  def create(host: String, port: Int): ConnectionProvider
}
