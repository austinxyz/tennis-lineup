terraform {
  required_version = ">= 1.5"
  required_providers {
    fly = {
      source  = "fly-apps/fly"
      version = "~> 0.1"
    }
  }
}

provider "fly" {
  fly_api_token = var.fly_api_token
}

# ── Fly App ───────────────────────────────────────────────────
resource "fly_app" "tennis" {
  name = var.fly_app_name
  org  = var.fly_org
}

# ── 公网 IP ───────────────────────────────────────────────────
resource "fly_ip" "ipv4" {
  app  = fly_app.tennis.name
  type = "v4"

  depends_on = [fly_app.tennis]
}

resource "fly_ip" "ipv6" {
  app  = fly_app.tennis.name
  type = "v6"

  depends_on = [fly_app.tennis]
}

# ── 持久化 Volume（存放 tennis-data.json 和 tennis-config.json）
resource "fly_volume" "data" {
  name   = "tennis_data"
  app    = fly_app.tennis.name
  size   = var.volume_size_gb
  region = var.fly_region

  depends_on = [fly_app.tennis]
}

# ── Machine（应用实例）───────────────────────────────────────
# 注意：fly-apps/fly v0.1.x 的 mounts/services/checks 使用 HCL block 语法，
#       不是 list-of-map 字面量。
resource "fly_machine" "app" {
  app    = fly_app.tennis.name
  region = var.fly_region
  name   = "${var.fly_app_name}-app"

  # 镜像在首次部署时由 docker push + flyctl deploy 推送
  image = "registry.fly.io/${var.fly_app_name}:latest"

  cpus     = 1
  memorymb = var.memory_mb

  env = {
    DATA_FILE_PATH    = "/data/tennis-data.json"
    CONFIG_FILE_PATH  = "/data/tennis-config.json"
    ZHIPU_API_KEY     = var.zhipu_api_key
    SERVER_PORT       = "8080"
    JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
  }

  # block 语法（非 = [...] 赋值）
  mounts {
    volume = fly_volume.data.id
    path   = "/data"
  }

  services {
    ports {
      port     = 443
      handlers = ["tls", "http"]
    }
    ports {
      port     = 80
      handlers = ["http"]
    }
    protocol      = "tcp"
    internal_port = 8080

    checks {
      type     = "http"
      interval = "15s"
      timeout  = "10s"
      path     = "/api/teams"
      method   = "GET"
    }
  }

  depends_on = [fly_volume.data, fly_app.tennis]
}
