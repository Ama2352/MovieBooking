# =============================================================================
# MovieBooking Network Module - Azure Resources
# =============================================================================

locals {
  tags = {
    Project     = var.project_name
    Environment = "development"
    ManagedBy   = "terraform"
    Purpose     = "k6-load-testing"
  }
}

# -----------------------------------------------------------------------------
# Azure Resource Group
# -----------------------------------------------------------------------------
resource "azurerm_resource_group" "main" {
  name     = "${var.project_name}-rg"
  location = var.region
  tags     = local.tags
}

# -----------------------------------------------------------------------------
# Azure Virtual Network
# -----------------------------------------------------------------------------
resource "azurerm_virtual_network" "main" {
  name                = "${var.project_name}-vnet"
  address_space       = [var.cidr_block]
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

# -----------------------------------------------------------------------------
# Public Subnet
# -----------------------------------------------------------------------------
resource "azurerm_subnet" "public" {
  name                 = "${var.project_name}-public-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.public_subnet]
}

# -----------------------------------------------------------------------------
# Private Subnet (for future use)
# -----------------------------------------------------------------------------
resource "azurerm_subnet" "private" {
  name                 = "${var.project_name}-private-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.private_subnet]
}
