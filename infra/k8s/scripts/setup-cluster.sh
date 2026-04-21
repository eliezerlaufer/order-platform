#!/usr/bin/env bash
# =============================================================================
# ORDER PLATFORM — Cluster Bootstrap Script
# =============================================================================
# Creates a local Kubernetes cluster (kind or minikube) and installs
# the Strimzi operator + nginx ingress controller.
#
# Usage:
#   ./setup-cluster.sh              # auto-detect kind or minikube
#   ./setup-cluster.sh kind         # force kind
#   ./setup-cluster.sh minikube     # force minikube
# =============================================================================
set -euo pipefail

CLUSTER_NAME="order-platform"
NAMESPACE="order-platform"
STRIMZI_VERSION="0.44.0"
DRIVER="${1:-auto}"

# --- Colors ----------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# --- Detect cluster tool ---------------------------------------------------
detect_tool() {
  if [ "$DRIVER" != "auto" ]; then
    echo "$DRIVER"
    return
  fi
  if command -v kind &>/dev/null; then
    echo "kind"
  elif command -v minikube &>/dev/null; then
    echo "minikube"
  else
    err "Neither 'kind' nor 'minikube' found. Install one of them first."
    err "  kind:     https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    err "  minikube: https://minikube.sigs.k8s.io/docs/start/"
    exit 1
  fi
}

# --- Pre-flight checks -----------------------------------------------------
preflight() {
  for cmd in kubectl helm docker; do
    if ! command -v "$cmd" &>/dev/null; then
      err "'$cmd' is required but not found. Please install it first."
      exit 1
    fi
  done
  log "Pre-flight checks passed (kubectl, helm, docker found)"
}

# --- Create kind cluster ----------------------------------------------------
create_kind_cluster() {
  if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    warn "kind cluster '${CLUSTER_NAME}' already exists — skipping creation"
    return
  fi

  log "Creating kind cluster '${CLUSTER_NAME}'..."
  cat <<EOF | kind create cluster --name "${CLUSTER_NAME}" --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
  - role: worker
EOF
  log "kind cluster '${CLUSTER_NAME}' created"
}

# --- Create minikube cluster ------------------------------------------------
create_minikube_cluster() {
  if minikube status -p "${CLUSTER_NAME}" &>/dev/null; then
    warn "minikube cluster '${CLUSTER_NAME}' already exists — skipping creation"
    return
  fi

  log "Creating minikube cluster '${CLUSTER_NAME}'..."
  minikube start \
    --profile "${CLUSTER_NAME}" \
    --cpus 4 \
    --memory 8192 \
    --driver docker \
    --kubernetes-version v1.30.0

  log "Enabling minikube addons..."
  minikube addons enable ingress -p "${CLUSTER_NAME}"
  minikube addons enable metrics-server -p "${CLUSTER_NAME}"

  log "minikube cluster '${CLUSTER_NAME}' created"
}

# --- Install nginx ingress controller (kind only) --------------------------
install_ingress_kind() {
  log "Installing nginx ingress controller for kind..."
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

  log "Waiting for ingress controller to be ready..."
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=120s
  log "nginx ingress controller ready"
}

# --- Install Strimzi operator -----------------------------------------------
install_strimzi() {
  log "Installing Strimzi operator v${STRIMZI_VERSION}..."

  # Create namespace if it doesn't exist
  kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

  # Install Strimzi cluster operator
  kubectl apply -f "https://strimzi.io/install/latest?namespace=${NAMESPACE}" -n "${NAMESPACE}"

  log "Waiting for Strimzi operator to be ready..."
  kubectl wait deployment/strimzi-cluster-operator \
    --for=condition=Available \
    --timeout=180s \
    -n "${NAMESPACE}"

  log "Strimzi operator v${STRIMZI_VERSION} ready"
}

# --- Install metrics-server (kind only) -------------------------------------
install_metrics_server_kind() {
  log "Installing metrics-server for kind..."
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

  # Patch for kind (self-signed certs)
  kubectl patch deployment metrics-server \
    -n kube-system \
    --type='json' \
    -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]' \
    2>/dev/null || true

  log "metrics-server installed"
}

# --- Main -------------------------------------------------------------------
main() {
  preflight

  TOOL=$(detect_tool)
  log "Using cluster tool: ${TOOL}"

  case "$TOOL" in
    kind)
      create_kind_cluster
      install_ingress_kind
      install_metrics_server_kind
      ;;
    minikube)
      create_minikube_cluster
      ;;
    *)
      err "Unknown tool: ${TOOL}. Use 'kind' or 'minikube'."
      exit 1
      ;;
  esac

  install_strimzi

  log ""
  log "============================================="
  log "  Cluster '${CLUSTER_NAME}' is ready!"
  log "============================================="
  log ""
  log "Next steps:"
  log "  1. Build images:  ./infra/k8s/scripts/build-images.sh"
  log "  2. Deploy:        ./infra/k8s/scripts/deploy.sh"
  log ""
}

main "$@"
