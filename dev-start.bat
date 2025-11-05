@echo off
REM SeatFlow Development Environment Startup Script for Windows

echo 🎭 Starting SeatFlow Development Environment...

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not running. Please start Docker first.
    pause
    exit /b 1
)

REM Stop any existing containers
echo 🛑 Stopping existing containers...
docker-compose -f docker-compose.dev.yml down

REM Build and start services
echo 🚀 Building and starting services...
docker-compose -f docker-compose.dev.yml up --build -d

REM Wait a bit for services to start
echo ⏳ Waiting for services to start...
timeout /t 10 /nobreak >nul

echo.
echo 🎉 SeatFlow Development Environment is starting!
echo.
echo 📍 Service URLs:
echo    Frontend (Hot Reload): http://localhost:5173
echo    Backend API:          http://localhost:8080/api
echo    Health Check:         http://localhost:8080/api/actuator/health
echo.
echo 🔧 Development Features:
echo    ✨ Frontend hot reload enabled
echo    ✨ Backend continuous build enabled
echo    ✨ Source code changes auto-applied
echo.
echo 📁 Edit files in:
echo    Backend:  .\backend\src\
echo    Frontend: .\frontend\src\
echo.
echo 🛠️  Useful Commands:
echo    View logs:    docker-compose -f docker-compose.dev.yml logs -f
echo    Stop all:     docker-compose -f docker-compose.dev.yml down
echo    Restart:      dev-start.bat
echo.
echo Happy coding! 🚀
echo.
echo Press any key to view logs...
pause >nul
docker-compose -f docker-compose.dev.yml logs -f