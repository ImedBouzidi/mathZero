#!/usr/bin/env bash

# Interactive service manager for Mathematics Zero to Hero.
set -u
set -o pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${ROOT_DIR}/infra/compose/docker-compose.yml"
RUNTIME_DIR="${ROOT_DIR}/.manage-services"
LOG_DIR="${RUNTIME_DIR}/logs"
PID_DIR="${RUNTIME_DIR}/pids"

C_RESET=$'\033[0m'
C_BOLD=$'\033[1m'
C_CYAN=$'\033[36m'
C_GREEN=$'\033[32m'
C_RED=$'\033[31m'
C_YELLOW=$'\033[33m'
C_DIM=$'\033[2m'

mkdir -p "$LOG_DIR" "$PID_DIR"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

eureka_port="${EUREKA_PORT:-8761}"
gateway_port="${GATEWAY_PORT:-8090}"
identity_port="${IDENTITY_PORT:-8091}"
curriculum_port="${CURRICULUM_PORT:-8082}"
resource_port="${RESOURCE_PORT:-8089}"
postgres_port="${POSTGRES_PORT:-5432}"
rabbitmq_port="${RABBITMQ_PORT:-5672}"
rabbitmq_management_port="${RABBITMQ_MANAGEMENT_PORT:-15672}"
angular_port="${ANGULAR_PORT:-4200}"

print_header() {
  clear 2>/dev/null || true
  printf '\n%s%s  MATHEMATICS ZERO TO HERO%s\n' "$C_BOLD" "$C_CYAN" "$C_RESET"
  printf '%s  Local Services Manager%s\n\n' "$C_DIM" "$C_RESET"
}

ok() { printf '%s✓%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
warn() { printf '%s!%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
fail() { printf '%s✗%s %s\n' "$C_RED" "$C_RESET" "$*"; }
info() { printf '%s→%s %s\n' "$C_CYAN" "$C_RESET" "$*"; }

resolve_full_jdk() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" && -x "${JAVA_HOME}/bin/javac" ]]; then
    case ":${PATH}:" in
      *":${JAVA_HOME}/bin:"*) ;;
      *) PATH="${JAVA_HOME}/bin:${PATH}"; export PATH ;;
    esac
    return 0
  fi

  local javac_command
  javac_command="$(command -v javac || true)"
  if [[ -z "$javac_command" || ! -x "$javac_command" ]]; then
    return 1
  fi

  local javac_path
  javac_path="$(readlink -f "$javac_command" 2>/dev/null || true)"
  [[ "$javac_path" == */bin/javac ]] || return 1

  local detected_java_home="${javac_path%/bin/javac}"
  [[ -x "${detected_java_home}/bin/java" ]] || return 1

  local previous_java_home="${JAVA_HOME:-<unset>}"
  JAVA_HOME="$detected_java_home"
  export JAVA_HOME
  case ":${PATH}:" in
    *":${JAVA_HOME}/bin:"*) ;;
    *) PATH="${JAVA_HOME}/bin:${PATH}"; export PATH ;;
  esac
  printf 'JAVA_HOME %s is not a full JDK; using detected JDK: %s\n' \
    "$previous_java_home" "$JAVA_HOME" >&2
}

resolve_full_jdk || true

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

pid_file() {
  printf '%s/%s.pid' "$PID_DIR" "$1"
}

log_file() {
  printf '%s/%s.log' "$LOG_DIR" "$1"
}

is_running() {
  local name="$1"
  local pid_file_path
  pid_file_path="$(pid_file "$name")"
  [[ -f "$pid_file_path" ]] || return 1

  local pid
  pid="$(cat "$pid_file_path" 2>/dev/null || true)"
  [[ "$pid" =~ ^[0-9]+$ ]] || return 1
  kill -0 "$pid" 2>/dev/null
}

is_http_up() {
  curl -fsS --max-time 2 "$1" >/dev/null 2>&1
}

port_open() {
  local host="$1"
  local port="$2"
  (exec 3<>"/dev/tcp/${host}/${port}") 2>/dev/null
}

show_recent_log() {
  local name="$1"
  local path
  path="$(log_file "$name")"
  if [[ -f "$path" ]]; then
    printf '\n%sLast 20 lines of %s:%s\n' "$C_DIM" "$name" "$C_RESET"
    tail -n 20 "$path"
    printf '\n'
  fi
}

