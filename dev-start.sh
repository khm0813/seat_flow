#!/bin/bash

# SeatFlow Development Environment Startup Script

echo "🎭 Starting SeatFlow Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker-compose -f docker-compose.dev.yml down

# Remove old volumes (optional - uncomment if you want fresh data)
# echo "🗑️  Removing old volumes..."
# docker volume prune -f

# Build and start services
echo "🚀 Building and starting services..."
docker-compose -f docker-compose.dev.yml up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."

# Function to check service health
check_service() {
    local service_name=$1
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f docker-compose.dev.yml ps $service_name | grep -q "healthy\|Up"; then
            echo "✅ $service_name is ready"
            return 0
        fi
        echo "⏳ Waiting for $service_name... (attempt $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ $service_name failed to start"
    return 1
}

# Check each service
check_service "postgres"
check_service "redis"
check_service "backend"
check_service "frontend"

echo ""
echo "🎉 SeatFlow Development Environment is ready!"
echo ""
echo "📍 Service URLs:"
echo "   Frontend (Hot Reload): http://localhost:5173"
echo "   Backend API:          http://localhost:8080/api"
echo "   Health Check:         http://localhost:8080/api/actuator/health"
echo ""
echo "🔧 Development Features:"
echo "   ✨ Frontend hot reload enabled"
echo "   ✨ Backend continuous build enabled"
echo "   ✨ Source code changes auto-applied"
echo ""
echo "📁 Edit files in:"
echo "   Backend:  ./backend/src/"
echo "   Frontend: ./frontend/src/"
echo ""
echo "🛠️  Useful Commands:"
echo "   View logs:    docker-compose -f docker-compose.dev.yml logs -f"
echo "   Stop all:     docker-compose -f docker-compose.dev.yml down"
echo "   Restart:      ./dev-start.sh"
echo ""
echo "Happy coding! 🚀"