package com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.repository;

import com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.model.SentinelMetric;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface SentinelMetricRepository extends CrudRepository<SentinelMetric, Long> {

    Optional<List<SentinelMetric>> findAllByAppAndResourceAndTimestampBetween(String app, String resource, Date startTime, Date endTime);

    Optional<List<SentinelMetric>> findAllByAppAndTimestampGreaterThanEqual(String app, Date startTime);
}
