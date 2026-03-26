#!/bin/bash
set -e

echo "=== ChatApp AWS EC2 Deploy Script ==="

# Check if docker and docker-compose are installed
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    sudo yum update -y
    sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "Docker installed. Please log out and back in, then run this script again."
    exit 0
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose installed."
fi

# Clone or pull latest code
if [ -d "chatapp" ]; then
    echo "Pulling latest code..."
    cd chatapp
    git pull origin main
else
    echo "Cloning repository..."
    git clone https://github.com/KARTHI141/chatapp.git
    cd chatapp
fi

# Check for .env.production
if [ ! -f .env.production ]; then
    echo ""
    echo "ERROR: .env.production file not found!"
    echo "Create it with: nano .env.production"
    echo "Required variables:"
    echo "  MYSQL_ROOT_PASSWORD=YourStrongPassword"
    echo "  JWT_SECRET=your-jwt-secret"
    echo "  MAIL_USERNAME=your-email@gmail.com"
    echo "  MAIL_PASSWORD=your-app-password"
    echo "  CORS_ALLOWED_ORIGINS=http://YOUR_EC2_PUBLIC_IP"
    exit 1
fi

echo "Building and starting services..."
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d --build

echo ""
echo "=== Deployment Complete ==="
echo "Waiting for services to start..."
sleep 10
docker-compose -f docker-compose.prod.yml ps
echo ""
echo "App is running at: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo 'YOUR_EC2_PUBLIC_IP')"
