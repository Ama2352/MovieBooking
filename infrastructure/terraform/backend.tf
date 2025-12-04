# =============================================================================
# Terraform Backend Configuration - Azure Blob Storage
# =============================================================================
# Remote state is auto-configured by the CI/CD workflow
# =============================================================================

terraform {
  backend "azurerm" {
    # These values are set dynamically by CI/CD via -backend-config flags:
    # resource_group_name  = "moviebooking-tfstate-rg"
    # storage_account_name = "moviebookingtfstate"
    # container_name       = "tfstate"
    # key                  = "terraform.tfstate"
  }
}
