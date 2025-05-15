/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.pojo;

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.TaskProfile;
import org.apache.inlong.agent.constant.CycleUnitType;
import org.apache.inlong.agent.pojo.BinlogTask.BinlogTaskConfig;
import org.apache.inlong.agent.pojo.COSTask.COSTaskConfig;
import org.apache.inlong.agent.pojo.FileTask.FileTaskConfig;
import org.apache.inlong.agent.pojo.FileTask.Line;
import org.apache.inlong.agent.pojo.KafkaTask.KafkaTaskConfig;
import org.apache.inlong.agent.pojo.MongoTask.MongoTaskConfig;
import org.apache.inlong.agent.pojo.MqttTask.MqttConfig;
import org.apache.inlong.agent.pojo.OracleTask.OracleTaskConfig;
import org.apache.inlong.agent.pojo.PostgreSQLTask.PostgreSQLTaskConfig;
import org.apache.inlong.agent.pojo.PulsarTask.PulsarTaskConfig;
import org.apache.inlong.agent.pojo.RedisTask.RedisTaskConfig;
import org.apache.inlong.agent.pojo.SQLTask.SQLTaskConfig;
import org.apache.inlong.agent.pojo.SqlServerTask.SqlserverTaskConfig;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.enums.TaskTypeEnum;
import org.apache.inlong.common.pojo.agent.DataConfig;

import com.google.gson.Gson;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_ADDR;
import static org.apache.inlong.agent.constant.TaskConstants.SYNC_SEND_OPEN;
import static org.apache.inlong.common.enums.DataReportTypeEnum.NORMAL_SEND_TO_DATAPROXY;

@Data
public class TaskProfileDto {

    private static final Logger logger = LoggerFactory.getLogger(TaskProfileDto.class);
    public static final String DEFAULT_CHANNEL = "org.apache.inlong.agent.plugin.channel.MemoryChannel";
    public static final String MANAGER_JOB = "MANAGER_JOB";
    public static final String DEFAULT_DATA_PROXY_SINK = "org.apache.inlong.agent.plugin.sinks.ProxySink";
    public static final String PULSAR_SINK = "org.apache.inlong.agent.plugin.sinks.PulsarSink";
    public static final String KAFKA_SINK = "org.apache.inlong.agent.plugin.sinks.KafkaSink";
    private static final Gson GSON = new Gson();
    public static final String deafult_time_offset = "0";
    private static final String DEFAULT_AUDIT_VERSION = "0";

    private Task task;
    private Proxy proxy;

