# =============================================================================
# MovieBooking Terraform Providers
# =============================================================================

terraform {
  required_version = ">= 1.0.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

# -----------------------------------------------------------------------------
# Azure Provider Configuration
# -----------------------------------------------------------------------------
provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
    virtual_machine {
      delete_os_disk_on_deletion     = true
      skip_shutdown_and_force_delete = false
    }
  }

  # Credentials can be provided via:
  # 1. Environment variables (TF_VAR_azure_*)
  # 2. terraform.tfvars
  # 3. Azure CLI login (az login)

  subscription_id = var.azure_subscription_id != "" ? var.azure_subscription_id : null
  client_id       = var.azure_client_id != "" ? var.azure_client_id : null
  client_secret   = var.azure_client_secret != "" ? var.azure_client_secret : null
  tenant_id       = var.azure_tenant_id != "" ? var.azure_tenant_id : null
}
