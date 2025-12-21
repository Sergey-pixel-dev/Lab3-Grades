.PHONY: help build up down restart logs clean test shell-db shell-a shell-b health

.DEFAULT_GOAL := help

help:
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $$1, $$2}'

build:
	docker-compose build

up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

logs-a:
	docker-compose logs -f service-a

logs-b:
	docker-compose logs -f service-b

logs-db:
	docker-compose logs -f postgres

clean:
	docker-compose down -v --rmi all

clean-volumes:
	docker-compose down -v

shell-db:
	docker-compose exec postgres psql -U postgres -d grades_db

shell-a:
	docker-compose exec service-a sh

shell-b:
	docker-compose exec service-b sh

health:
	@curl -s http://localhost:8080/api/client/health
	@echo ""
	@curl -s http://localhost:8081/api/grades/health
	@echo ""
	@curl -s http://localhost:8080/api/client/health/service-b

test:
	@curl -s http://localhost:8080/api/client/grades/average/Mathematics

test-all-courses:
	@for course in Mathematics Physics "Computer Science" Chemistry Biology English History Philosophy Economics Statistics; do \
		echo "Testing: $$course"; \
		curl -s "http://localhost:8080/api/client/grades/average/$$course"; \
		echo ""; \
	done

ps:
	docker-compose ps

rebuild: down build up

dev-up:
	docker-compose up
