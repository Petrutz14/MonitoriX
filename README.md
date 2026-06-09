# MonitoriX

A real-time PC monitoring system. A lightweight Python agent runs on each machine and sends system metrics to a Spring Boot backend every 15 seconds. An Angular dashboard displays live CPU, RAM, and disk usage across all connected machines and fires alerts based on configurable rules.

## Stack

Spring Boot 4 / Java 25, Angular 21, PostgreSQL 16, Python 3

## How it works

The agent collects CPU usage, RAM usage, disk usage, uptime, top processes, and disk partition info, then POSTs them to the backend. The backend persists the data in PostgreSQL, evaluates alert rules on every ingestion, and broadcasts updates to the frontend over WebSocket (STOMP). The dashboard subscribes to live metric and alert feeds and shows per-machine detail views with historical charts.

Alert rules can be scoped to a specific machine or applied globally. Each rule targets a metric type, an operator (higher, lower, equal), a threshold, and a severity level (low, medium, high, critical). When a rule triggers an alert is created and pushed to the frontend in real time. It resolves automatically when the condition clears.

## Components

**monitor_pc** is the backend. It exposes a REST API for machines, metrics, alert rules, and alerts. Start PostgreSQL first with `docker-compose up -d`, then start the app with `./mvnw spring-boot:run`. It listens on port 8080.

**MonitorX_Frontend** is the Angular dashboard. Run `npm install` then `ng serve`. It listens on port 4200.

**MonitorX_Metrics.py** is the agent. Install dependencies with `pip install psutil requests` and run it with `python MonitorX_Metrics.py` on any machine you want to monitor. It auto-detects the machine hostname, OS, and local IP address.
