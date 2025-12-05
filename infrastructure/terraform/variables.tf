# =============================================================================
# MovieBooking Infrastructure Variables
# =============================================================================

# -----------------------------------------------------------------------------
# Project Settings
# -----------------------------------------------------------------------------
variable "project_name" {
  description = "Name of the project (used for naming resources)"
  type        = string
  default     = "moviebooking"
}

variable "region" {
  description = "Azure region to deploy resources"
  type        = string
  default     = "southeastasia"
}

# -----------------------------------------------------------------------------
# Network Settings
# -----------------------------------------------------------------------------
variable "cidr_block" {
  description = "CIDR block for VNet"
  type        = string
  default     = "10.1.0.0/16"
}

variable "public_subnet" {
  description = "Public subnet CIDR"
  type        = string
  default     = "10.1.1.0/24"
}

variable "private_subnet" {
  description = "Private subnet CIDR"
  type        = string
  default     = "10.1.2.0/24"
}

# -----------------------------------------------------------------------------
# Azure Credentials (set via environment variables)
# -----------------------------------------------------------------------------
variable "azure_subscription_id" {
  description = "Azure Subscription ID"
  type        = string
  default     = ""
  sensitive   = true
}

variable "azure_client_id" {
  description = "Azure Client ID (Service Principal)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "azure_client_secret" {
  description = "Azure Client Secret"
  type        = string
  default     = ""
  sensitive   = true
}

variable "azure_tenant_id" {
  description = "Azure Tenant ID"
  type        = string
  default     = ""
  sensitive   = true
}

# -----------------------------------------------------------------------------
# VM Configuration
# -----------------------------------------------------------------------------
variable "vm_size" {
  description = "Size of the Azure VM"
  type        = string
  default     = "Standard_B2s" # 2 vCPU, 4GB RAM
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
  description = "Size of the data disk in GB"
  type        = number
  default     = 64
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH into the VM"
  type        = string
  default     = "0.0.0.0/0"
}

variable "additional_ssh_keys" {
  description = "Additional SSH public keys to add to the VM (for local machine access)"
  type        = list(string)
  default     = []
}
