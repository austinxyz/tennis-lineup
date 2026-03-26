variable "fly_api_token" {
  description = "Fly.io API Token (run: flyctl auth token)"
  type        = string
  sensitive   = true
}

variable "fly_org" {
  description = "Fly.io organization slug (run: flyctl orgs list)"
  type        = string
}

variable "fly_app_name" {
  description = "App name — becomes <app_name>.fly.dev (must be globally unique on Fly.io)"
  type        = string
}

variable "fly_region" {
  description = "Fly.io deployment region code (hkg=Hong Kong, nrt=Tokyo, sin=Singapore)"
  type        = string
  default     = "hkg"
}

variable "zhipu_api_key" {
  description = "Zhipu AI API Key (leave empty to use rule-based fallback)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "memory_mb" {
  description = "Machine memory in MB (256 = free tier)"
  type        = number
  default     = 256
}

variable "volume_size_gb" {
  description = "Persistent volume size in GB (stores tennis-data.json)"
  type        = number
  default     = 1
}
