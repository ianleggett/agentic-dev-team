{{- define "yaadt-preview.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "yaadt-preview.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "yaadt-preview.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "yaadt-preview.labels" -}}
app.kubernetes.io/name: {{ include "yaadt-preview.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end }}

{{- define "yaadt-preview.selectorLabels" -}}
app.kubernetes.io/name: {{ include "yaadt-preview.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
