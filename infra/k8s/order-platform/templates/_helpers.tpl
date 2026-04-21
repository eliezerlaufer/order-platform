{{/*
Expand the name of the chart.
*/}}
{{- define "order-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "order-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "order-platform.labels" -}}
helm.sh/chart: {{ include "order-platform.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: order-platform
{{ include "order-platform.selectorLabels" . }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "order-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "order-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Chart label
*/}}
{{- define "order-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Component labels — use as: {{ include "order-platform.componentLabels" (dict "component" "order-service" "context" $) }}
*/}}
{{- define "order-platform.componentLabels" -}}
helm.sh/chart: {{ include "order-platform.chart" .context }}
app.kubernetes.io/managed-by: {{ .context.Release.Service }}
app.kubernetes.io/part-of: order-platform
app.kubernetes.io/name: {{ .component }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{/*
Component selector labels
*/}}
{{- define "order-platform.componentSelectorLabels" -}}
app.kubernetes.io/name: {{ .component }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
{{- end }}

{{/*
PostgreSQL connection URL for a given database name
Usage: {{ include "order-platform.jdbcUrl" (dict "database" "orders_db") }}
*/}}
{{- define "order-platform.jdbcUrl" -}}
jdbc:postgresql://postgresql:5432/{{ .database }}
{{- end }}

{{/*
Kafka bootstrap server (Strimzi naming convention)
*/}}
{{- define "order-platform.kafkaBootstrap" -}}
order-platform-kafka-kafka-bootstrap:9092
{{- end }}

{{/*
Keycloak issuer URI
*/}}
{{- define "order-platform.keycloakIssuerUri" -}}
http://keycloak:8080/realms/order-platform
{{- end }}

{{/*
OTel Collector endpoint
*/}}
{{- define "order-platform.otelEndpoint" -}}
http://otel-collector:4317
{{- end }}

{{/*
Prometheus scrape annotations — add to pod template metadata
*/}}
{{- define "order-platform.prometheusAnnotations" -}}
prometheus.io/scrape: "true"
prometheus.io/path: "/actuator/prometheus"
prometheus.io/port: "{{ .port }}"
{{- end }}
