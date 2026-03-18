output "acr_name" {
  description = "The name of the Container Registry"
  value       = azurerm_container_registry.main.name
}

output "acr_login_server" {
  description = "The login server URL of the Container Registry"
  value       = azurerm_container_registry.main.login_server
}

output "acr_id" {
  description = "ARM ID of the ACR for Role Assignments"
  value       = azurerm_container_registry.main.id
}
