/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.integtests.fixtures.problems

import groovy.transform.CompileStatic
import org.gradle.api.problems.AdditionalData
import org.gradle.api.problems.FileLocation
import org.gradle.api.problems.LineInFileLocation
import org.gradle.api.problems.OffsetInFileLocation
import org.gradle.api.problems.ProblemDefinition
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemLocation
import org.gradle.api.problems.Severity
import org.gradle.api.problems.internal.InternalDocLink
import org.gradle.api.problems.internal.InternalProblem
import org.gradle.api.problems.internal.InternalProblemBuilder
import org.gradle.api.problems.internal.PluginIdLocation
import org.gradle.api.problems.internal.ProblemsInfrastructure
import org.gradle.api.problems.internal.StackTraceLocation
import org.gradle.api.problems.internal.TaskLocation

/*
 * A deserialized representation of a problem received from the build operation trace.
 */
@CompileStatic
class ReceivedProblem implements InternalProblem {
    private final long operationId
    private final ReceivedProblemDefinition definition
    private final String contextualLabel
    private final String details
    private final List<String> solutions
    private final List<ProblemLocation> originLocations
    private final List<ProblemLocation> contextualLocations
    private final ReceivedAdditionalData additionalData
    private final ReceivedException exception

    ReceivedProblem(long operationId, Map<String, Object> problemDetails) {
        this.operationId = operationId
        this.definition = new ReceivedProblemDefinition(problemDetails['definition'] as Map<String, Object>)
        this.contextualLabel = problemDetails['contextualLabel'] as String
        this.details =  problemDetails['details'] as String
        this.solutions = problemDetails['solutions'] as List<String>
        this.originLocations = fromList(problemDetails['originLocations'] as List<Object>)
        this.contextualLocations = fromList(problemDetails['contextualLocations'] as List<Object>)
        this.additionalData = new ReceivedAdditionalData(problemDetails['additionalData'] as Map<String, Object>)
        this.exception = problemDetails['exception'] == null ? null : new ReceivedException(problemDetails['exception'] as Map<String, Object>)
    }

    private static List<ProblemLocation> fromList(List<Object> locations) {
        List<ProblemLocation> result = []
        locations.each { location ->
            result += fromLocation(location)
        }
        result
    }

    private static ProblemLocation fromLocation(location) {
        if (location['pluginId'] != null) {
            return new ReceivedPluginIdLocation(location as Map<String, Object>)
        } else if (location['line'] != null) {
            return new ReceivedLineInFileLocation(location as Map<String, Object>)
        } else if (location['offset'] != null) {
            return new ReceivedOffsetInFileLocation(location as Map<String, Object>)
        } else if (location['path'] != null) {
            return new ReceivedFileLocation(location as Map<String, Object>)
        } else if (location['buildTreePath'] != null) {
            return new ReceivedTaskLocation(location as Map<String, Object>)
        } else if (location['stackTrace'] != null) {
            return new ReceivedStackTraceLocation(location as Map<String, Object>)
        } else {
            return new ReceivedFileLocation(location as Map<String, Object>)
        }
    }

    long getOperationId() {
        operationId
    }

    <T> T oneLocation(Class<T> type) {
        def result = allLocations(type)
        assert result.size() == 1
        result.first()
    }

    <T> List<T> allLocations(Class<T> type) {
        allLocations.findAll { type.isInstance(it) } as List<T>
    }

    private List<?> getAllLocations() {
        getOriginLocations() + getContextualLocations()
    }

    @Override
    ReceivedProblemDefinition getDefinition() {
        definition
    }

    Severity getSeverity() {
        definition.severity
    }

    // The content of the problem definition is tested in `KnownProblemIds`; in the integration tests we only want to verify if we receive a problem with the expected identifier.
    String getFqid() {
        definition.id.fqid
    }

    @Override
    String getContextualLabel() {
       contextualLabel
    }

    @Override
    String getDetails() {
       details
    }

    @Override
    List<String> getSolutions() {
        solutions
    }

    @Override
    List<ProblemLocation> getOriginLocations() {
        originLocations
    }

    @Override
    List<ProblemLocation> getContextualLocations() {
        contextualLocations
    }

    <T extends ProblemLocation> T getSingleOriginLocation(Class<T> locationType) {
        return getSingleLocation(locationType, originLocations)
    }

