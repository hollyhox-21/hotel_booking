SHELL := /bin/bash
# The service builds share common/build; parallel goals must not write it concurrently.
.NOTPARALLEL:
SERVICES := eureka_server hotel_management_service booking_service API_gateway
RUNTIME := $(CURDIR)/.runtime
GRADLE := ./booking_service/gradlew
EUREKA_PORT ?= 8761
HOTEL_PORT ?= 8082
BOOKING_PORT ?= 8081
GATEWAY_PORT ?= 8080
FRONTEND_PORT ?= 5173
EUREKA_URL ?= http://127.0.0.1:$(EUREKA_PORT)/eureka/
GATEWAY_URL ?= http://localhost:$(GATEWAY_PORT)
HOTEL_TEST_URL ?= http://localhost:$(HOTEL_PORT)
export GATEWAY_URL HOTEL_TEST_URL

# A PID belongs to this launcher only when its command and birth signature still match.
define PROCESS_HELPERS
signature() { ps -p "$$1" -o lstart= -o args= 2>/dev/null | sed 's/^[[:space:]]*//'; }; \
is_running() { \
name="$$1"; \
[ -f "$(RUNTIME)/$$name.pid" ] && [ -f "$(RUNTIME)/$$name.signature" ] || return 1; \
pid=$$(cat "$(RUNTIME)/$$name.pid"); \
case "$$pid" in ''|*[!0-9]*) return 1 ;; esac; \
actual=$$(signature "$$pid") || return 1; \
[ -n "$$actual" ] && [ "$$actual" = "$$(cat "$(RUNTIME)/$$name.signature")" ] || return 1; \
case "$$actual" in *"$(CURDIR)/$$name/build/libs/"*|*"$(CURDIR)/$$name/node_modules/vite/bin/vite.js"*) ;; *) return 1 ;; esac; \
state=$$(ps -p "$$pid" -o stat= 2>/dev/null) || return 1; \
case "$$state" in *Z*) return 1 ;; esac; \
}; \
stop_process() { \
target="$$1"; \
if is_running "$$target"; then \
kill "$$pid"; \
attempt=0; \
while is_running "$$target" && [ "$$attempt" -lt 50 ]; do \
sleep 0.2; \
attempt=$$((attempt + 1)); \
done; \
if is_running "$$target"; then kill -KILL "$$pid"; fi; \
echo "Stopped $$target"; \
fi; \
rm -f "$(RUNTIME)/$$target.pid" "$(RUNTIME)/$$target.signature"; \
};
endef

.PHONY: help build backend frontend test format check start up stop restart status logs e2e demo
help:
	@set -eu; \
	echo 'make build    — build backend/frontend and run checks'; \
	echo 'make start    — launch built services and React in background'; \
	echo 'make up       — build and start everything'; \
	echo 'make stop / restart / status / logs'; \
	echo 'make test     — Java unit/integration tests and frontend tests'; \
	echo 'make e2e      — Java/JUnit scenarios against running application'; \
	echo 'make demo     — repeatable demo data through Java HTTP client'; \
	echo 'make format / check';

build: backend frontend
backend:
	@set -eu; \
	for service in $(SERVICES); do \
	(cd "$$service" && ./gradlew build spotlessCheck --no-daemon); \
	done; \
	$(GRADLE) -p integration_tests build spotlessCheck --no-daemon;
frontend:
	@set -eu; \
	cd frontend; \
	npm ci; \
	npm run format:check; \
	npm test; \
	npm run build;

test:
	@set -eu; \
	for service in $(SERVICES); do \
	(cd "$$service" && ./gradlew test --no-daemon); \
	done; \
	$(GRADLE) -p integration_tests test --no-daemon; \
	cd frontend && npm test;
format:
	@set -eu; \
	(cd frontend && npm run format); \
	for service in $(SERVICES); do \
	(cd "$$service" && ./gradlew spotlessApply --no-daemon); \
	done; \
	$(GRADLE) -p integration_tests spotlessApply --no-daemon;
check:
	@set -eu; \
	(cd frontend && npm run format:check); \
	for service in $(SERVICES); do \
	(cd "$$service" && ./gradlew spotlessCheck --no-daemon); \
	done; \
	$(GRADLE) -p integration_tests spotlessCheck --no-daemon;

