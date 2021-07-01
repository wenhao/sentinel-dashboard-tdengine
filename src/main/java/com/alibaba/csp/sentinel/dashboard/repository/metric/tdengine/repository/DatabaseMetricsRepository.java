package com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.repository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.model.SentinelMetric;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(MetricEntity metric) {
        if (Objects.isNull(metric) || StringUtil.isBlank(metric.getApp())) {
            return;
        }
        Optional.of(metric)
                .map(this::toSentinelMetric)
                .ifPresent(entityManager::persist);
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
        StringBuilder hql = new StringBuilder();
        hql.append("FROM SentinelMetric");
        hql.append(" WHERE app=:app");
        hql.append(" AND resource=:resource");
        hql.append(" AND timestamp>=:startTime");
        hql.append(" AND timestamp<=:endTime");

        TypedQuery<SentinelMetric> query = entityManager.createQuery(hql.toString(), SentinelMetric.class);
        query.setParameter("app", app);
        query.setParameter("resource", resource);
        query.setParameter("startTime", Date.from(Instant.ofEpochMilli(startTime)));
        query.setParameter("endTime", Date.from(Instant.ofEpochMilli(endTime)));
        return Optional.ofNullable(query.getResultList())
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
        StringBuilder hql = new StringBuilder();
        hql.append("FROM SentinelMetric");
        hql.append(" WHERE app=:app");
        hql.append(" AND timestamp>=:startTime");
        TypedQuery<SentinelMetric> query = entityManager.createQuery(hql.toString(), SentinelMetric.class);
        query.setParameter("app", app);
        query.setParameter("startTime", Date.from(Instant.ofEpochMilli(startTime)));

        List<MetricEntity> sentinelMetrics = query.getResultList().stream()
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
