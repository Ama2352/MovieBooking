# =============================================================================
# MovieBooking Network Module - Variables
# =============================================================================

variable "project_name" {
  description = "Name of the project (used for resource naming)"
  type        = string
}

variable "region" {
  description = "Azure region for deployment"
  type        = string
}

variable "cidr_block" {
  description = "CIDR block for the virtual network"
  type        = string
  default     = "10.1.0.0/16"
}

variable "public_subnet" {
  description = "CIDR block for public subnet"
  type        = string
  default     = "10.1.1.0/24"
}

variable "private_subnet" {
  description = "CIDR block for private subnet"
  type        = string
  default     = "10.1.2.0/24"
}
