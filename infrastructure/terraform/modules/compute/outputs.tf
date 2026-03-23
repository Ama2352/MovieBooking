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

# ============================================================================
# NGINX & HTTPS Endpoints
# ============================================================================
output "http_endpoint" {
  description = "HTTP endpoint (auto-redirects to HTTPS)"
  value       = "http://${azurerm_public_ip.main.ip_address}"
}

output "https_endpoint" {
  description = "HTTPS endpoint with self-signed certificate (development)"
  value       = "https://${azurerm_public_ip.main.ip_address}"
}

output "https_endpoint_note" {
  description = "Note about certificate"
  value       = "Self-signed certificate - add browser exception when accessing by public IP"
}

output "nginx_status_url" {
  description = "Internal NGINX health check endpoint"
  value       = "http://${azurerm_public_ip.main.ip_address}:8888/health"
}

output "certificate_path" {
  description = "Path to SSL certificate on VM"
  value       = "/opt/moviebooking/nginx/certs/"
}
