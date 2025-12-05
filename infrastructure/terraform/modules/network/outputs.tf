# =============================================================================
# MovieBooking Network Module - Outputs
# =============================================================================

output "network_id" {
  description = "The ID of the Azure Virtual Network"
  value       = azurerm_virtual_network.main.id
}

output "network_name" {
  description = "The name of the Azure Virtual Network"
  value       = azurerm_virtual_network.main.name
}

output "public_subnet_id" {
  description = "Public subnet ID"
  value       = azurerm_subnet.public.id
}

output "private_subnet_id" {
  description = "Private subnet ID"
  value       = azurerm_subnet.private.id
}

output "resource_group_name" {
  description = "The name of the Azure Resource Group"
  value       = azurerm_resource_group.main.name
}

output "resource_group_location" {
  description = "The location of the Azure Resource Group"
  value       = azurerm_resource_group.main.location
}