    <T extends ProblemLocation> T getSingleContextualLocation(Class<T> locationType) {
        return getSingleLocation(locationType, contextualLocations)
    }

    private static <T extends ProblemLocation> T getSingleLocation(Class<T> locationType, List<ProblemLocation> locations) {
        def location = locations.find {
            locationType.isInstance(it)
        }
        assert location != null : "Expected a location of type $locationType, but found none. Available locations: ${locations.collect { it.getClass().name }}"
        return locationType.cast(location)
    }

    @Override
    ReceivedAdditionalData getAdditionalData() {
       additionalData
    }


    @Override
    ReceivedException getException() {
        exception
    }

    @Override
    String toString() {
        String originLocationsStr = originLocations.collect { formatLocation(it) }.join(", ")
        String contextualLocationsStr = contextualLocations.collect { formatLocation(it) }.join(", ")
        String solutionsStr = solutions.collect { "'${it}'" }.join(", ")

        return "ReceivedProblem{" +
            "id=${definition.id.fqid}" +
            ", severity=${severity}" +
            ", label='${contextualLabel}'" +
            ", details='${details}'" +
            ", originLocations=[${originLocationsStr}]" +
            ", contextualLocations=[${contextualLocationsStr}]" +
            ", solutions=[${solutionsStr}]" +
            "}"
    }

    private String formatLocation(ProblemLocation location) {
        if (location instanceof FileLocation) {
            String result = "File(" + ((FileLocation) location).path
            if (location instanceof LineInFileLocation) {
                LineInFileLocation lineLocation = (LineInFileLocation) location
                result += ", line=" + lineLocation.line + ", column=" + lineLocation.column
            } else if (location instanceof OffsetInFileLocation) {
                OffsetInFileLocation offsetLocation = (OffsetInFileLocation) location
                result += ", offset=" + offsetLocation.offset
            }
            result += ")"
            return result
        } else if (location instanceof PluginIdLocation) {
            return "Plugin(" + ((PluginIdLocation) location).pluginId + ")"
        } else if (location instanceof TaskLocation) {
            return "Task(" + ((TaskLocation) location).buildTreePath + ")"
        } else if (location instanceof StackTraceLocation) {
            StackTraceLocation stackLocation = (StackTraceLocation) location
            return "StackTrace(elements=" + stackLocation.stackTrace.size() + ")"
        } else {
            return location.toString()
        }
    }

    @Override
    InternalProblemBuilder toBuilder(ProblemsInfrastructure infrastructure) {
        throw new UnsupportedOperationException("Not implemented")
    }

    static class ReceivedProblemDefinition implements ProblemDefinition {
        private final ReceivedProblemId id
        private final Severity severity
        private final ReceivedDocumentationLink documentationLink

        ReceivedProblemDefinition(Map<String, Object> definition) {
            id = new ReceivedProblemId(definition['id'] as Map<String, Object>)
            severity = Severity.valueOf(definition['severity'] as String)
            documentationLink = definition['documentationLink'] ==  null ? null : new ReceivedDocumentationLink(definition['documentationLink'] as Map<String, Object>)
        }

        @Override
        ReceivedProblemId getId() {
            id
        }

        @Override
        Severity getSeverity() {
            severity
        }

        @Override
        ReceivedDocumentationLink getDocumentationLink() {
            documentationLink
        }
    }

    static class ReceivedProblemId extends ProblemId {
        private final String name
        private final String displayName
        private final ReceivedProblemGroup group
        private final String fqid

        ReceivedProblemId(Map<String, Object> id) {
            name = id['name'] as String
            displayName = id['displayName'] as String
            group = new ReceivedProblemGroup(id['group'] as Map<String, Object>)
            fqid = fqid(id)
        }

        private static String fqid(Map<String, Object> id) {
            String result = id['name']
            def parent = id['group']
            while (parent != null) {
                result = "${parent['name']}:$result"
                parent = parent['parent']
            }
            result
        }

        String getFqid() {
            fqid
        }

        @Override
        String getName() {
           name
        }

        @Override
        String getDisplayName() {
            displayName
        }

        @Override
        ReceivedProblemGroup getGroup() {
            group
        }
    }

