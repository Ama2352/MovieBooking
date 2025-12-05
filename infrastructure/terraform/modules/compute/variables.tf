# =============================================================================
# MovieBooking Compute Module - Variables
# =============================================================================

variable "project_name" {
  description = "Name of the project (used for resource naming)"
  type        = string
}

variable "region" {
  description = "Azure region for deployment"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the Azure Resource Group"
  type        = string
}

variable "subnet_id" {
  description = "ID of the subnet to deploy VM into"
  type        = string
}

# -----------------------------------------------------------------------------
# VM Configuration
# -----------------------------------------------------------------------------
variable "vm_size" {
  description = "Size of the Azure VM"
  type        = string
  default     = "Standard_B2s" # 2 vCPU, 4GB RAM - budget friendly
}

variable "admin_username" {
  description = "Admin username for the VM"
  type        = string
  default     = "azureuser"
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key file"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "os_disk_size_gb" {
  description = "Size of the OS disk in GB"
  type        = number
  default     = 32
}

variable "data_disk_size_gb" {
  description = "Size of the data disk in GB (0 = no data disk)"
  type        = number
  default     = 64
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH into the VM"
  type        = string
  default     = "0.0.0.0/0"
}

variable "additional_ssh_keys" {
  description = "Additional SSH public keys to add to the VM"
  type        = list(string)
  default     = []
}
