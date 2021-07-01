package com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.repository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.model.SentinelMetric;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
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

@Primary
@Transactional
@Repository
public class DatabaseMetricsRepository implements MetricsRepository<MetricEntity> {

    private final SentinelMetricMapper sentinelMetricMapper;

    @Autowired
    public DatabaseMetricsRepository(SentinelMetricMapper sentinelMetricMapper) {
        this.sentinelMetricMapper = sentinelMetricMapper;
    }

    @Override
    public void save(MetricEntity metric) {
        if (Objects.isNull(metric) || StringUtil.isBlank(metric.getApp())) {
            return;
        }
        Optional.of(metric)
                .map(this::toSentinelMetric)
                .ifPresent(sentinelMetricMapper::insert);
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        metrics.forEach(this::save);
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        if (StringUtil.isBlank(app) || StringUtil.isBlank(resource)) {
            return Lists.newArrayList();
        }
        List<SentinelMetric> sentinelMetrics = sentinelMetricMapper.findAllByAppAndResourceBetween(app, resource, Date.from(Instant.ofEpochMilli(startTime)), Date.from(Instant.ofEpochMilli(endTime)));
        return Optional.ofNullable(sentinelMetrics)
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
        List<MetricEntity> sentinelMetrics = sentinelMetricMapper.findAllByResourcesOfApp(app, Date.from(Instant.ofEpochMilli(startTime))).stream()
                .map(this::toMetricEntity)
                .collect(Collectors.toList());
        Map<String, MetricEntity> resourceCount = new HashMap<>(32);

        for (MetricEntity metricEntity : sentinelMetrics) {
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
