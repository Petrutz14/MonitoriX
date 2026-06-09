import psutil
import requests
import time
import platform
import socket

API_URL = "http://localhost:8080/api/metrics"
MACHINE_ID = platform.node()
DISPLAY_NAME = platform.node()   # change this to a friendlier name if you want
INTERVAL = 15

# Root mountpoint differs by OS
ROOT_MOUNT = 'C:\\' if platform.system() == 'Windows' else '/'


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
    """Call cpu_percent(interval=None) on every process to set baseline timestamps."""
    for p in psutil.process_iter():
        try:
            p.cpu_percent(interval=None)
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass


CPU_COUNT = psutil.cpu_count() or 1
# Windows kernel pseudo-processes — can't be primed, always return garbage CPU values
SKIP_PIDS = {0, 4} if platform.system() == 'Windows' else set()

def get_top_processes(n=5):
    """Read cpu_percent after counters have already been primed + slept."""
    total_ram = psutil.virtual_memory().total
    max_cpu   = 100.0 * CPU_COUNT   # theoretical ceiling for one process
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


while True:
    # Prime process CPU counters at the very start of the cycle
    prime_cpu_counters()

    # Collect CPU (1 s interval) and RAM concurrently with the prime sleep
    cpu = psutil.cpu_percent(interval=1)

    # Sleep the remaining 0.5 s so process counters have a full ~1.5 s window
    time.sleep(0.5)

    root_usage = psutil.disk_usage(ROOT_MOUNT)
    ram = psutil.virtual_memory()

    data = {
        "machineId":    MACHINE_ID,
        "displayName":  DISPLAY_NAME,
        "cpuPercent":   cpu,
        "ramPercent":   ram.percent,
        "ramUsedGb":    round(ram.used  / (1024 ** 3), 2),
        "diskPercent":  root_usage.percent,
        "diskFreeGb":   round(root_usage.free / (1024 ** 3), 2),
        "uptimeSeconds": int(time.time() - psutil.boot_time()),
        "osName":       f"{platform.system()} {platform.release()}",
        "ipAddress":    get_ip(),
        "totalRamGb":   round(ram.total / (1024 ** 3), 2),
        "topProcesses": get_top_processes(5),
        "diskPartitions": get_disk_partitions()
    }

    try:
        r = requests.post(API_URL, json=data, timeout=5)
        print(f"Sent: CPU={data['cpuPercent']}% RAM={data['ramPercent']}% "
              f"Disk={data['diskPercent']}% Processes={len(data['topProcesses'])} "
              f"Partitions={len(data['diskPartitions'])} -> {r.status_code}")
    except Exception as e:
        print(f"Failed: {e}")

    time.sleep(INTERVAL - 1.5)  # subtract the 1.5 s already spent above
