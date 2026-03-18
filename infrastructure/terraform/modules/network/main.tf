# =============================================================================
# MovieBooking Network Module - Azure Resources
# =============================================================================

locals {
  tags = {
    Project     = var.project_name
    Environment = "development"
    ManagedBy   = "terraform"
  }
}

# -----------------------------------------------------------------------------
# Azure Resource Group
# -----------------------------------------------------------------------------
data "azurerm_resource_group" "main" {
  name = "${var.project_name}-tfstate-rg"
}

# -----------------------------------------------------------------------------
# Azure Virtual Network
# -----------------------------------------------------------------------------
resource "azurerm_virtual_network" "main" {
  name                = "${var.project_name}-vnet"
  address_space       = [var.cidr_block]
  location            = data.azurerm_resource_group.main.location
  resource_group_name = data.azurerm_resource_group.main.name
  tags                = local.tags
}

# -----------------------------------------------------------------------------
# Public Subnet
# -----------------------------------------------------------------------------
resource "azurerm_subnet" "public" {
  name                 = "${var.project_name}-public-subnet"
  resource_group_name  = data.azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.public_subnet]
}
