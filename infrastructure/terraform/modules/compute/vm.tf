# -----------------------------------------------------------------------------
# Azure Virtual Machine and Role Assignments
# -----------------------------------------------------------------------------
resource "azurerm_linux_virtual_machine" "main" {
  name                = "${var.project_name}-vm"
  location            = var.region
  resource_group_name = var.resource_group_name
  size                = var.vm_size
  admin_username      = var.admin_username

  admin_ssh_key {
    username   = var.admin_username
    public_key = var.ssh_public_key
  }

  dynamic "admin_ssh_key" {
    for_each = var.additional_ssh_keys
    content {
      username   = var.admin_username
      public_key = admin_ssh_key.value
    }
  }

  network_interface_ids = [azurerm_network_interface.main.id]

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
    disk_size_gb         = var.os_disk_size_gb
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  identity {
    type         = "SystemAssigned, UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.vm.id]
  }

  custom_data = base64encode(local.cloud_init_script)
  tags        = local.tags
}