start_process() {
  local name="$1"
  local cwd="$2"
  shift 2

  if is_running "$name"; then
    warn "${name} is already running"
    return 0
  fi

  info "Starting ${name}..."
  nohup setsid bash -c 'cd "$1" && shift && exec "$@"' _ "$cwd" "$@" \
    > "$(log_file "$name")" 2>&1 &
  printf '%s\n' "$!" > "$(pid_file "$name")"

  sleep 1
  if is_running "$name"; then
    ok "${name} started"
  else
    rm -f "$(pid_file "$name")"
    fail "${name} failed to start; see $(log_file "$name")"
    show_recent_log "$name"
  fi
}

stop_process() {
  local name="$1"
  local pid_file_path
  pid_file_path="$(pid_file "$name")"

  if [[ ! -f "$pid_file_path" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "$pid_file_path" 2>/dev/null || true)"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    info "Stopping ${name}..."
    kill -TERM -- "-${pid}" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true

    for _ in {1..20}; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.25
    done
    if kill -0 "$pid" 2>/dev/null; then
      kill -KILL -- "-${pid}" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
    fi
  fi

  rm -f "$pid_file_path"
  ok "${name} stopped"
}

start_infra() {
  if ! command -v docker >/dev/null 2>&1; then
    fail "Docker is not installed or not in PATH"
    return 1
  fi
  if ! docker info >/dev/null 2>&1; then
    fail "Docker daemon is not running"
    info "Start Docker, or use host PostgreSQL/RabbitMQ already listening on the configured ports"
    return 1
  fi
  info "Starting PostgreSQL and RabbitMQ..."
  if compose up -d postgres rabbitmq; then
    ok "Infrastructure containers started"
  else
    fail "Could not start infrastructure containers"
  fi
}

stop_infra() {
  info "Stopping infrastructure containers..."
  compose down
  ok "Infrastructure containers stopped"
}

start_infra_service() {
  local service="$1"
  if ! command -v docker >/dev/null 2>&1; then
    fail "Docker is not installed or not in PATH"
    return 1
  fi
  if ! docker info >/dev/null 2>&1; then
    fail "Docker daemon is not running"
    info "Start Docker, or use host PostgreSQL/RabbitMQ already listening on the configured ports"
    return 1
  fi
  info "Starting ${service}..."
  if compose up -d "$service"; then
    ok "${service} started"
  else
    fail "Could not start ${service}"
    return 1
  fi
}

stop_infra_service() {
  local service="$1"
  info "Stopping ${service}..."
  if compose stop "$service"; then
    ok "${service} stopped"
  else
    fail "Could not stop ${service}"
    return 1
  fi
}

start_eureka() {
  check_maven_java || return 1
  start_process eureka-server "${ROOT_DIR}/backend" \
    mvn -pl eureka-server spring-boot:run
  wait_http "http://localhost:${eureka_port}/actuator/health" "Eureka" eureka-server
}

start_gateway() {
  check_maven_java || return 1
  start_process gateway-service "${ROOT_DIR}/backend" \
    mvn -pl gateway-service spring-boot:run
  wait_http "http://localhost:${gateway_port}/actuator/health" "Gateway" gateway-service
}

start_identity() {
  if ! port_open localhost "$postgres_port"; then
    warn "PostgreSQL is not reachable on localhost:${postgres_port}"
    info "Start it first with: ./manage-services.sh start postgres"
  fi
  if [[ -f "${ROOT_DIR}/backend/identity-service/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "${ROOT_DIR}/backend/identity-service/.env"
    set +a
  fi
  check_maven_java || return 1
  start_process identity-service "${ROOT_DIR}/backend" \
    mvn -pl identity-service spring-boot:run
  wait_http "http://localhost:${identity_port}/actuator/health" "Identity" identity-service
}

start_curriculum() {
  check_maven_java || return 1
  if ! port_open localhost "$postgres_port"; then
    warn "PostgreSQL is not reachable on localhost:${postgres_port}"
    info "Start it first with: ./manage-services.sh start postgres"
  fi
  start_process curriculum-service "${ROOT_DIR}/backend" \
    mvn -pl curriculum-service spring-boot:run
  wait_http "http://localhost:${curriculum_port}/actuator/health" "Curriculum" curriculum-service
}

start_resource() {
  check_maven_java || return 1
  if ! port_open localhost "$postgres_port"; then
    warn "PostgreSQL is not reachable on localhost:${postgres_port}"
    info "Start it first with: ./manage-services.sh start postgres"
  fi
  start_process resource-service "${ROOT_DIR}/backend" \
    mvn -pl resource-service spring-boot:run
  wait_http "http://localhost:${resource_port}/actuator/health" "Resource" resource-service
}

start_frontend() {
  if [[ ! -d "${ROOT_DIR}/frontend/mathzero-angular/node_modules" ]]; then
    info "Installing Angular dependencies..."
    (cd "${ROOT_DIR}/frontend/mathzero-angular" && npm ci) \
      >> "$(log_file frontend)" 2>&1 || {
        fail "npm ci failed; see $(log_file frontend)"
        return 1
      }
  fi
  start_process frontend "${ROOT_DIR}/frontend/mathzero-angular" \
    npm start -- --host 0.0.0.0 --port "$angular_port"
  wait_http "http://localhost:${angular_port}" "Angular" frontend
}

check_maven_java() {
  if ! resolve_full_jdk; then
    if [[ -z "${JAVA_HOME:-}" ]]; then
      fail "JAVA_HOME is not set and no full JDK was detected; JDK 17 or newer is required"
    else
      fail "JAVA_HOME does not contain a full JDK: ${JAVA_HOME}"
      info "Point JAVA_HOME at a full JDK 17+ installation, not a JRE"
    fi
    return 1
  fi

  if ! command -v mvn >/dev/null 2>&1; then
    fail "Maven is not installed or not in PATH"
    return 1
  fi

  local mvn_output
  if ! mvn_output="$(mvn -version 2>&1)"; then
    fail "Could not determine the Java runtime used by Maven"
    return 1
  fi

  local version
  version="$(printf '%s\n' "$mvn_output" | sed -n 's/^Java version: //p' | head -n 1)"
  if [[ -z "$version" ]]; then
    fail "Could not determine the Java runtime used by Maven; check 'mvn -version'"
    return 1
  fi

  local major
  major="$(java_major_version "$version")"
  if [[ ! "$major" =~ ^[0-9]+$ ]] || (( major < 17 )); then
    fail "Maven is using Java ${version}; this project requires JDK 17 or newer"
    info "Set JAVA_HOME to a JDK 17+ installation and ensure its bin directory is first in PATH"
    info "Confirm Maven's runtime with: mvn -version"
    return 1
  fi

  local javac_path="${JAVA_HOME}/bin/javac"
  local javac_output
  if ! javac_output="$("$javac_path" -version 2>&1)"; then
    fail "Could not determine the javac version"
    return 1
  fi
  local javac_version="${javac_output##* }"
  local javac_major
  javac_major="$(java_major_version "$javac_version")"
  if [[ ! "$javac_major" =~ ^[0-9]+$ ]] || (( javac_major < 17 )); then
    fail "JAVA_HOME provides javac ${javac_version}; this project requires JDK 17 or newer"
    info "Set JAVA_HOME to a full JDK 17+ installation"
    return 1
  fi
  return 0
}

java_major_version() {
  local version="$1"
  local major="${version%%.*}"
  if [[ "$major" == "1" ]]; then
    local remainder="${version#*.}"
    major="${remainder%%.*}"
  fi
  printf '%s' "$major"
}

wait_http() {
  local url="$1"
  local label="$2"
  local process_name="$3"
  printf '%s  Waiting for %s%s' "$C_DIM" "$label" "$C_RESET"
  for _ in {1..80}; do
    if is_http_up "$url"; then
      printf '\r'
      ok "${label} is ready"
      return 0
    fi
    if ! is_running "$process_name"; then
      printf '\r'
      rm -f "$(pid_file "$process_name")"
      fail "${label} exited before becoming ready; see $(log_file "$process_name")"
      show_recent_log "$process_name"
      return 1
    fi
    printf '.'
    sleep 0.5
  done
  printf '\r'
  warn "${label} did not respond within 40 seconds"
  return 1
}

status_process() {
  local name="$1"
  local port="$2"
  local url="$3"
  local label="$4"
  if is_running "$name" && (is_http_up "$url"); then
    printf '  %s●%s %-18s %s%-9s%s http://localhost:%s\n' \
      "$C_GREEN" "$C_RESET" "$label" "$C_GREEN" "RUNNING" "$C_RESET" "$port"
  elif is_running "$name"; then
    printf '  %s●%s %-18s %s%-9s%s http://localhost:%s\n' \
      "$C_YELLOW" "$C_RESET" "$label" "$C_YELLOW" "STARTING" "$C_RESET" "$port"
  else
    printf '  %s○%s %-18s %s%-9s%s http://localhost:%s\n' \
      "$C_DIM" "$C_RESET" "$label" "$C_DIM" "STOPPED" "$C_RESET" "$port"
  fi
}

status_infra() {
  printf '\n%sInfrastructure%s\n' "$C_BOLD" "$C_RESET"
  local row state port_label
  for row in postgres rabbitmq; do
    case "$row" in
      postgres) port_label="localhost:${postgres_port}" ;;
      rabbitmq) port_label="localhost:${rabbitmq_port} (AMQP), localhost:${rabbitmq_management_port} (management)" ;;
    esac
    state="$(infra_state "$row")"
    printf '  %s %-18s %s\n' "$(state_colored "$state")" "$row" "$port_label"
  done
}

show_status() {
  print_header
  status_infra
  printf '\n%sApplications%s\n' "$C_BOLD" "$C_RESET"
  status_process eureka-server "$eureka_port" "http://localhost:${eureka_port}/actuator/health" "Eureka"
  status_process identity-service "$identity_port" "http://localhost:${identity_port}/actuator/health" "Identity"
  status_process curriculum-service "$curriculum_port" "http://localhost:${curriculum_port}/actuator/health" "Curriculum"
  status_process resource-service "$resource_port" "http://localhost:${resource_port}/actuator/health" "Resource"
  status_process gateway-service "$gateway_port" "http://localhost:${gateway_port}/actuator/health" "Gateway"
  status_process frontend "$angular_port" "http://localhost:${angular_port}" "Angular"
  printf '\n'
}

service_label() {
  case "$1" in
    postgres) printf 'PostgreSQL' ;;
    rabbitmq) printf 'RabbitMQ' ;;
    eureka|eureka-server) printf 'Eureka' ;;
    identity|identity-service) printf 'Identity' ;;
    curriculum|curriculum-service) printf 'Curriculum' ;;
    resource|resource-service) printf 'Resource' ;;
    gateway|gateway-service) printf 'Gateway' ;;
    frontend|angular) printf 'Angular' ;;
    *) printf '%s' "$1" ;;
  esac
}

