# =============================================================================
# MovieBooking Azure Infrastructure
# =============================================================================

# -----------------------------------------------------------------------------
# Network Module
# -----------------------------------------------------------------------------
module "network" {
  source = "./modules/network"

  project_name   = var.project_name
  region         = var.region
  cidr_block     = var.cidr_block
  public_subnet  = var.public_subnet
  private_subnet = var.private_subnet
}

# -----------------------------------------------------------------------------
# Compute Module
# -----------------------------------------------------------------------------
module "compute" {
  source = "./modules/compute"

  project_name        = var.project_name
  region              = var.region
  resource_group_name = module.network.resource_group_name
  subnet_id           = module.network.public_subnet_id

  vm_size             = var.vm_size
  admin_username      = var.admin_username
  ssh_public_key_path = var.ssh_public_key_path
  os_disk_size_gb     = var.os_disk_size_gb
  data_disk_size_gb   = var.data_disk_size_gb
  allowed_ssh_cidr    = var.allowed_ssh_cidr
}

# -----------------------------------------------------------------------------
# Outputs
# -----------------------------------------------------------------------------
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

output "grafana_endpoint" {
  description = "Grafana dashboard endpoint"
  value       = "http://${module.compute.vm_public_ip}:3000"
}

output "admin_username" {
  description = "VM admin username"
  value       = module.compute.admin_username
}
