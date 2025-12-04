# =============================================================================
# MovieBooking Compute Module - Azure VM
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
# Public IP Address
# -----------------------------------------------------------------------------
resource "azurerm_public_ip" "main" {
  name                = "${var.project_name}-pip"
  location            = var.region
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = local.tags
}

# -----------------------------------------------------------------------------
# Network Security Group
# -----------------------------------------------------------------------------
resource "azurerm_network_security_group" "main" {
  name                = "${var.project_name}-nsg"
  location            = var.region
  resource_group_name = var.resource_group_name
  tags                = local.tags
}

# SSH Rule
resource "azurerm_network_security_rule" "ssh" {
  name                        = "SSH"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "22"
  source_address_prefix       = var.allowed_ssh_cidr
  destination_address_prefix  = "*"
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.main.name
}

# Spring Boot API (8080) - Public for k6 testing
resource "azurerm_network_security_rule" "api" {
  name                        = "SpringBootAPI"
  priority                    = 110
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "8080"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.main.name
}

# Grafana (3000)
resource "azurerm_network_security_rule" "grafana" {
  name                        = "Grafana"
  priority                    = 120
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "3000"
  source_address_prefix       = var.allowed_ssh_cidr
  destination_address_prefix  = "*"
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.main.name
}

# Prometheus (9090)
resource "azurerm_network_security_rule" "prometheus" {
  name                        = "Prometheus"
  priority                    = 130
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "9090"
  source_address_prefix       = var.allowed_ssh_cidr
  destination_address_prefix  = "*"
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.main.name
}

# Outbound - Allow All
resource "azurerm_network_security_rule" "outbound" {
  name                        = "AllowAllOutbound"
  priority                    = 200
  direction                   = "Outbound"
  access                      = "Allow"
  protocol                    = "*"
  source_port_range           = "*"
  destination_port_range      = "*"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = var.resource_group_name
  network_security_group_name = azurerm_network_security_group.main.name
}

# -----------------------------------------------------------------------------
# Network Interface
# -----------------------------------------------------------------------------
resource "azurerm_network_interface" "main" {
  name                = "${var.project_name}-nic"
  location            = var.region
  resource_group_name = var.resource_group_name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = var.subnet_id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.main.id
  }

  tags = local.tags
}

# Associate NSG with NIC
resource "azurerm_network_interface_security_group_association" "main" {
  network_interface_id      = azurerm_network_interface.main.id
  network_security_group_id = azurerm_network_security_group.main.id
}

# -----------------------------------------------------------------------------
# Cloud-init Script
# -----------------------------------------------------------------------------
locals {
  cloud_init_script = <<-EOF
    #cloud-config
    package_update: true
    package_upgrade: true
    
    packages:
      - apt-transport-https
      - ca-certificates
      - curl
      - gnupg
      - lsb-release
      - git
      - htop
      - jq

    runcmd:
      # Install Docker
      - curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
      - echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
      - apt-get update
      - apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      
      # Add admin user to docker group
      - usermod -aG docker ${var.admin_username}
      
      # Start Docker service
      - systemctl enable docker
      - systemctl start docker
      
      # Create directories for MovieBooking
      - mkdir -p /opt/moviebooking
      - mkdir -p /data/postgres
      - mkdir -p /data/redis
      - mkdir -p /data/grafana
      - mkdir -p /data/prometheus
      
      # Set permissions
      - chown -R ${var.admin_username}:${var.admin_username} /opt/moviebooking
      - chown -R ${var.admin_username}:${var.admin_username} /data
      
      # Log completion
      - echo "MovieBooking VM ready at $(date)" >> /var/log/cloud-init-output.log

    final_message: "MovieBooking VM is ready after $UPTIME seconds"
  EOF
}

# -----------------------------------------------------------------------------
# Azure Virtual Machine
# -----------------------------------------------------------------------------
resource "azurerm_linux_virtual_machine" "main" {
  name                = "${var.project_name}-vm"
  location            = var.region
  resource_group_name = var.resource_group_name
  size                = var.vm_size
  admin_username      = var.admin_username

  admin_ssh_key {
    username   = var.admin_username
    public_key = file(var.ssh_public_key_path)
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

  custom_data = base64encode(local.cloud_init_script)
  tags        = local.tags
}

# -----------------------------------------------------------------------------
# Data Disk for Docker Volumes
# -----------------------------------------------------------------------------
resource "azurerm_managed_disk" "data" {
  count                = var.data_disk_size_gb > 0 ? 1 : 0
  name                 = "${var.project_name}-data-disk"
  location             = var.region
  resource_group_name  = var.resource_group_name
  storage_account_type = "Standard_LRS"
  create_option        = "Empty"
  disk_size_gb         = var.data_disk_size_gb
  tags                 = local.tags
}

resource "azurerm_virtual_machine_data_disk_attachment" "data" {
  count              = var.data_disk_size_gb > 0 ? 1 : 0
  managed_disk_id    = azurerm_managed_disk.data[0].id
  virtual_machine_id = azurerm_linux_virtual_machine.main.id
  lun                = 0
  caching            = "ReadWrite"
}
