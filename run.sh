#!/usr/bin/env bash
set -euo pipefail

check_env() {
  if [ ! -f .env ]; then
    echo "Error: .env file not found. Copy .env.example and fill in the required values."
    exit 1
  fi

  if ! grep -qE '^MONGO_URI=.+' .env; then
    echo "Error: MONGO_URI is not set in .env"
    exit 1
  fi
}

start() {
  check_env
  echo "Starting MedBridge services..."
  docker compose up --build -d
  echo ""
  echo "Services started. Checking status..."
  sleep 5
  docker compose ps
}

stop() {
  echo "Stopping MedBridge services..."
  docker compose down
}

restart() {
  stop
  start
}

logs() {
  local service="${1:-}"
  if [ -n "$service" ]; then
    docker compose logs -f "$service"
  else
    docker compose logs -f
  fi
}

status() {
  docker compose ps
}

rebuild() {
  local service="${1:-}"
  check_env
  if [ -n "$service" ]; then
    echo "Rebuilding $service..."
    docker compose up --build -d "$service"
  else
    echo "Rebuilding all services..."
    docker compose up --build -d
  fi
}

usage() {
  echo "Usage: ./run.sh [command] [service]"
  echo ""
  echo "Commands:"
  echo "  start            Build and start all services (default)"
  echo "  stop             Stop all services"
  echo "  restart          Stop then start all services"
  echo "  logs [service]   Tail logs for all or a specific service"
  echo "  status           Show running containers"
  echo "  rebuild [service] Rebuild and restart all or a specific service"
  echo ""
  echo "Services: keycloak, redis, eureka, zipkin, api-gateway, fhir-service, terminology-service"
}

case "${1:-start}" in
  start)   start ;;
  stop)    stop ;;
  restart) restart ;;
  logs)    logs "${2:-}" ;;
  status)  status ;;
  rebuild) rebuild "${2:-}" ;;
  help|--help|-h) usage ;;
  *)
    echo "Unknown command: ${1}"
    usage
    exit 1
    ;;
esac