process_name_for() {
  case "$1" in
    eureka) printf 'eureka-server' ;;
    identity) printf 'identity-service' ;;
    curriculum) printf 'curriculum-service' ;;
    resource) printf 'resource-service' ;;
    gateway) printf 'gateway-service' ;;
    frontend) printf 'frontend' ;;
    *) printf '%s' "$1" ;;
  esac
}

service_url() {
  case "$1" in
    rabbitmq) printf 'http://localhost:%s' "$rabbitmq_management_port" ;;
    eureka) printf 'http://localhost:%s' "$eureka_port" ;;
    identity) printf 'http://localhost:%s' "$identity_port" ;;
    curriculum) printf 'http://localhost:%s' "$curriculum_port" ;;
    resource) printf 'http://localhost:%s' "$resource_port" ;;
    gateway) printf 'http://localhost:%s' "$gateway_port" ;;
    frontend) printf 'http://localhost:%s' "$angular_port" ;;
    *) printf '' ;;
  esac
}

health_url() {
  case "$1" in
    eureka) printf 'http://localhost:%s/actuator/health' "$eureka_port" ;;
    identity) printf 'http://localhost:%s/actuator/health' "$identity_port" ;;
    curriculum) printf 'http://localhost:%s/actuator/health' "$curriculum_port" ;;
    resource) printf 'http://localhost:%s/actuator/health' "$resource_port" ;;
    gateway) printf 'http://localhost:%s/actuator/health' "$gateway_port" ;;
    frontend) printf 'http://localhost:%s' "$angular_port" ;;
    *) printf '' ;;
  esac
}

