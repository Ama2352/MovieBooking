# =============================================================================
# MovieBooking Compute Module - Outputs
# =============================================================================

output "vm_id" {
  description = "The ID of the Azure VM"
  value       = azurerm_linux_virtual_machine.main.id
}

output "vm_name" {
  description = "The name of the Azure VM"
  value       = azurerm_linux_virtual_machine.main.name
}

output "vm_public_ip" {
  description = "The public IP address of the VM"
  value       = azurerm_public_ip.main.ip_address
}

output "vm_private_ip" {
  description = "The private IP address of the VM"
  value       = azurerm_network_interface.main.private_ip_address
}

output "ssh_connection_string" {
  description = "SSH command to connect to the VM"
  value       = "ssh ${var.admin_username}@${azurerm_public_ip.main.ip_address}"
}

output "admin_username" {
  description = "Admin username for SSH"
  value       = var.admin_username
}
