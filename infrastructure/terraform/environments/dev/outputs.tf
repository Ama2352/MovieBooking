# =============================================================================
# Outputs
# =============================================================================

output "resource_group_name" {
  description = "Azure Resource Group name"
  value       = module.network.resource_group_name
}

output "vm_public_ip" {
  description = "Public IP address of the VM"
  value       = module.compute.vm_public_ip
}

output "ssh_command" {
  description = "SSH command to connect to the VM"
  value       = module.compute.ssh_connection_string
}

output "api_endpoint" {
  description = "MovieBooking API endpoint"
  value       = "http://${module.compute.vm_public_ip}:8080"
}

output "admin_username" {
  description = "VM admin username"
  value       = module.compute.admin_username
}

output "acr_name" {
  description = "Azure Container Registry Name"
  value       = module.acr.acr_name
}

output "acr_login_server" {
  description = "Azure Container Registry Login Server"
  value       = module.acr.acr_login_server
}

output "key_vault_name" {
  description = "Azure Key Vault Name"
  value       = module.keyvault.key_vault_name
}

output "vm_name" {
  description = "Virtual Machine Name"
  value       = module.compute.vm_name
}

output "vm_identity_client_id" {
  description = "Virtual Machine Identity Client ID"
  value       = module.compute.vm_identity_client_id
}
