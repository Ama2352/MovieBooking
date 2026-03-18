locals {
  tags = {
    Project     = var.project_name
    Environment = "development"
    ManagedBy   = "terraform"
  }
}

data "azurerm_client_config" "current" {}

resource "random_id" "kv" {
  byte_length = 4
}

resource "azurerm_key_vault" "main" {
  name                       = lower(replace(substr("${var.project_name}kv${random_id.kv.hex}", 0, 24), "-", ""))
  location                   = var.region
  resource_group_name        = var.resource_group_name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true

  tags = local.tags
}
