# SeatFlow Project Makefile
# ========================

.PHONY: help dev prod stop clean logs test build restart status install

# Default target
.DEFAULT_GOAL := help

# Colors for output
YELLOW := \033[33m
GREEN := \033[32m
BLUE := \033[34m
RED := \033[31m
RESET := \033[0m

# Docker Compose files
DEV_COMPOSE := docker-compose.dev.yml
PROD_COMPOSE := docker-compose.yml

## Help
help: ## Show this help message
	@echo "$(BLUE)🎭 SeatFlow Development Commands$(RESET)"
	@echo ""
	@echo "$(GREEN)Development:$(RESET)"
	@echo "  make dev       - Start development environment (Hot Reload)"
	@echo "  make stop      - Stop all services"
	@echo "  make restart   - Restart development environment"
	@echo "  make logs      - Show logs from all services"
	@echo "  make status    - Show status of all services"
	@echo ""
	@echo "$(GREEN)Production:$(RESET)"
	@echo "  make prod      - Start production environment"
	@echo "  make prod-stop - Stop production environment"
	@echo ""
	@echo "$(GREEN)Development Tools:$(RESET)"
	@echo "  make test      - Run all tests"
	@echo "  make build     - Build all services without starting"
	@echo "  make clean     - Clean up containers, volumes, and images"
	@echo "  make install   - Install dependencies locally"
	@echo ""
	@echo "$(GREEN)Debugging:$(RESET)"
	@echo "  make logs-backend  - Show backend logs only"
	@echo "  make logs-frontend - Show frontend logs only"
	@echo "  make shell-backend - Access backend container shell"
	@echo "  make db-shell      - Access PostgreSQL shell"
	@echo "  make redis-cli     - Access Redis CLI"
	@echo ""
	@echo "$(GREEN)Monitoring:$(RESET)"
	@echo "  make health    - Check health of all services"
	@echo "  make ps        - Show running containers"
	@echo ""

## Development Environment
dev: ## Start development environment with hot reload
	@echo "$(YELLOW)🚀 Starting SeatFlow Development Environment...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) down --remove-orphans 2>/dev/null || true
	@docker-compose -f $(DEV_COMPOSE) up --build -d
	@echo "$(GREEN)✅ Development environment started!$(RESET)"
	@echo ""
	@echo "$(BLUE)📍 Service URLs:$(RESET)"
	@echo "   Frontend (Hot Reload): http://localhost:5173"
	@echo "   Backend API:          http://localhost:8080/api"
	@echo "   Health Check:         http://localhost:8080/api/actuator/health"
	@echo ""
	@echo "$(BLUE)🔧 Development Features:$(RESET)"
	@echo "   ✨ Frontend hot reload enabled"
	@echo "   ✨ Backend continuous build enabled"
	@echo "   ✨ Source code changes auto-applied"
	@echo ""
	@echo "$(YELLOW)💡 Use 'make logs' to see real-time logs$(RESET)"

dev-build: ## Build development environment without starting
	@echo "$(YELLOW)🔨 Building development environment...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) build

## Production Environment
prod: ## Start production environment
	@echo "$(YELLOW)🚀 Starting SeatFlow Production Environment...$(RESET)"
	@docker-compose -f $(PROD_COMPOSE) down --remove-orphans 2>/dev/null || true
	@docker-compose -f $(PROD_COMPOSE) up --build -d
	@echo "$(GREEN)✅ Production environment started!$(RESET)"
	@echo ""
	@echo "$(BLUE)📍 Service URLs:$(RESET)"
	@echo "   Frontend: http://localhost:3000"
	@echo "   Backend API: http://localhost:8080/api"

prod-stop: ## Stop production environment
	@echo "$(YELLOW)🛑 Stopping production environment...$(RESET)"
	@docker-compose -f $(PROD_COMPOSE) down
	@echo "$(GREEN)✅ Production environment stopped!$(RESET)"

## Service Management
stop: ## Stop development environment
	@echo "$(YELLOW)🛑 Stopping development environment...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) down
	@echo "$(GREEN)✅ Development environment stopped!$(RESET)"

restart: ## Restart development environment
	@echo "$(YELLOW)🔄 Restarting development environment...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) restart
	@echo "$(GREEN)✅ Development environment restarted!$(RESET)"

restart-backend: ## Restart only backend service
	@echo "$(YELLOW)🔄 Restarting backend service...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) restart backend
	@echo "$(GREEN)✅ Backend service restarted!$(RESET)"

restart-frontend: ## Restart only frontend service
	@echo "$(YELLOW)🔄 Restarting frontend service...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) restart frontend
	@echo "$(GREEN)✅ Frontend service restarted!$(RESET)"

## Logging and Monitoring
logs: ## Show logs from all services
	@echo "$(BLUE)📋 Showing logs from all services (Ctrl+C to exit)...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) logs -f

logs-backend: ## Show backend logs only
	@echo "$(BLUE)📋 Showing backend logs (Ctrl+C to exit)...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) logs -f backend

logs-frontend: ## Show frontend logs only
	@echo "$(BLUE)📋 Showing frontend logs (Ctrl+C to exit)...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) logs -f frontend

logs-db: ## Show database logs only
	@echo "$(BLUE)📋 Showing database logs (Ctrl+C to exit)...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) logs -f postgres

status: ## Show status of all services
	@echo "$(BLUE)📊 Service Status:$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) ps

ps: ## Show running containers
	@echo "$(BLUE)🐳 Running Containers:$(RESET)"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

health: ## Check health of all services
	@echo "$(BLUE)🏥 Health Check:$(RESET)"
	@echo "Backend Health:"
	@curl -s http://localhost:8080/api/actuator/health | jq . || echo "❌ Backend not responding"
	@echo ""
	@echo "Redis Health:"
	@docker exec seatflow-redis-dev redis-cli ping || echo "❌ Redis not responding"
	@echo ""
	@echo "PostgreSQL Health:"
	@docker exec seatflow-postgres-dev pg_isready -U postgres || echo "❌ PostgreSQL not responding"

## Development Tools
test: ## Run all tests
	@echo "$(YELLOW)🧪 Running backend tests...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec backend ./gradlew test
	@echo "$(GREEN)✅ Tests completed!$(RESET)"

test-backend: ## Run backend tests only
	@echo "$(YELLOW)🧪 Running backend tests...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec backend ./gradlew test

build: ## Build all services without starting
	@echo "$(YELLOW)🔨 Building all services...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) build
	@echo "$(GREEN)✅ Build completed!$(RESET)"

## Shell Access
shell-backend: ## Access backend container shell
	@echo "$(BLUE)🐚 Accessing backend container shell...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec backend bash

shell-frontend: ## Access frontend container shell
	@echo "$(BLUE)🐚 Accessing frontend container shell...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec frontend sh

db-shell: ## Access PostgreSQL shell
	@echo "$(BLUE)🐚 Accessing PostgreSQL shell...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec postgres psql -U postgres -d seatflow

redis-cli: ## Access Redis CLI
	@echo "$(BLUE)🐚 Accessing Redis CLI...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) exec redis redis-cli

## Database Operations
db-reset: ## Reset database (WARNING: This will delete all data)
	@echo "$(RED)⚠️  WARNING: This will delete all database data!$(RESET)"
	@read -p "Are you sure? (y/N) " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo ""; \
		echo "$(YELLOW)🗑️  Resetting database...$(RESET)"; \
		docker-compose -f $(DEV_COMPOSE) stop postgres; \
		docker volume rm seatflow_postgres_data_dev 2>/dev/null || true; \
		docker-compose -f $(DEV_COMPOSE) up -d postgres; \
		echo "$(GREEN)✅ Database reset completed!$(RESET)"; \
	else \
		echo ""; \
		echo "$(GREEN)✅ Database reset cancelled.$(RESET)"; \
	fi

db-backup: ## Create database backup
	@echo "$(YELLOW)💾 Creating database backup...$(RESET)"
	@mkdir -p backups
	@docker-compose -f $(DEV_COMPOSE) exec postgres pg_dump -U postgres seatflow > backups/seatflow_backup_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✅ Database backup created in backups/ directory$(RESET)"

## Local Development
install: ## Install dependencies locally
	@echo "$(YELLOW)📦 Installing backend dependencies...$(RESET)"
	@cd backend && ./gradlew dependencies
	@echo "$(YELLOW)📦 Installing frontend dependencies...$(RESET)"
	@cd frontend && npm install
	@echo "$(GREEN)✅ Dependencies installed!$(RESET)"

local-backend: ## Run backend locally (requires local Java 17+)
	@echo "$(YELLOW)🚀 Starting backend locally...$(RESET)"
	@cd backend && ./gradlew bootRun

local-frontend: ## Run frontend locally (requires local Node.js 18+)
	@echo "$(YELLOW)🚀 Starting frontend locally...$(RESET)"
	@cd frontend && npm run dev

## Cleanup
clean: ## Clean up containers, volumes, and images
	@echo "$(YELLOW)🧹 Cleaning up Docker resources...$(RESET)"
	@docker-compose -f $(DEV_COMPOSE) down --volumes --remove-orphans
	@docker-compose -f $(PROD_COMPOSE) down --volumes --remove-orphans 2>/dev/null || true
	@docker system prune -f
	@echo "$(GREEN)✅ Cleanup completed!$(RESET)"

clean-all: ## Clean up everything including images
	@echo "$(RED)⚠️  WARNING: This will remove all Docker images and volumes!$(RESET)"
	@read -p "Are you sure? (y/N) " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo ""; \
		echo "$(YELLOW)🧹 Deep cleaning Docker resources...$(RESET)"; \
		docker-compose -f $(DEV_COMPOSE) down --volumes --remove-orphans --rmi all; \
		docker-compose -f $(PROD_COMPOSE) down --volumes --remove-orphans --rmi all 2>/dev/null || true; \
		docker system prune -af; \
		echo "$(GREEN)✅ Deep cleanup completed!$(RESET)"; \
	else \
		echo ""; \
		echo "$(GREEN)✅ Deep cleanup cancelled.$(RESET)"; \
	fi

## Quick Actions
quick-start: dev ## Quick start (alias for dev)

quick-stop: stop ## Quick stop (alias for stop)

quick-restart: restart ## Quick restart (alias for restart)

demo: ## Start demo environment with sample data
	@echo "$(YELLOW)🎭 Starting SeatFlow Demo...$(RESET)"
	@make dev
	@echo "$(GREEN)🎉 Demo ready! Visit http://localhost:5173$(RESET)"