    private static BinlogTask getBinlogTask(DataConfig dataConfigs) {
        BinlogTaskConfig binlogTaskConfig = GSON.fromJson(dataConfigs.getExtParams(),
                BinlogTaskConfig.class);

        BinlogTask binlogTask = new BinlogTask();
        binlogTask.setHostname(binlogTaskConfig.getHostname());
        binlogTask.setPassword(binlogTaskConfig.getPassword());
        binlogTask.setUser(binlogTaskConfig.getUser());
        binlogTask.setTableWhiteList(binlogTaskConfig.getTableWhiteList());
        binlogTask.setDatabaseWhiteList(binlogTaskConfig.getDatabaseWhiteList());
        binlogTask.setSchema(binlogTaskConfig.getIncludeSchema());
        binlogTask.setPort(binlogTaskConfig.getPort());
        binlogTask.setOffsets(dataConfigs.getSnapshot());
        binlogTask.setDdl(binlogTaskConfig.getMonitoredDdl());
        binlogTask.setServerTimezone(binlogTaskConfig.getServerTimezone());

        BinlogTask.Offset offset = new BinlogTask.Offset();
        offset.setIntervalMs(binlogTaskConfig.getIntervalMs());
        offset.setFilename(binlogTaskConfig.getOffsetFilename());
        offset.setSpecificOffsetFile(binlogTaskConfig.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(binlogTaskConfig.getSpecificOffsetPos());

        binlogTask.setOffset(offset);

        BinlogTask.Snapshot snapshot = new BinlogTask.Snapshot();
        snapshot.setMode(binlogTaskConfig.getSnapshotMode());

        binlogTask.setSnapshot(snapshot);

        BinlogTask.History history = new BinlogTask.History();
        history.setFilename(binlogTaskConfig.getHistoryFilename());

        binlogTask.setHistory(history);

        return binlogTask;
    }

    private static FileTask getFileTask(DataConfig dataConfig) {
        FileTask fileTask = new FileTask();
        fileTask.setId(dataConfig.getTaskId());

        FileTaskConfig taskConfig = GSON.fromJson(dataConfig.getExtParams(),
                FileTaskConfig.class);

        FileTask.Dir dir = new FileTask.Dir();
        dir.setPatterns(taskConfig.getPattern().trim());
        dir.setBlackList(taskConfig.getBlackList());
        fileTask.setDir(dir);
        fileTask.setCollectType(taskConfig.getCollectType());
        fileTask.setContentCollectType(taskConfig.getContentCollectType());
        fileTask.setDataContentStyle(taskConfig.getDataContentStyle());
        fileTask.setDataSeparator(taskConfig.getDataSeparator());
        fileTask.setMaxFileCount(taskConfig.getMaxFileCount());
        fileTask.setRetry(taskConfig.getRetry());
        fileTask.setCycleUnit(taskConfig.getCycleUnit());
        fileTask.setDataTimeFrom(taskConfig.getDataTimeFrom());
        fileTask.setDataTimeTo(taskConfig.getDataTimeTo());
        if (taskConfig.getFilterStreams() != null) {
            fileTask.setFilterStreams(GSON.toJson(taskConfig.getFilterStreams()));
        }
        if (taskConfig.getTimeOffset() != null) {
            fileTask.setTimeOffset(taskConfig.getTimeOffset());
        } else {
            fileTask.setTimeOffset(deafult_time_offset + fileTask.getCycleUnit());
        }

        if (taskConfig.getAdditionalAttr() != null) {
            fileTask.setAddictiveString(taskConfig.getAdditionalAttr());
        }

        if (null != taskConfig.getLineEndPattern()) {
            Line line = new Line();
            line.setEndPattern(taskConfig.getLineEndPattern());
            fileTask.setLine(line);
        }

        if (null != taskConfig.getMonitorInterval()) {
            fileTask.setMonitorInterval(taskConfig.getMonitorInterval());
        }

        if (null != taskConfig.getMonitorStatus()) {
            fileTask.setMonitorStatus(taskConfig.getMonitorStatus());
        }
        return fileTask;
    }

    private static COSTask getCOSTask(DataConfig dataConfig) {
        COSTask cosTask = new COSTask();
        cosTask.setId(dataConfig.getTaskId());
        COSTaskConfig taskConfig = GSON.fromJson(dataConfig.getExtParams(),
                COSTaskConfig.class);
        cosTask.setPattern(taskConfig.getPattern().trim());
        cosTask.setCollectType(taskConfig.getCollectType());
        cosTask.setContentStyle(taskConfig.getContentStyle());
        cosTask.setDataSeparator(taskConfig.getDataSeparator());
        cosTask.setMaxFileCount(taskConfig.getMaxFileCount());
        cosTask.setRetry(taskConfig.getRetry());
        cosTask.setCycleUnit(taskConfig.getCycleUnit());
        cosTask.setDataTimeFrom(taskConfig.getDataTimeFrom());
        cosTask.setDataTimeTo(taskConfig.getDataTimeTo());
        cosTask.setBucketName(taskConfig.getBucketName());
        cosTask.setSecretId(taskConfig.getCredentialsId());
        cosTask.setSecretKey(taskConfig.getCredentialsKey());
        cosTask.setRegion(taskConfig.getRegion());
        if (taskConfig.getFilterStreams() != null) {
            cosTask.setFilterStreams(GSON.toJson(taskConfig.getFilterStreams()));
        }
        if (taskConfig.getTimeOffset() != null) {
            cosTask.setTimeOffset(taskConfig.getTimeOffset());
        } else {
            cosTask.setTimeOffset(deafult_time_offset + cosTask.getCycleUnit());
        }
        return cosTask;
    }

    private static SQLTask getSQLTask(DataConfig dataConfig) {
        SQLTask sqlTask = new SQLTask();
        sqlTask.setId(dataConfig.getTaskId());
        SQLTaskConfig taskConfig = GSON.fromJson(dataConfig.getExtParams(),
                SQLTaskConfig.class);
        sqlTask.setSql(taskConfig.getSql());
        sqlTask.setMaxInstanceCount(taskConfig.getMaxInstanceCount());
        sqlTask.setRetry(taskConfig.getRetry());
        sqlTask.setCycleUnit(taskConfig.getCycleUnit());
        sqlTask.setDataTimeFrom(taskConfig.getDataTimeFrom());
        sqlTask.setDataTimeTo(taskConfig.getDataTimeTo());
        sqlTask.setUsername(taskConfig.getUsername());
        sqlTask.setJdbcPassword(taskConfig.getJdbcPassword());
        sqlTask.setDataSeparator(taskConfig.getDataSeparator());
        sqlTask.setFetchSize(taskConfig.getFetchSize());
        sqlTask.setJdbcUrl(taskConfig.getJdbcUrl());
        if (taskConfig.getTimeOffset() != null) {
            sqlTask.setTimeOffset(taskConfig.getTimeOffset());
        } else {
            sqlTask.setTimeOffset(deafult_time_offset + sqlTask.getCycleUnit());
        }
        return sqlTask;
    }

    private static KafkaTask getKafkaTask(DataConfig dataConfigs) {

        KafkaTaskConfig kafkaTaskConfig = GSON.fromJson(dataConfigs.getExtParams(),
                KafkaTaskConfig.class);
        KafkaTask kafkaTask = new KafkaTask();

        KafkaTask.Bootstrap bootstrap = new KafkaTask.Bootstrap();
        bootstrap.setServers(kafkaTaskConfig.getBootstrapServers());
        kafkaTask.setBootstrap(bootstrap);
        KafkaTask.Partition partition = new KafkaTask.Partition();
        partition.setOffset(kafkaTaskConfig.getPartitionOffsets());
        kafkaTask.setPartition(partition);
        KafkaTask.Group group = new KafkaTask.Group();
        group.setId(kafkaTaskConfig.getGroupId());
        kafkaTask.setGroup(group);
        KafkaTask.RecordSpeed recordSpeed = new KafkaTask.RecordSpeed();
        recordSpeed.setLimit(kafkaTaskConfig.getRecordSpeedLimit());
        kafkaTask.setRecordSpeed(recordSpeed);
        KafkaTask.ByteSpeed byteSpeed = new KafkaTask.ByteSpeed();
        byteSpeed.setLimit(kafkaTaskConfig.getByteSpeedLimit());
        kafkaTask.setByteSpeed(byteSpeed);
        kafkaTask.setAutoOffsetReset(kafkaTaskConfig.getAutoOffsetReset());

        kafkaTask.setTopic(kafkaTaskConfig.getTopic());

        return kafkaTask;
    }

    private static PulsarTask getPulsarTask(DataConfig dataConfig) {
        PulsarTaskConfig pulsarTaskConfig = GSON.fromJson(dataConfig.getExtParams(),
                PulsarTaskConfig.class);
        PulsarTask pulsarTask = new PulsarTask();

        pulsarTask.setTenant(pulsarTaskConfig.getPulsarTenant());
        pulsarTask.setNamespace(pulsarTaskConfig.getNamespace());
        pulsarTask.setTopic(pulsarTaskConfig.getTopic());
        pulsarTask.setSubscription(pulsarTaskConfig.getSubscription());
        pulsarTask.setSubscriptionType(pulsarTaskConfig.getSubscriptionType());
        pulsarTask.setServiceUrl(pulsarTaskConfig.getServiceUrl());
        pulsarTask.setSubscriptionPosition(pulsarTaskConfig.getScanStartupMode());
        pulsarTask.setResetTime(pulsarTaskConfig.getResetTime());

        return pulsarTask;
    }

    private static PostgreSQLTask getPostgresTask(DataConfig dataConfigs) {
        PostgreSQLTaskConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                PostgreSQLTaskConfig.class);
        PostgreSQLTask postgreSQLTask = new PostgreSQLTask();

        postgreSQLTask.setUser(config.getUsername());
        postgreSQLTask.setPassword(config.getPassword());
        postgreSQLTask.setHostname(config.getHostname());
        postgreSQLTask.setPort(config.getPort());
        postgreSQLTask.setDbname(config.getDatabase());
        postgreSQLTask.setSchemaIncludeList(config.getSchema());
        postgreSQLTask.setPluginName(config.getDecodingPluginName());
        // Each identifier is of the form schemaName.tableName and connected with ","
        postgreSQLTask.setTableIncludeList(
                config.getTableNameList().stream().map(tableName -> config.getSchema() + "." + tableName).collect(
                        Collectors.joining(",")));
        postgreSQLTask.setServerTimeZone(config.getServerTimeZone());
        postgreSQLTask.setSnapshotMode(config.getScanStartupMode());
        postgreSQLTask.setPrimaryKey(config.getPrimaryKey());

        return postgreSQLTask;
    }

