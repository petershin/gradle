/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.workers.internal;

import org.gradle.api.internal.AsmBackedClassGenerator;
import org.gradle.api.internal.DefaultInstantiatorFactory;
import org.gradle.api.internal.InstantiatorFactory;
import org.gradle.cache.internal.CrossBuildInMemoryCacheFactory;
import org.gradle.internal.event.DefaultListenerManager;

import javax.inject.Inject;

public class WorkerDaemonServer extends DefaultWorkerServer {
    private static final InstantiatorFactory INSTANTIATOR_FACTORY = new DefaultInstantiatorFactory(new AsmBackedClassGenerator(), new CrossBuildInMemoryCacheFactory(new DefaultListenerManager()));

    @Inject
    WorkerDaemonServer() {
        super(INSTANTIATOR_FACTORY.inject());
    }

    @Override
    public DefaultWorkResult execute(ActionExecutionSpec spec) {
        try {
            return super.execute(spec);
        } catch (Throwable t) {
            return new DefaultWorkResult(true, t);
        }
    }

    @Override
    public String toString() {
        return "WorkerDaemonServer{}";
    }
}