infra_state() {
  local svc="$1"
  local status

  if command -v docker >/dev/null 2>&1; then
    status="$(docker inspect --format '{{.State.Status}}' "mathzero-${svc}" 2>/dev/null || true)"
    if [[ "$status" == "running" ]]; then
      printf 'RUNNING'
      return 0
    fi
  fi

  local port
  case "$svc" in
    postgres) port="$postgres_port" ;;
    rabbitmq) port="$rabbitmq_port" ;;
  esac
  if port_open localhost "$port"; then
    printf 'EXTERNAL'
  elif ! command -v docker >/dev/null 2>&1; then
    printf 'DOCKER N/A'
  else
    printf 'STOPPED'
  fi
}

infra_health() {
  local svc="$1"
  local status

  status="$(docker inspect \
    --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
    "mathzero-${svc}" 2>/dev/null || true)"
  if [[ -n "$status" ]]; then
    printf '%s' "$status"
    return 0
  fi

  local port
  case "$svc" in
    postgres) port="$postgres_port" ;;
    rabbitmq) port="$rabbitmq_port" ;;
  esac
  if port_open localhost "$port"; then
    printf 'reachable (host)'
  fi
}

process_state() {
  local name="$1"
  local url="$2"
  if is_running "$name" && is_http_up "$url"; then
    printf 'RUNNING'
  elif is_running "$name"; then
    printf 'STARTING'
  else
    printf 'STOPPED'
  fi
}