    private static RedisTask getRedisTask(DataConfig dataConfig) {
        RedisTaskConfig config = GSON.fromJson(dataConfig.getExtParams(), RedisTaskConfig.class);
        RedisTask redisTask = new RedisTask();

        redisTask.setAuthUser(config.getUsername());
        redisTask.setAuthPassword(config.getPassword());
        redisTask.setHostname(config.getHostname());
        redisTask.setPort(config.getPort());
        redisTask.setSsl(config.getSsl());
        redisTask.setReadTimeout(config.getTimeout());
        redisTask.setReplId(config.getReplId());
        redisTask.setCommand(config.getCommand());
        redisTask.setDbName(config.getDbName());
        redisTask.setKeys(config.getKeys());
        redisTask.setFieldOrMember(config.getFieldOrMember());
        redisTask.setIsSubscribe(config.getIsSubscribe());
        redisTask.setSyncFreq(config.getSyncFreq());
        redisTask.setSubscriptionOperation(config.getSubscriptionOperation());

        return redisTask;
    }

    private static MongoTask getMongoTask(DataConfig dataConfigs) {

        MongoTaskConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                MongoTaskConfig.class);
        MongoTask mongoTask = new MongoTask();

        mongoTask.setHosts(config.getHosts());
        mongoTask.setUser(config.getUsername());
        mongoTask.setPassword(config.getPassword());
        mongoTask.setDatabaseIncludeList(config.getDatabase());
        mongoTask.setCollectionIncludeList(config.getCollection());
        mongoTask.setSnapshotMode(config.getSnapshotMode());

