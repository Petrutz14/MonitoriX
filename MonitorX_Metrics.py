import psutil
import requests
import time
import platform
import socket

BASE_URL = "http://localhost:8080"
API_URL = f"{BASE_URL}/api/metrics"
AUTH_URL = f"{BASE_URL}/api/auth/login"
AGENT_USERNAME = platform.node()
AGENT_PASSWORD = "agent-secret-change-me"
AGENT_REGISTRATION_SECRET = "demo_secret"
REGISTER_AGENT_URL = f"{BASE_URL}/api/auth/register-agent"

MACHINE_ID = platform.node()
DISPLAY_NAME = platform.node()
INTERVAL = 15
MEASURE_SECONDS = 1.5  # cpu_percent(1s) + 0.5s process-counter settle

ROOT_MOUNT = 'C:\\' if platform.system() == 'Windows' else '/'
CPU_COUNT = psutil.cpu_count() or 1
# PIDs 0 and 4 are Windows kernel pseudo-processes — cpu_percent always returns garbage for them
SKIP_PIDS = {0, 4} if platform.system() == 'Windows' else set()

_token = None


def get_token():
    global _token
    resp = requests.post(AUTH_URL, json={"username": AGENT_USERNAME, "password": AGENT_PASSWORD}, timeout=5)
    resp.raise_for_status()
    _token = resp.json()["token"]


def ensure_authenticated():
    try:
        get_token()
    except requests.HTTPError:
        requests.post(
            REGISTER_AGENT_URL,
            json={"username": AGENT_USERNAME, "password": AGENT_PASSWORD},
            headers={"X-Agent-Secret": AGENT_REGISTRATION_SECRET},
            timeout=5
        ).raise_for_status()
        get_token()


def auth_headers():
    return {"Authorization": f"Bearer {_token}"}


def get_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "unknown"


def prime_cpu_counters():
    for p in psutil.process_iter():
        try:
            p.cpu_percent(interval=None)
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass


def get_top_processes(n=5):
    total_ram = psutil.virtual_memory().total
    max_cpu = 100.0 * CPU_COUNT
    procs = []
    for p in psutil.process_iter(['pid', 'name', 'memory_percent']):
        try:
            if p.info['pid'] in SKIP_PIDS:
                continue
            cpu = min(p.cpu_percent(interval=None), max_cpu)
            mem = p.info['memory_percent'] or 0.0
            procs.append({
                "pid": p.info['pid'],
                "name": p.info['name'],
                "cpuPercent": round(cpu, 1),
                "ramPercent": round(mem, 2),
                "ramUsedMb": round(mem / 100 * total_ram / (1024 * 1024), 1)
            })
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
    procs.sort(key=lambda x: x['cpuPercent'], reverse=True)
    return procs[:n]


def get_disk_partitions():
    partitions = []
    for part in psutil.disk_partitions():
        try:
            usage = psutil.disk_usage(part.mountpoint)
            partitions.append({
                "device": part.device,
                "mountPoint": part.mountpoint,
                "fileSystem": part.fstype,
                "totalGb": round(usage.total / (1024 ** 3), 2),
                "usedGb": round(usage.used / (1024 ** 3), 2),
                "percent": usage.percent
            })
        except (PermissionError, OSError):
            continue
    return partitions


ensure_authenticated()

while True:
    prime_cpu_counters()
    cpu = psutil.cpu_percent(interval=1)
    time.sleep(0.5)  # let process counters settle over the full ~1.5s window

    root_usage = psutil.disk_usage(ROOT_MOUNT)
    ram = psutil.virtual_memory()

    data = {
        "machineId":      MACHINE_ID,
        "displayName":    DISPLAY_NAME,
        "cpuPercent":     cpu,
        "ramPercent":     ram.percent,
        "ramUsedGb":      round(ram.used / (1024 ** 3), 2),
        "diskPercent":    root_usage.percent,
        "diskFreeGb":     round(root_usage.free / (1024 ** 3), 2),
        "uptimeSeconds":  int(time.time() - psutil.boot_time()),
        "osName":         f"{platform.system()} {platform.release()}",
        "ipAddress":      get_ip(),
        "totalRamGb":     round(ram.total / (1024 ** 3), 2),
        "topProcesses":   get_top_processes(5),
        "diskPartitions": get_disk_partitions()
    }

    try:
        r = requests.post(API_URL, json=data, headers=auth_headers(), timeout=5)
        if r.status_code == 401:
            get_token()
            r = requests.post(API_URL, json=data, headers=auth_headers(), timeout=5)
        print(f"Sent: CPU={data['cpuPercent']}% RAM={data['ramPercent']}% "
              f"Disk={data['diskPercent']}% -> {r.status_code}")
    except Exception as e:
        print(f"Failed: {e}")

    time.sleep(INTERVAL - MEASURE_SECONDS)
