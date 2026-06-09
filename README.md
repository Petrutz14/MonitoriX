# MonitoriX

A real-time PC monitoring system. A Python agent runs on each machine and sends system metrics to a Spring Boot backend every 15 seconds. An Angular dashboard displays live CPU, RAM, and disk usage and fires alerts based on configurable rules.

## Stack

Spring Boot 4 / Java 25, Angular 21, PostgreSQL, Python 3

## Components

**monitor_pc** is the backend. Start PostgreSQL with `docker-compose up -d` then run the app with `./mvnw spring-boot:run`.

**MonitorX_Frontend** is the dashboard. Run `npm install` then `ng serve`.

**MonitorX_Metrics.py** is the agent. Run it with `python MonitorX_Metrics.py` on any machine you want to monitor. It requires `psutil` and `requests`.