        MongoTask.Offset offset = new MongoTask.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        mongoTask.setOffset(offset);

        MongoTask.Snapshot snapshot = new MongoTask.Snapshot();
        snapshot.setMode(config.getSnapshotMode());
        mongoTask.setSnapshot(snapshot);

        MongoTask.History history = new MongoTask.History();
        history.setFilename(config.getHistoryFilename());
        mongoTask.setHistory(history);

        return mongoTask;
    }

    private static OracleTask getOracleTask(DataConfig dataConfigs) {
        OracleTaskConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                OracleTaskConfig.class);
        OracleTask oracleTask = new OracleTask();

        oracleTask.setHostname(config.getHostname());
        oracleTask.setPort(config.getPort());
        oracleTask.setUser(config.getUsername());
        oracleTask.setPassword(config.getPassword());
        oracleTask.setSchemaIncludeList(config.getSchemaName());
        oracleTask.setDbname(config.getDatabase());
        oracleTask.setTableIncludeList(config.getTableName());

        OracleTask.Offset offset = new OracleTask.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        oracleTask.setOffset(offset);

        OracleTask.Snapshot snapshot = new OracleTask.Snapshot();
        snapshot.setMode(config.getScanStartupMode());
        oracleTask.setSnapshot(snapshot);

        OracleTask.History history = new OracleTask.History();
        history.setFilename(config.getHistoryFilename());
        oracleTask.setHistory(history);

        return oracleTask;
    }

    private static SqlServerTask getSqlServerTask(DataConfig dataConfigs) {
        SqlserverTaskConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                SqlserverTaskConfig.class);
        SqlServerTask sqlServerTask = new SqlServerTask();
        sqlServerTask.setUser(config.getUsername());
        sqlServerTask.setHostname(config.getHostname());
        sqlServerTask.setPassword(config.getPassword());
        sqlServerTask.setPort(config.getPort());
        sqlServerTask.setServerName(config.getSchemaName());
        sqlServerTask.setDbname(config.getDatabase());
        sqlServerTask.setSchemaName(config.getSchemaName());
        sqlServerTask.setTableName(config.getSchemaName() + "." + config.getTableName());
        sqlServerTask.setServerTimezone(config.getServerTimezone());
        sqlServerTask.setUnixTimestampFormatEnable(config.getUnixTimestampFormatEnable());

        SqlServerTask.Offset offset = new SqlServerTask.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        sqlServerTask.setOffset(offset);

        SqlServerTask.Snapshot snapshot = new SqlServerTask.Snapshot();
        snapshot.setMode(config.getSnapshotMode());
        sqlServerTask.setSnapshot(snapshot);

        SqlServerTask.History history = new SqlServerTask.History();
        history.setFilename(config.getHistoryFilename());
        sqlServerTask.setHistory(history);

        return sqlServerTask;
    }

    public static MqttTask getMqttTask(DataConfig dataConfigs) {
        MqttConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                MqttConfig.class);
        MqttTask mqttTask = new MqttTask();

        mqttTask.setServerURI(config.getServerURI());
        mqttTask.setUserName(config.getUsername());
        mqttTask.setPassword(config.getPassword());
        mqttTask.setTopic(config.getTopic());
        mqttTask.setConnectionTimeOut(config.getConnectionTimeOut());
        mqttTask.setKeepAliveInterval(config.getKeepAliveInterval());
        mqttTask.setQos(config.getQos());
        mqttTask.setCleanSession(config.getCleanSession());
        mqttTask.setClientIdPrefix(config.getClientId());
        mqttTask.setQueueSize(config.getQueueSize());
        mqttTask.setAutomaticReconnect(config.getAutomaticReconnect());
        mqttTask.setMqttVersion(config.getMqttVersion());

        return mqttTask;
    }

    private static Proxy getProxy(DataConfig dataConfigs) {
        Proxy proxy = new Proxy();
        Manager manager = new Manager();
        AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
        manager.setAddr(agentConf.get(AGENT_MANAGER_ADDR));
        proxy.setInlongGroupId(dataConfigs.getInlongGroupId());
        proxy.setInlongStreamId(dataConfigs.getInlongStreamId());
        proxy.setManager(manager);
        if (null != dataConfigs.getSyncSend()) {
            proxy.setSync(dataConfigs.getSyncSend() == SYNC_SEND_OPEN);
        }
        if (null != dataConfigs.getSyncPartitionKey()) {
            proxy.setPartitionKey(dataConfigs.getSyncPartitionKey());
        }
        return proxy;
    }

    /**
     * convert DataConfig to TaskProfile
     */
    public static TaskProfile convertToTaskProfile(DataConfig dataConfig) {
        if (!dataConfig.isValid()) {
            throw new IllegalArgumentException("input dataConfig" + dataConfig + "is invalid please check");
        }

        TaskProfileDto profileDto = new TaskProfileDto();
        Proxy proxy = getProxy(dataConfig);
        profileDto.setProxy(proxy);
        Task task = new Task();

        // common attribute
        task.setId(String.valueOf(dataConfig.getTaskId()));
        task.setTaskType(dataConfig.getTaskType());
        task.setGroupId(dataConfig.getInlongGroupId());
        task.setStreamId(dataConfig.getInlongStreamId());
        task.setChannel(DEFAULT_CHANNEL);
        task.setIp(dataConfig.getIp());
        task.setOp(dataConfig.getOp());
        task.setDeliveryTime(dataConfig.getDeliveryTime());
        task.setUuid(dataConfig.getUuid());
        task.setVersion(dataConfig.getVersion());
        task.setState(dataConfig.getState());
        task.setPredefinedFields(dataConfig.getPredefinedFields());
        task.setCycleUnit(CycleUnitType.REAL_TIME);
        task.setTimeZone(dataConfig.getTimeZone());
        if (dataConfig.getAuditVersion() == null) {
            task.setAuditVersion(DEFAULT_AUDIT_VERSION);
        } else {
            task.setAuditVersion(dataConfig.getAuditVersion());
        }
        // set sink type
        if (dataConfig.getDataReportType() == NORMAL_SEND_TO_DATAPROXY.ordinal()) {
            task.setSink(DEFAULT_DATA_PROXY_SINK);
            task.setProxySend(false);
        } else if (dataConfig.getDataReportType() == 1) {
            task.setSink(DEFAULT_DATA_PROXY_SINK);
            task.setProxySend(true);
        } else {
            String mqType = dataConfig.getMqClusters().get(0).getMqType();
            task.setMqClusters(GSON.toJson(dataConfig.getMqClusters()));
            task.setTopicInfo(GSON.toJson(dataConfig.getTopicInfo()));
            if (mqType.equals(MQType.PULSAR)) {
                task.setSink(PULSAR_SINK);
            } else if (mqType.equals(MQType.KAFKA)) {
                task.setSink(KAFKA_SINK);
            } else {
                throw new IllegalArgumentException("invalid mq type " + mqType + " please check");
            }
        }
        task.setRetry(false);
        TaskTypeEnum taskType = TaskTypeEnum.getTaskType(dataConfig.getTaskType());
        switch (requireNonNull(taskType)) {
            case SQL:
                SQLTask sqlTask = getSQLTask(dataConfig);
                task.setCycleUnit(sqlTask.getCycleUnit());
                task.setSqlTask(sqlTask);
                task.setRetry(sqlTask.getRetry());
                profileDto.setTask(task);
                break;
            case BINLOG:
                BinlogTask binlogTask = getBinlogTask(dataConfig);
                task.setBinlogTask(binlogTask);
                profileDto.setTask(task);
                break;
            case FILE:
                FileTask fileTask = getFileTask(dataConfig);
                task.setCycleUnit(fileTask.getCycleUnit());
                task.setFileTask(fileTask);
                task.setRetry(fileTask.getRetry());
                profileDto.setTask(task);
                break;
            case KAFKA:
                KafkaTask kafkaTask = getKafkaTask(dataConfig);
                task.setKafkaTask(kafkaTask);
                profileDto.setTask(task);
                break;
            case PULSAR:
                PulsarTask pulsarTask = getPulsarTask(dataConfig);
                task.setPulsarTask(pulsarTask);
                profileDto.setTask(task);
                break;
            case POSTGRES:
                PostgreSQLTask postgreSQLTask = getPostgresTask(dataConfig);
                task.setPostgreSQLTask(postgreSQLTask);
                profileDto.setTask(task);
                break;
            case ORACLE:
                OracleTask oracleTask = getOracleTask(dataConfig);
                task.setOracleTask(oracleTask);
                profileDto.setTask(task);
                break;
            case SQLSERVER:
                SqlServerTask sqlserverTask = getSqlServerTask(dataConfig);
                task.setSqlserverTask(sqlserverTask);
                profileDto.setTask(task);
                break;
            case MONGODB:
                MongoTask mongoTask = getMongoTask(dataConfig);
                task.setMongoTask(mongoTask);
                profileDto.setTask(task);
                break;
            case REDIS:
                RedisTask redisTask = getRedisTask(dataConfig);
                task.setRedisTask(redisTask);
                profileDto.setTask(task);
                break;
            case MQTT:
                MqttTask mqttTask = getMqttTask(dataConfig);
                task.setMqttTask(mqttTask);
                profileDto.setTask(task);
                break;
            case MOCK:
                profileDto.setTask(task);
                break;
            case COS:
                COSTask cosTask = getCOSTask(dataConfig);
                task.setCycleUnit(cosTask.getCycleUnit());
                task.setCosTask(cosTask);
                task.setRetry(cosTask.getRetry());
                profileDto.setTask(task);
                break;
            default:
                logger.error("invalid task type {}", taskType);
        }
        return TaskProfile.parseJsonStr(GSON.toJson(profileDto));
    }

    @Data
    public static class Task {

        private String id;
        private String groupId;
        private String streamId;
        private String ip;
        private String source;
        private String sink;
        private String channel;
        private String name;
        private String op;
        private String retryTime;
        private String deliveryTime;
        private String uuid;
        private Integer version;
        private boolean proxySend;
        private String mqClusters;
        private String topicInfo;
        private String taskClass;
        private Integer taskType;
        private String predefinedFields;
        private Integer state;
        private String cycleUnit;
        private String timeZone;
        private String auditVersion;
        private boolean retry;

        private FileTask fileTask;
        private BinlogTask binlogTask;
        private KafkaTask kafkaTask;
        private PulsarTask pulsarTask;
        private PostgreSQLTask postgreSQLTask;
        private OracleTask oracleTask;
        private MongoTask mongoTask;
        private RedisTask redisTask;
        private MqttTask mqttTask;
        private SqlServerTask sqlserverTask;
        private COSTask cosTask;
        private SQLTask sqlTask;
    }

    @Data
    public static class Manager {

        private String addr;
    }

    @Data
    public static class Proxy {

        private String inlongGroupId;
        private String inlongStreamId;
        private Manager manager;
        private Boolean sync;
        private String partitionKey;
    }

}