    static class ReceivedProblemGroup extends ProblemGroup {
        private final String name
        private final String displayName
        private final ReceivedProblemGroup parent

        ReceivedProblemGroup(Map<String, Object> group) {
            name = group['name'] as String
            displayName = group['displayName'] as String
            parent = group['parent'] ? new ReceivedProblemGroup(group['parent'] as Map<String, Object>) : null
        }

        @Override
        String getName() {
            name
        }

        @Override
        String getDisplayName() {
            displayName
        }

        @Override
        ReceivedProblemGroup getParent() {
            parent
        }
    }

    static class ReceivedDocumentationLink implements InternalDocLink {
        private final String url
        private final String consultDocumentationMessage

        ReceivedDocumentationLink(Map<String, Object> documentationLink) {
            url = documentationLink['url'] as String
            consultDocumentationMessage = documentationLink['consultDocumentationMessage'] as String
        }

        @Override
        String getUrl() {
            url
        }

        @Override
        String getConsultDocumentationMessage() {
            consultDocumentationMessage
        }
    }

    static class ReceivedException extends RuntimeException {
        private final String message
        private final String stacktrace

        ReceivedException(Map<String, Object> exception) {
            message = exception['message'] as String
            stacktrace = exception['stackTrace'] as String
        }

        String getStacktrace() {
            stacktrace
        }

        @Override
        String getMessage() {
            message
        }
    }

    static class ReceivedFileLocation implements FileLocation {
        private final String path

        ReceivedFileLocation(Map<String, Object> location) {
            path = location['path'] as String
        }

        @Override
        String getPath() {
            path
        }
    }

    static class ReceivedLineInFileLocation extends ReceivedFileLocation implements LineInFileLocation {
        private final int line
        private final int column
        private final int length

        ReceivedLineInFileLocation(Map<String, Object> location) {
            super(location)
            line = location['line'] as int
            column = location['column'] as int
            length = location['length'] as int
        }

        @Override
        int getLine() {
            line
        }

        @Override
        int getColumn() {
            column
        }

        @Override
        int getLength() {
            length
        }
    }

    static class ReceivedOffsetInFileLocation extends ReceivedFileLocation implements OffsetInFileLocation {
        private final int offset
        private final int length

        ReceivedOffsetInFileLocation(Map<String, Object> location) {
            super(location)
            offset = location['offset'] as int
            length = location['length'] as int
        }

        @Override
        int getOffset() {
            offset
        }

        @Override
        int getLength() {
            length
        }
    }

    static class ReceivedStackTraceLocation implements StackTraceLocation {
        final List<StackTraceElement> stackTrace
        final FileLocation fileLocation

        ReceivedStackTraceLocation(Map<String, Object> location) {
            def fileLocationJson = location['fileLocation']
            this.fileLocation = fileLocationJson == null ? null : fromLocation(fileLocationJson) as FileLocation
            this.stackTrace = location.stackTrace.collect {
                new StackTraceElement(it['className'] as String, it['methodName'] as String, it['fileName'] as String, it['lineNumber'] as int)
            }
        }
    }

    static class ReceivedPluginIdLocation implements PluginIdLocation {
        private final String pluginId

        ReceivedPluginIdLocation(Map<String, Object> location) {
            this.pluginId = location['pluginId'] as String
        }

        @Override
        String getPluginId() {
            pluginId
        }
    }

    static class ReceivedTaskLocation implements TaskLocation {
        private final String buildTreePath

        ReceivedTaskLocation(Map<String, Object> location) {
            this.buildTreePath = location['buildTreePath'] as String
        }

        @Override
        String getBuildTreePath() {
            buildTreePath
        }
    }

    static class ReceivedAdditionalData implements AdditionalData {
        private final Map<String, Object> data

        ReceivedAdditionalData(Map<String, Object> data) {
            if (data == null) {
                this.data = [:]
            } else {
                def d = data.findAll { k, v -> v != null }
                // GeneralData already contains asMap property; it is removed for clarity
                if (d['asMap'] instanceof Map) {
                    this.data = d['asMap'] as Map<String, Object>
                } else {
                    this.data = d
                }
            }
        }

        Map<String, Object> getAsMap() {
            data
        }

        boolean containsAll(Map<String, Object> properties) {
            data.entrySet().containsAll(properties.entrySet())
        }
    }
}
