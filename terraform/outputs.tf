output "app_url" {
  description = "Application URL"
  value       = "https://${fly_app.tennis.name}.fly.dev"
}

output "app_name" {
  description = "Fly.io app name"
  value       = fly_app.tennis.name
}

output "ipv4" {
  description = "Public IPv4 address"
  value       = fly_ip.ipv4.address
}
