/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.util.exec;

import org.gradle.util.shutdown.ShutdownHookAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminates the external running 'sub' process when the Gradle process is being cancelled.
 *
 * @author Tom Eyckmans
 */
public class ExecHandleShutdownHookAction implements ShutdownHookAction {

    private static final Logger logger = LoggerFactory.getLogger(ExecHandleShutdownHookAction.class);
    private final ExecHandle execHandle;

    public static ExecHandleShutdownHookAction forHandle(ExecHandle execHandle) 
    {
        return new ExecHandleShutdownHookAction(execHandle);
    }

    private ExecHandleShutdownHookAction(ExecHandle execHandle) {
        if ( execHandle == null ) throw new IllegalArgumentException("execHandle is null!");

        this.execHandle = execHandle;
    }

    public void execute() {
        try {
            execHandle.abort();
        }
        catch ( Throwable t ) {
            logger.error("failed to abort " + execHandle.getCommand(), t);
        }
    }
}
