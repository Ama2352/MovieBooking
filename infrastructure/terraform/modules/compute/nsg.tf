locals {
  inbound_rules = {
    ssh   = { priority = 100, port = "22", source = var.allowed_ssh_cidr }
    http  = { priority = 110, port = "80", source = "*" }
    https = { priority = 120, port = "443", source = "*" }
    api   = { priority = 130, port = "8080", source = "*" } # Consider restricting this rule
  }
}

resource "azurerm_network_security_group" "main" {
  name                = "${var.project_name}-nsg"
  location            = var.region
  resource_group_name = var.resource_group_name

  dynamic "security_rule" {
    for_each = local.inbound_rules
    content {
      name                       = security_rule.key
      priority                   = security_rule.value.priority
      direction                  = "Inbound"
      access                     = "Allow"
      protocol                   = "Tcp"
      source_port_range          = "*"
      destination_port_range     = security_rule.value.port
      source_address_prefix      = security_rule.value.source
      destination_address_prefix = "*"
    }
  }
}