state_colored() {
  case "$1" in
    RUNNING) printf '%s● RUNNING%s' "$C_GREEN" "$C_RESET" ;;
    EXTERNAL) printf '%s● EXTERNAL%s' "$C_CYAN" "$C_RESET" ;;
    STARTING) printf '%s● STARTING%s' "$C_YELLOW" "$C_RESET" ;;
    "DOCKER N/A") printf '%s○ DOCKER N/A%s' "$C_DIM" "$C_RESET" ;;
    *) printf '%s○ STOPPED%s' "$C_DIM" "$C_RESET" ;;
  esac
}

service_state() {
  case "$1" in
    postgres|rabbitmq) infra_state "$1" ;;
    *) process_state "$(process_name_for "$1")" "$(health_url "$1")" ;;
  esac
}

dashboard() {
  local svc name state url
  printf '\n%sService status%s\n' "$C_BOLD" "$C_RESET"
  for svc in postgres rabbitmq eureka identity curriculum resource gateway frontend; do
    name="$(service_label "$svc")"
    state="$(service_state "$svc")"
    url="$(service_url "$svc")"
    case "$svc" in
      postgres) url="localhost:${postgres_port}" ;;
      rabbitmq) url="localhost:${rabbitmq_port} (AMQP) / ${rabbitmq_management_port} (mgmt)" ;;
    esac
    printf '  %-12s %s  %s%s%s\n' \
      "$name" "$(state_colored "$state")" "$C_DIM" "$url" "$C_RESET"
  done
}

pause() {
  printf '\n%sPress Enter to return...%s' "$C_DIM" "$C_RESET"
  read -r _ || true
}

confirm() {
  local reply
  printf '%s [y/N] ' "$1"
  read -r reply || return 1
  [[ "$reply" =~ ^[Yy]$ ]]
}

open_url() {
  local url="$1"
  if [[ -z "$url" ]]; then
    warn "No browser URL for this service"
    return 1
  fi
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 &
    ok "Opened ${url}"
  else
    info "${url}"
  fi
}

health_check() {
  print_header
  printf '\n%sHealth checks%s\n\n' "$C_BOLD" "$C_RESET"

  local svc name url state
  for svc in postgres rabbitmq; do
    name="$(service_label "$svc")"
    state="$(infra_health "$svc")"
    if [[ "$state" == "healthy" || "$state" == "running" || "$state" == "reachable (host)" ]]; then
      ok "${name} (${state})"
    else
      fail "${name} (${state:-not running})"
    fi
  done

  for svc in eureka identity curriculum resource gateway frontend; do
    name="$(service_label "$svc")"
    url="$(health_url "$svc")"
    if is_http_up "$url"; then
      ok "${name}  ${url}"
    else
      fail "${name}  ${url}"
    fi
  done
}

