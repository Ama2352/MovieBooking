resource "azurerm_user_assigned_identity" "vm" {
  name                = "${var.project_name}-vm-identity"
  resource_group_name = var.resource_group_name
  location            = var.region
}

resource "azurerm_role_assignment" "kv_secrets_user" {
  scope                = var.key_vault_id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.vm.principal_id
}
