# =============================================================================
# Terraform Backend Configuration - Azure Blob Storage
# =============================================================================
# Remote state is auto-configured by the CI/CD workflow
# =============================================================================

terraform {
  backend "azurerm" {
    # Backend configuration is provided natively via 'terraform init' CLI arguments in CI/CD.
    # Ex: terraform init -backend-config="resource_group_name=..."
    # Always keep this block empty to avoid coupling infra code to a specific state environment.
  }
}
