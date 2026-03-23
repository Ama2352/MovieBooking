locals {
  tags = {
    Project     = var.project_name
    Environment = "development"
    ManagedBy   = "terraform"
  }

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
      - openssl

    runcmd:
      # Install Docker
      - curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
      - echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
      - apt-get update
      - apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      
      # Install Azure CLI
      - curl -sL https://aka.ms/InstallAzureCLIDeb | bash
      
      # Add admin user to docker group
      - usermod -aG docker ${var.admin_username}
      
      # Start Docker service
      - systemctl enable docker
      - systemctl start docker
      
      # Create directories for MovieBooking
      - mkdir -p /opt/moviebooking
      - mkdir -p /opt/moviebooking/nginx/conf.d
      - mkdir -p /opt/moviebooking/nginx/certs
      - mkdir -p /opt/moviebooking/logs/nginx
      - mkdir -p /data/postgres
      - mkdir -p /data/redis
      
      # Set permissions
      - chown -R ${var.admin_username}:${var.admin_username} /opt/moviebooking
      - chmod 755 /opt/moviebooking
      - chown -R ${var.admin_username}:${var.admin_username} /data
      
      # Generate self-signed certificate for development
      - openssl req -x509 -newkey rsa:4096 -keyout /opt/moviebooking/nginx/certs/moviebooking.key -out /opt/moviebooking/nginx/certs/moviebooking.crt -days 365 -nodes -subj "/C=VN/ST=Ho Chi Minh/L=Ho Chi Minh/O=MovieBooking/CN=moviebooking.local"
      - chmod 644 /opt/moviebooking/nginx/certs/moviebooking.crt
      - chmod 600 /opt/moviebooking/nginx/certs/moviebooking.key
      
      # Log completion
      - echo "MovieBooking VM ready at $(date)" >> /var/log/cloud-init-output.log
      - echo "Self-signed certificate generated at $(date)" >> /var/log/cloud-init-output.log

    final_message: "MovieBooking VM is ready after $UPTIME seconds"
  EOF
}
