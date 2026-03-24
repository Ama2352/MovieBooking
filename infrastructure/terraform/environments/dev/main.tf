# =============================================================================
# MovieBooking Azure Infrastructure
# =============================================================================

# -----------------------------------------------------------------------------
# Network Module
# -----------------------------------------------------------------------------
module "network" {
  source = "../../modules/network"

  project_name  = var.project_name
  region        = var.region
  cidr_block    = var.cidr_block
  public_subnet = var.public_subnet
}

# -----------------------------------------------------------------------------
# Compute Module
# -----------------------------------------------------------------------------
module "compute" {
  source = "../../modules/compute"

  project_name        = var.project_name
  region              = var.region
  resource_group_name = module.network.resource_group_name
  subnet_id           = module.network.public_subnet_id

  vm_size             = var.vm_size
  admin_username      = var.admin_username
  ssh_public_key      = var.ssh_public_key
  os_disk_size_gb     = var.os_disk_size_gb
  data_disk_size_gb   = var.data_disk_size_gb
  allowed_ssh_cidr    = var.allowed_ssh_cidr
  additional_ssh_keys = var.additional_ssh_keys

  key_vault_id = module.keyvault.key_vault_id
}


# -----------------------------------------------------------------------------
# Key Vault Module
# -----------------------------------------------------------------------------
module "keyvault" {
  source = "../../modules/keyvault"

  project_name        = var.project_name
  region              = var.region
  resource_group_name = module.network.resource_group_name
}
