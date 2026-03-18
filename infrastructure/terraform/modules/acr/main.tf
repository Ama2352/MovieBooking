locals {
  # ACR name must be alphanumeric only and globally unique (within Azure)
  # Ensure no hyphens and convert to lowercase
  acr_name = lower(replace("${var.project_name}registry", "-", ""))
}

resource "azurerm_container_registry" "main" {
  name                = local.acr_name
  resource_group_name = var.resource_group_name
  location            = var.region
  sku                 = "Basic"
  admin_enabled       = true # Useful for fallback, though workflows will primarily use OIDC
  
  tags = {
    Project     = var.project_name
    Environment = "development"
    ManagedBy   = "terraform"
  }
}
