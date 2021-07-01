package com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.repository;

import com.alibaba.csp.sentinel.dashboard.repository.metric.tdengine.model.SentinelMetric;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface SentinelMetricMapper {

    @Insert("insert into sentinel_metric(gmt_create, gmt_modified, app, statistics_timestamp, resource, pass_qps, success_qps, block_qps, exception_qps, rt, statistics_count, resource_code) values(#{gmtCreate}, #{gmtModified}, #{app}, #{timestamp}, #{resource}, #{passQps}, #{successQps}, #{blockQps}, #{exceptionQps}, #{rt}, #{count}, #{resourceCode})")
    void insert(SentinelMetric sentinelMetric);

    @Select("select * from sentinel_metric where app=#{app} and resource=#{resource} and timestamp>=#{startTime} and timestamp<=#{endTime}")
    @Results({
            @Result(property = "gmtCreate", column = "gmt_create"),
            @Result(property = "gmtModified", column = "gmt_modified"),
            @Result(property = "timestamp", column = "statistics_timestamp"),
            @Result(property = "passQps", column = "pass_qps"),
            @Result(property = "successQps", column = "success_qps"),
            @Result(property = "blockQps", column = "block_qps"),
            @Result(property = "exceptionQps", column = "exception_qps"),
            @Result(property = "count", column = "statistics_count"),
            @Result(property = "resourceCode", column = "resource_code")
    })
    List<SentinelMetric> findAllByAppAndResourceBetween(@Param("app") String app, @Param("resource") String resource, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("select * from sentinel_metric where app=#{app} and timestamp>=#{startTime}")
    @Results({
            @Result(property = "gmtCreate", column = "gmt_create"),
            @Result(property = "gmtModified", column = "gmt_modified"),
            @Result(property = "timestamp", column = "statistics_timestamp"),
            @Result(property = "passQps", column = "pass_qps"),
            @Result(property = "successQps", column = "success_qps"),
            @Result(property = "blockQps", column = "block_qps"),
            @Result(property = "exceptionQps", column = "exception_qps"),
            @Result(property = "count", column = "statistics_count"),
            @Result(property = "resourceCode", column = "resource_code")
    })
    List<SentinelMetric> findAllByResourcesOfApp(@Param("app") String app, @Param("startTime") Date startTime);
}