start:
	@set -eu; set -m; \
	$(PROCESS_HELPERS) \
	mkdir -p "$(RUNTIME)/logs"; \
	command -v curl >/dev/null; \
	command -v java >/dev/null; \
	command -v node >/dev/null; \
	for service in $(SERVICES); do \
	test -f "$(CURDIR)/$$service/build/libs/$$service-1.0.0.jar" || { echo "Missing JAR for $$service; run make build" >&2; exit 1; }; \
	done; \
	test -f "$(CURDIR)/frontend/node_modules/vite/bin/vite.js" || { echo 'Run make build first' >&2; exit 1; }; \
	created=''; \
	success=0; \
	trap 'if [ "$$success" -ne 1 ]; then for item in $$created; do stop_process "$$item"; done; fi' 0; \
	trap 'exit 130' INT; \
	trap 'exit 143' TERM; \
	for entry in eureka_server:$(EUREKA_PORT) hotel_management_service:$(HOTEL_PORT) booking_service:$(BOOKING_PORT) API_gateway:$(GATEWAY_PORT) frontend:$(FRONTEND_PORT); do \
	service=$${entry%:*}; \
	port=$${entry#*:}; \
	if is_running "$$service"; then echo "Already running $$service"; continue; fi; \
	if curl -s -o /dev/null --max-time 2 "http://127.0.0.1:$$port/"; then \
	echo "Port $$port is occupied outside this launcher" >&2; \
	exit 1; \
	fi; \
	if [ "$$service" = frontend ]; then \
	(cd "$(CURDIR)/frontend" && exec nohup node "$(CURDIR)/frontend/node_modules/vite/bin/vite.js" --host 127.0.0.1 --port "$(FRONTEND_PORT)" --strictPort) >"$(RUNTIME)/logs/$$service.log" 2>&1 < /dev/null & \
	else \
	(cd "$(CURDIR)/$$service" && exec nohup java -Xms64m -Xmx320m -jar "$(CURDIR)/$$service/build/libs/$$service-1.0.0.jar" --server.port="$$port" --eureka.client.service-url.defaultZone="$(EUREKA_URL)" --eureka.instance.ip-address=127.0.0.1) >"$(RUNTIME)/logs/$$service.log" 2>&1 < /dev/null & \
	fi; \
	pid=$$!; \
	printf '%s\n' "$$pid" >"$(RUNTIME)/$$service.pid"; \
	sleep 0.2; \
	signature "$$pid" >"$(RUNTIME)/$$service.signature"; \
	created="$$service $$created"; \
	endpoint=actuator/health; \
	if [ "$$service" = frontend ]; then endpoint=''; fi; \
	attempt=0; \
	until curl -fsS --max-time 2 "http://127.0.0.1:$$port/$$endpoint" >/dev/null 2>&1; do \
	if ! is_running "$$service" || [ "$$attempt" -ge 100 ]; then \
	echo "Startup failed: $$service; see $(RUNTIME)/logs/$$service.log" >&2; \
	exit 1; \
	fi; \
	attempt=$$((attempt + 1)); \
	sleep 1; \
	done; \
	echo "Ready $$service: http://localhost:$$port"; \
	done; \
	attempt=0; \
	until curl -fsS --max-time 2 "$(EUREKA_URL)apps/BOOKING-SERVICE" >/dev/null 2>&1 && curl -fsS --max-time 2 "$(EUREKA_URL)apps/HOTEL-SERVICE" >/dev/null 2>&1 && [ "$$(curl -s --max-time 3 -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' -d '{}' "$(GATEWAY_URL)/api/auth/login")" = 400 ]; do \
	if [ "$$attempt" -ge 60 ]; then echo 'Discovery readiness timeout; see $(RUNTIME)/logs' >&2; exit 1; fi; \
	attempt=$$((attempt + 1)); \
	sleep 1; \
	done; \
	success=1; \
	echo 'Application: http://localhost:$(FRONTEND_PORT) — Gateway: $(GATEWAY_URL)';

up: build
	@set -eu; \
	$(MAKE) start;
stop:
	@set -eu; \
	$(PROCESS_HELPERS) \
	for service in frontend API_gateway booking_service hotel_management_service eureka_server; do stop_process "$$service"; done;
restart:
	@set -eu; \
	$(MAKE) stop; \
	$(MAKE) start;
status:
	@set -eu; \
	$(PROCESS_HELPERS) \
	for service in $(SERVICES) frontend; do \
	if is_running "$$service"; then printf '%-27s running pid=%s\n' "$$service" "$$pid"; else printf '%-27s stopped\n' "$$service"; fi; \
	done;
logs:
	@set -eu; \
	tail -n 50 "$(RUNTIME)/logs/"*.log;
e2e:
	@set -eu; \
	$(GRADLE) -p integration_tests e2e --no-daemon;
demo:
	@set -eu; \
	$(GRADLE) -p integration_tests run --no-daemon;
