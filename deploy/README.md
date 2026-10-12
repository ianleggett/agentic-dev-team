# Kubernetes Preview Deployment

This directory contains a first-draft, namespace-scoped Helm chart for
ephemeral application previews. It deliberately does not create namespaces;
CI should create one per preview and install the chart into it.

The chart does not give the agent Kubernetes or Docker access. A CI runner or
preview controller owns deployment credentials. The agent receives the preview
URL and test artifacts through Beads or the CI system.

## Install

```bash
NAMESPACE=preview-org-chart-bd-a1b2

kubectl create namespace "$NAMESPACE"
helm upgrade --install org-chart deploy/helm/yaadt-preview \
  --namespace "$NAMESPACE" \
  --set image.repository=registry.example.com/org-chart \
  --set image.tag=abc123 \
  --set ingress.hosts[0].host=org-chart-bd-a1b2.preview.example.com
```

The namespace should be labelled for ownership and cleanup:

```bash
kubectl label namespace "$NAMESPACE" \
  yaadt.dev/managed-by=yaadt-ci \
  yaadt.dev/project=org-chart \
  yaadt.dev/instance=bd-a1b2
```

## Playwright

Set `playwright.enabled=true` and provide a CI-built test image containing the
Playwright tests. The chart passes `BASE_URL` to that job. The default job is
disabled because every project has different test packaging and commands.

```bash
helm upgrade --install org-chart deploy/helm/yaadt-preview \
  --namespace "$NAMESPACE" \
  --reuse-values \
  --set playwright.enabled=true \
  --set playwright.image=registry.example.com/org-chart-e2e:abc123 \
  --set playwright.baseURL=https://org-chart-bd-a1b2.preview.example.com
```

## First-draft limitations

- The application container, database, and test image are intentionally generic.
- No namespace, registry, certificate issuer, or ingress controller is assumed.
- The chart does not provide shared Beads/Dolt storage.
- Team Runs aggregation still needs a central API or object-storage design.
- Replace the default image, probes, service port, ingress class, and network
  policy rules for each deployment environment.
