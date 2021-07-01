CREATE DATABASE IF NOT EXISTS sentinel_metric;

USE sentinel_metric;

CREATE TABLE IF NOT EXISTS sentinel_metric(
    gmt_create TIMESTAMP,
    id BIGINT,
    gmt_modified TIMESTAMP,
    app NCHAR(100),
    statistics_timestamp TIMESTAMP,
    resource NCHAR(500),
    pass_qps BIGINT,
    success_qps BIGINT,
    block_qps BIGINT,
    exception_qps BIGINT,
    rt FLOAT,
    statistics_count INT,
    resource_code INT
);