SELECTED_SERVICE=""
select_service() {
  local title="$1"
  local choice svc i=1
  print_header
  printf '\n%s%s%s\n\n' "$C_BOLD" "$title" "$C_RESET"
  for svc in postgres rabbitmq eureka identity curriculum resource gateway frontend; do
    printf '  %s%d)%s %-12s %s\n' \
      "$C_CYAN" "$i" "$C_RESET" "$(service_label "$svc")" \
      "$(state_colored "$(service_state "$svc")")"
    i=$((i + 1))
  done
  printf '  %s0)%s Back\n\n' "$C_CYAN" "$C_RESET"
  printf 'Choose a service: '
  read -r choice || return 1

  case "$choice" in
    1) SELECTED_SERVICE=postgres ;;
    2) SELECTED_SERVICE=rabbitmq ;;
    3) SELECTED_SERVICE=eureka ;;
    4) SELECTED_SERVICE=identity ;;
    5) SELECTED_SERVICE=curriculum ;;
    6) SELECTED_SERVICE=resource ;;
    7) SELECTED_SERVICE=gateway ;;
    8) SELECTED_SERVICE=frontend ;;
    0|b|B|back) return 1 ;;
    *)
      warn "Invalid selection"
      return 1
      ;;
  esac
  return 0
}

start_all() {
  print_header
  start_infra
  start_eureka
  start_identity
  start_curriculum
  start_resource
  start_gateway
  start_frontend
}

stop_all() {
  print_header
  stop_process frontend
  stop_process gateway-service
  stop_process resource-service
  stop_process curriculum-service
  stop_process identity-service
  stop_process eureka-server
  stop_infra
}

restart_all() {
  stop_all
  start_all
}

show_logs() {
  local name="${1:-all}"
  case "$name" in
    infra)
      compose logs --tail=200 -f postgres rabbitmq
      ;;
    postgres|rabbitmq)
      compose logs --tail=200 -f "$name"
      ;;
    eureka|eureka-server)
      follow_log eureka-server
      ;;
    curriculum|curriculum-service)
      follow_log curriculum-service
      ;;
    resource|resource-service)
      follow_log resource-service
      ;;
    gateway|gateway-service)
      follow_log gateway-service
      ;;
    identity|identity-service)
      follow_log identity-service
      ;;
    frontend|angular)
      follow_log frontend
      ;;
    all)
      local logs=()
      for service in eureka-server identity-service curriculum-service resource-service gateway-service frontend; do
        local path
        path="$(log_file "$service")"
        [[ -f "$path" ]] && logs+=("$path")
      done
      if [[ ${#logs[@]} -eq 0 ]]; then
        warn "No application logs yet. Start a service first."
        return 1
      fi
      tail -n 200 -F "${logs[@]}"
      ;;
    *)
      fail "Unknown service: ${name}"
      return 1
      ;;
  esac
}

follow_log() {
  local service="$1"
  local path
  path="$(log_file "$service")"
  if [[ ! -f "$path" ]]; then
    warn "No logs found for ${service}. Start it first."
    return 1
  fi

  info "Following ${service} logs"
  if command -v less >/dev/null 2>&1 && [[ -t 1 ]]; then
    info "Ctrl+C stops following; q returns to the menu"
    less +F "$path"
  else
    info "Ctrl+C stops following"
    tail -n 200 -F "$path"
  fi
}

view_logs() {
  local name="${1:-all}"
  case "$name" in
    infra)
      compose logs --tail=200 postgres rabbitmq
      ;;
    postgres|rabbitmq)
      compose logs --tail=200 "$name"
      ;;
    eureka|eureka-server)
      show_log_snapshot eureka-server
      ;;
    curriculum|curriculum-service)
      show_log_snapshot curriculum-service
      ;;
    resource|resource-service)
      show_log_snapshot resource-service
      ;;
    gateway|gateway-service)
      show_log_snapshot gateway-service
      ;;
    identity|identity-service)
      show_log_snapshot identity-service
      ;;
    frontend|angular)
      show_log_snapshot frontend
      ;;
    all)
      local found=0
      for service in eureka-server identity-service curriculum-service resource-service gateway-service frontend; do
        if [[ -f "$(log_file "$service")" ]]; then
          show_log_snapshot "$service"
          found=1
        fi
      done
      if [[ "$found" -eq 0 ]]; then
        warn "No application logs yet. Start a service first."
      fi
      compose logs --tail=100 postgres rabbitmq
      ;;
    *)
      fail "Unknown service: ${name}"
      return 1
      ;;
  esac
}

