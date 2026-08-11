package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.dto.MetricBucketProjection;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SystemMetricRepository extends JpaRepository<SystemMetric,Long> {
    Optional<SystemMetric> findTop1ByMachineOrderByRecordedAtDesc(Machine machine);

    List<SystemMetric> findAllByMachineAndRecordedAtAfterOrderByRecordedAtDesc(Machine machine, Instant cutoff);
    //Timescale style querry,good for averages
    @Query(value = """
            SELECT
                TO_TIMESTAMP(FLOOR(EXTRACT(EPOCH FROM recorded_at) / :bucketSeconds) * :bucketSeconds) AS bucket,
                AVG(cpu_percent)  AS avgcpu,
                AVG(ram_percent)  AS avgram,
                AVG(disk_percent) AS avgdisk,
                AVG(ram_used_gb)  AS avgramusedgb,
                AVG(disk_free_gb) AS avgdiskfreegb
            FROM system_metric
            WHERE machine_id = :machineId
              AND recorded_at > :cutoff
            GROUP BY bucket
            ORDER BY bucket DESC
            """, nativeQuery = true)
    List<MetricBucketProjection> findBucketedHistory(
            @Param("machineId") Long machineId,
            @Param("cutoff") Instant cutoff,
            @Param("bucketSeconds") int bucketSeconds);
}
