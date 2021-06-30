package com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.repository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.model.SentinelMetric;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Transactional
@Repository("TDEngineMetricsRepository")
public class TDEngineMetricsRepository implements MetricsRepository<MetricEntity> {

    private final SentinelMetricRepository sentinelMetricRepository;

    @Autowired
    public TDEngineMetricsRepository(SentinelMetricRepository sentinelMetricRepository) {
        this.sentinelMetricRepository = sentinelMetricRepository;
    }

    @Override
    public void save(MetricEntity metric) {
        if (Objects.isNull(metric) || StringUtil.isBlank(metric.getApp())) {
            return;
        }
        Optional.of(metric)
                .map(this::toSentinelMetric)
                .ifPresent(sentinelMetricRepository::save);
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        Optional.ofNullable(metrics)
                .map(it -> StreamSupport.stream(metrics.spliterator(), false)
                        .map(this::toSentinelMetric)
                        .collect(Collectors.toList()))
                .ifPresent(sentinelMetricRepository::saveAll);
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        if (StringUtil.isBlank(app) || StringUtil.isBlank(resource)) {
            return Lists.newArrayList();
        }
        return sentinelMetricRepository.findAllByAppAndResourceAndTimestampBetween(app, resource, Date.from(Instant.ofEpochMilli(startTime)), Date.from(Instant.ofEpochMilli(endTime)))
                .map(it -> it.stream()
                        .map(this::toMetricEntity)
                        .collect(Collectors.toList()))
                .orElse(Lists.newArrayList());
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }

        long startTime = System.currentTimeMillis() - 1000 * 60;
        List<MetricEntity> metricEntities = sentinelMetricRepository.findAllByAppAndTimestampGreaterThanEqual(app, Date.from(Instant.ofEpochMilli(startTime)))
                .map(it -> it.stream()
                        .map(this::toMetricEntity)
                        .collect(Collectors.toList()))
                .orElse(Lists.newArrayList());
        Map<String, MetricEntity> resourceCount = new HashMap<>(32);

        for (MetricEntity metricEntity : metricEntities) {
            String resource = metricEntity.getResource();
            if (resourceCount.containsKey(resource)) {
                MetricEntity oldEntity = resourceCount.get(resource);
                oldEntity.addPassQps(metricEntity.getPassQps());
                oldEntity.addRtAndSuccessQps(metricEntity.getRt(), metricEntity.getSuccessQps());
                oldEntity.addBlockQps(metricEntity.getBlockQps());
                oldEntity.addExceptionQps(metricEntity.getExceptionQps());
                oldEntity.addCount(1);
            } else {
                resourceCount.put(resource, MetricEntity.copyOf(metricEntity));
            }
        }

        // Order by last minute b_qps DESC.
        return resourceCount.entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    MetricEntity e1 = o1.getValue();
                    MetricEntity e2 = o2.getValue();
                    int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                    if (t != 0) {
                        return t;
                    }
                    return e2.getPassQps().compareTo(e1.getPassQps());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private SentinelMetric toSentinelMetric(MetricEntity metricEntity) {
        SentinelMetric sentinelMetric = new SentinelMetric();
        BeanUtils.copyProperties(metricEntity, sentinelMetric);
        return sentinelMetric;
    }

    private MetricEntity toMetricEntity(SentinelMetric sentinelMetric) {
        MetricEntity metricEntity = new MetricEntity();
        BeanUtils.copyProperties(sentinelMetric, metricEntity);
        return metricEntity;
    }
}