show_log_snapshot() {
  local service="$1"
  local path
  path="$(log_file "$service")"
  printf '\n%s── %s (last 200 lines) ──%s\n' "$C_BOLD" "$service" "$C_RESET"
  if [[ -f "$path" ]]; then
    tail -n 200 "$path"
  else
    warn "No logs found for ${service}. Start it first."
  fi
}

logs_menu() {
  local choice
  while true; do
    print_header
    printf '\n%sLogs%s\n\n' "$C_BOLD" "$C_RESET"
    printf '  %s1)%s All services (recent)\n' "$C_CYAN" "$C_RESET"
    printf '  %s2)%s Eureka\n' "$C_CYAN" "$C_RESET"
    printf '  %s3)%s Identity\n' "$C_CYAN" "$C_RESET"
    printf '  %s4)%s Gateway\n' "$C_CYAN" "$C_RESET"
    printf '  %s5)%s Curriculum\n' "$C_CYAN" "$C_RESET"
    printf '  %s6)%s Resource\n' "$C_CYAN" "$C_RESET"
    printf '  %s7)%s Angular\n' "$C_CYAN" "$C_RESET"
    printf '  %s8)%s Infrastructure (PostgreSQL + RabbitMQ)\n' "$C_CYAN" "$C_RESET"
    printf '  %s9)%s Follow a service log\n' "$C_CYAN" "$C_RESET"
    printf '  %s0)%s Back\n\n' "$C_CYAN" "$C_RESET"
    printf 'Choose an option: '
    read -r choice || return 0

    case "$choice" in
      1) view_logs all; pause ;;
      2) view_logs eureka; pause ;;
      3) view_logs identity; pause ;;
      4) view_logs gateway; pause ;;
      5) view_logs curriculum; pause ;;
      6) view_logs resource; pause ;;
      7) view_logs frontend; pause ;;
      8) view_logs infra; pause ;;
      9)
        if select_service "Follow logs for"; then
          show_logs "$SELECTED_SERVICE"
        fi
        ;;
      0|b|B|back) return 0 ;;
      *) warn "Invalid option"; sleep 1 ;;
    esac
  done
}

start_named() {
  case "${1:-all}" in
    all)
      start_all
      ;;
    infra)
      print_header
      start_infra
      ;;
    postgres|rabbitmq)
      print_header
      start_infra_service "$1"
      ;;
    eureka|eureka-server)
      print_header
      start_eureka
      ;;
    identity|identity-service)
      print_header
      start_identity
      ;;
    curriculum|curriculum-service)
      print_header
      start_curriculum
      ;;
    resource|resource-service)
      print_header
      start_resource
      ;;
    gateway|gateway-service)
      print_header
      start_gateway
      ;;
    frontend|angular)
      print_header
      start_frontend
      ;;
    *)
      fail "Unknown service: $1"
      show_help
      return 1
      ;;
  esac
}

stop_named() {
  case "${1:-all}" in
    all)
      stop_all
      ;;
    infra)
      print_header
      stop_infra
      ;;
    postgres|rabbitmq)
      print_header
      stop_infra_service "$1"
      ;;
    eureka|eureka-server)
      print_header
      stop_process eureka-server
      ;;
    identity|identity-service)
      print_header
      stop_process identity-service
      ;;
    curriculum|curriculum-service)
      print_header
      stop_process curriculum-service
      ;;
    resource|resource-service)
      print_header
      stop_process resource-service
      ;;
    gateway|gateway-service)
      print_header
      stop_process gateway-service
      ;;
    frontend|angular)
      print_header
      stop_process frontend
      ;;
    *)
      fail "Unknown service: $1"
      show_help
      return 1
      ;;
  esac
}

restart_named() {
  stop_named "${1:-all}"
  start_named "${1:-all}"
}

show_help() {
  cat <<'EOF'
Usage: ./manage-services.sh [command] [service]

Commands:
  menu                     Open the interactive menu (default)
  start [service]          Start a service or all services
  stop [service]           Stop a service or all services
  restart [service]        Restart a service or all services
  status                   Show detailed service status
  health                   Run health checks for all services
  open [service]           Open a service URL in the browser
  logs [service]           Follow logs (all, infra, eureka, identity, curriculum, resource, gateway, frontend)
  view-logs [service]      Show recent logs without following
  help                     Show this help

Services:
  all, infra, postgres, rabbitmq, eureka, identity, curriculum, resource, gateway, frontend

Interactive menu:
  The main menu shows live status for every service. From there you can start,
  stop or restart everything, manage one service, run health checks, browse or
  follow logs, and open a service in the browser. Use 0, q or b to go back.
EOF
}

