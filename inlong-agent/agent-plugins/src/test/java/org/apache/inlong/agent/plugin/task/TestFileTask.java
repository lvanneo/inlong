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

package org.apache.inlong.agent.plugin.task;

import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.TaskProfile;
import org.apache.inlong.agent.constant.CycleUnitType;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.task.logcollection.local.FileTask;
import org.apache.inlong.common.enums.TaskStateEnum;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileTask.class)
@PowerMockIgnore({"javax.management.*"})
public class TestFileTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileTask.class);
    private static final ClassLoader LOADER = TestFileTask.class.getClassLoader();
    private static AgentBaseTestsHelper helper;
    private static TaskManager manager;
    private static String resourceParentPath;
    private static final ThreadPoolExecutor EXECUTOR_SERVICE = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            1L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new AgentThreadFactory("TestLogfileCollectTask"));

    @BeforeClass
    public static void setup() throws Exception {
        helper = new AgentBaseTestsHelper(TestFileTask.class.getName()).setupAgentHome();
        resourceParentPath = new File(LOADER.getResource("testScan/temp.txt").getPath()).getParent();
        manager = new TaskManager();
    }

    @AfterClass
    public static void teardown() throws Exception {
        helper.teardownAgentHome();
    }

    @Test
    public void testScan() throws Exception {
        doTest(1, Arrays.asList("testScan/20230928_1/test_1.txt"),
                resourceParentPath + "/YYYYMMDD_[0-9]+/test_[0-9]+.txt", CycleUnitType.DAY, Arrays.asList("20230928"),
                "20230928", "20230930");
        doTest(2, Arrays.asList("testScan/2023092810_1/test_1.txt"),
                resourceParentPath + "/YYYYMMDDHH_[0-9]+/test_[0-9]+.txt",
                CycleUnitType.HOUR, Arrays.asList("2023092810"), "2023092800", "2023093023");
        doTest(3, Arrays.asList("testScan/202309281030_1/test_1.txt", "testScan/202309301059_1/test_1.txt"),
                resourceParentPath + "/YYYYMMDDHHmm_[0-9]+/test_[0-9]+.txt",
                CycleUnitType.MINUTE, Arrays.asList("202309281030", "202309301059"), "202309280000",
                "202309302300");
        doTest(4, Arrays.asList("testScan/20241030/23/59.txt"),
                resourceParentPath + "/YYYYMMDD/HH/mm.txt",
                CycleUnitType.MINUTE, Arrays.asList("202410302359"), "202410300000", "202410310000");
    }

    @Test
    public void testScanLowercase() throws Exception {
        doTest(1, Arrays.asList("testScan/20230928_1/test_1.txt"),
                resourceParentPath + "/yyyyMMdd_[0-9]+/test_[0-9]+.txt", CycleUnitType.DAY, Arrays.asList("20230928"),
                "20230928", "20230930");
        doTest(2, Arrays.asList("testScan/2023092810_1/test_1.txt"),
                resourceParentPath + "/yyyyMMddhh_[0-9]+/test_[0-9]+.txt",
                CycleUnitType.HOUR, Arrays.asList("2023092810"), "2023092800", "2023093023");
        doTest(3, Arrays.asList("testScan/202309281030_1/test_1.txt", "testScan/202309301059_1/test_1.txt"),
                resourceParentPath + "/yyyyMMddhhmm_[0-9]+/test_[0-9]+.txt",
                CycleUnitType.MINUTE, Arrays.asList("202309281030", "202309301059"), "202309280000",
                "202309302300");
        doTest(4, Arrays.asList("testScan/20241030/23/59.txt"),
                resourceParentPath + "/yyyyMMdd/hh/mm.txt",
                CycleUnitType.MINUTE, Arrays.asList("202410302359"), "202410300000", "202410310000");
    }

    private void doTest(int taskId, List<String> resources, String pattern, String cycle, List<String> srcDataTimes,
            String startTime, String endTime)
            throws Exception {
        List<String> resourceName = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            resourceName.add(LOADER.getResource(resources.get(i)).getPath());
        }
        TaskProfile taskProfile =
                helper.getFileTaskProfile(taskId, pattern, "csv", true, startTime, endTime, TaskStateEnum.RUNNING,
                        cycle, "GMT+8:00", null);
        FileTask dayTask = null;
        final List<String> fileName = new ArrayList();
        final List<String> dataTime = new ArrayList();
        try {
            dayTask = PowerMockito.spy(new FileTask());
            PowerMockito.doAnswer(invocation -> {
                fileName.add(invocation.getArgument(0));
                dataTime.add(invocation.getArgument(1));
                return null;
            }).when(dayTask, "addToEvenMap", Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString());
            Assert.assertTrue(dayTask.isProfileValid(taskProfile));
            manager.getTaskStore().storeTask(taskProfile);
            dayTask.init(manager, taskProfile, manager.getInstanceBasicStore());
            EXECUTOR_SERVICE.submit(dayTask);
        } catch (Exception e) {
            LOGGER.error("source init error", e);
            Assert.assertTrue("source init error", false);
        }
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> fileName.size() == resources.size() && dataTime.size() == resources.size());
        for (int i = 0; i < fileName.size(); i++) {
            Assert.assertEquals(0, fileName.get(i).compareTo(resourceName.get(i)));
            Assert.assertEquals(0, dataTime.get(i).compareTo(srcDataTimes.get(i)));
        }
        dayTask.destroy();
    }
}