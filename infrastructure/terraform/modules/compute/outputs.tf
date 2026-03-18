output "vm_public_ip" {
  description = "Public IP address of the VM"
  value       = azurerm_linux_virtual_machine.main.public_ip_address
}

output "vm_name" {
  description = "Name of the VM"
  value       = azurerm_linux_virtual_machine.main.name
}

output "admin_username" {
  description = "VM admin username"
  value       = var.admin_username
}

output "ssh_connection_string" {
  description = "SSH command to connect to the VM"
  value       = "ssh ${var.admin_username}@${azurerm_public_ip.main.ip_address}"
}

output "vm_identity_client_id" {
  description = "Client ID of the User Assigned Identity"
  value       = azurerm_user_assigned_identity.vm.client_id
}
