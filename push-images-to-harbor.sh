#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  HARBOR_REGISTRY=harbor.example.com HARBOR_PROJECT=yaadt \
    ./push-images-to-harbor.sh [tag] [--no-build]

Environment:
  HARBOR_REGISTRY  Harbor host, without https:// (required)
  HARBOR_PROJECT   Harbor project/namespace (required)
  IMAGE_TAG        Image tag; overridden by the first positional argument
  BUILD_PLATFORMS  Buildx platforms (default: linux/amd64,linux/arm64)

The script expects Docker to already be logged in to Harbor:
  docker login harbor.example.com
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

registry="${HARBOR_REGISTRY:-}"
project="${HARBOR_PROJECT:-}"
tag="${IMAGE_TAG:-}"
positional_tag=""
build=true

for argument in "$@"; do
  case "$argument" in
    --no-build) build=false ;;
    -*) echo "Unknown option: $argument" >&2; usage >&2; exit 2 ;;
    *)
      if [[ -n "$positional_tag" ]]; then
        echo "Only one image tag may be supplied" >&2
        exit 2
      fi
      positional_tag="$argument"
      ;;
  esac
done

if [[ -n "$positional_tag" ]]; then
  tag="$positional_tag"
fi

if [[ -z "$registry" || -z "$project" ]]; then
  echo "HARBOR_REGISTRY and HARBOR_PROJECT are required" >&2
  usage >&2
  exit 2
fi

if [[ "$registry" == *://* || "$registry" == */ ]]; then
  echo "HARBOR_REGISTRY must be a host, without scheme or trailing slash" >&2
  exit 2
fi

if [[ -z "$tag" ]]; then
  tag="$(git rev-parse --short=12 HEAD)"
fi

if [[ "$tag" =~ [^a-zA-Z0-9_.-] ]]; then
  echo "Invalid image tag: $tag" >&2
  exit 2
fi

services=(controller-api controller-worker controller-dashboard)
source_prefix="agentic-dev-team"
platforms="${BUILD_PLATFORMS:-linux/amd64,linux/arm64}"

if [[ "$build" == true ]]; then
  declare -A contexts=(
    [controller-api]=controller
    [controller-worker]=controller
    [controller-dashboard]=controller-dashboard
  )
  for service in "${services[@]}"; do
    target_image="${registry}/${project}/${service}:${tag}"
    docker buildx build \
      --platform "$platforms" \
      --tag "$target_image" \
      --push \
      "${contexts[$service]}"
    printf 'Built and pushed %s for %s\n' "$target_image" "$platforms"
  done
  exit 0
fi

for service in "${services[@]}"; do
  source_image="${source_prefix}-${service}:latest"
  target_image="${registry}/${project}/${service}:${tag}"

  docker image inspect "$source_image" >/dev/null
  docker tag "$source_image" "$target_image"
  docker push "$target_image"
  printf 'Pushed %s\n' "$target_image"
done

printf '\nImages pushed with tag %s\n' "$tag"