service_actions_menu() {
  local svc="$1"
  local choice name state url

  while true; do
    print_header
    name="$(service_label "$svc")"
    state="$(service_state "$svc")"
    url="$(service_url "$svc")"

    printf '\n%s%s%s  %s\n' "$C_BOLD" "$name" "$C_RESET" "$(state_colored "$state")"
    [[ -n "$url" ]] && printf '%s%s%s\n' "$C_DIM" "$url" "$C_RESET"
    printf '\n'
    printf '  %s1)%s Start\n' "$C_CYAN" "$C_RESET"
    printf '  %s2)%s Stop\n' "$C_CYAN" "$C_RESET"
    printf '  %s3)%s Restart\n' "$C_CYAN" "$C_RESET"
    printf '  %s4)%s View recent logs\n' "$C_CYAN" "$C_RESET"
    printf '  %s5)%s Follow logs\n' "$C_CYAN" "$C_RESET"
    printf '  %s6)%s Open in browser\n' "$C_CYAN" "$C_RESET"
    printf '  %s0)%s Back\n\n' "$C_CYAN" "$C_RESET"
    printf 'Choose an action: '
    read -r choice || return 0

    case "$choice" in
      1) start_named "$svc" || pause ;;
      2) stop_named "$svc" || pause ;;
      3) restart_named "$svc" || pause ;;
      4) view_logs "$svc"; pause ;;
      5) show_logs "$svc" ;;
      6) open_url "$url"; pause ;;
      0|b|B|back) return 0 ;;
      *) warn "Invalid option"; sleep 1 ;;
    esac
  done
}

interactive_menu() {
  local choice url
  while true; do
    print_header
    dashboard
    printf '\n%sActions%s\n' "$C_BOLD" "$C_RESET"
    printf '  %s1)%s Start all services\n' "$C_CYAN" "$C_RESET"
    printf '  %s2)%s Stop all services\n' "$C_CYAN" "$C_RESET"
    printf '  %s3)%s Restart all services\n' "$C_CYAN" "$C_RESET"
    printf '  %s4)%s Manage one service\n' "$C_CYAN" "$C_RESET"
    printf '  %s5)%s Health check\n' "$C_CYAN" "$C_RESET"
    printf '  %s6)%s View logs\n' "$C_CYAN" "$C_RESET"
    printf '  %s7)%s Detailed status\n' "$C_CYAN" "$C_RESET"
    printf '  %s8)%s Open a service in browser\n' "$C_CYAN" "$C_RESET"
    printf '  %s0)%s Exit\n\n' "$C_CYAN" "$C_RESET"
    printf 'Choose an option: '
    read -r choice || break

    case "$choice" in
      1) start_all ;;
      2) confirm "Stop all services?" && stop_all ;;
      3) confirm "Restart all services?" && restart_all ;;
      4) select_service "Manage a service" && service_actions_menu "$SELECTED_SERVICE" ;;
      5) health_check; pause ;;
      6) logs_menu ;;
      7) show_status; pause ;;
      8)
        if select_service "Open a service in browser"; then
          url="$(service_url "$SELECTED_SERVICE")"
          open_url "$url"
          pause
        fi
        ;;
      0|q|Q|exit) break ;;
      *) warn "Invalid option"; sleep 1 ;;
    esac
  done
}

command="${1:-menu}"
argument="${2:-all}"

if [[ "$command" == "menu" && ! -t 0 ]]; then
  show_help
  exit 1
fi

case "$command" in
  menu)
    interactive_menu
    ;;
  start)
    start_named "$argument"
    show_status
    ;;
  stop)
    stop_named "$argument"
    show_status
    ;;
  restart)
    restart_named "$argument"
    show_status
    ;;
  status)
    show_status
    ;;
  health)
    health_check
    ;;
  open)
    open_url "$(service_url "$argument")"
    ;;
  logs)
    show_logs "$argument"
    ;;
  view-logs)
    view_logs "$argument"
    ;;
  help|-h|--help)
    show_help
    ;;
  *)
    fail "Unknown command: ${command}"
    show_help
    exit 1
    ;;
esac
