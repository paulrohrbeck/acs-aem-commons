/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
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
 * #L%
 */

package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.AgentFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replication Agent Filter used to identify Flush agents.
 */
public class DispatcherFlushFilter implements AgentFilter {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlushFilter.class);

    /**
     * All: All Enablied Dispatcher Flush Agents.
     * Hierarchical: "Normal" flush invalidation that effects entire content hierarchies.
     * ResourceOnly: Targets agents with a CQ-Scope-Action of "Resource Only" defined.
     */
    public static enum FlushType {
        All,
        Hierarchical,
        ResourceOnly
    }

    public static final String SERIALIZATION_TYPE = "flush";
    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String CQ_ACTION_HEADER = "CQ-Action:";
    public static final String CQ_SCOPE_ACTION_HEADER = "CQ-Scope-Action: ResourceOnly";

    private final FlushType flushType;

    /**
     * Default constructor; Same as: new DispatcherFlushFilter(FlushType.All);.
     */
    public DispatcherFlushFilter() {
        this.flushType = FlushType.All;
    }

    /**
     * Targets a set of Dispatcher Flush agents based on the parameter flushType.
     *
     * @param flushType The type of Flush agents this Agent should target
     */
    public DispatcherFlushFilter(final FlushType flushType) {
        this.flushType = flushType;
    }

    /**
     * Checks if the @agent is considered an active Flush agent (Serialization Type ~> Flush and is enabled).
     *
     * @param agent the agent to test test
     * @return true is is considered an enabled Flush agent
     */
    @Override
    public final boolean isIncluded(final Agent agent) {
        if (FlushType.All.equals(this.flushType)) {
            return this.isIncludedCommon(agent);
        } else if (FlushType.Hierarchical.equals(this.flushType)) {
            return this.isIncludedHierarchical(agent);
        } else if (FlushType.ResourceOnly.equals(this.flushType)) {
            return this.isIncludedResourceOnly(agent);
        }

        return false;
    }

    /**
     * Returns the Dispatcher FlushType this filter was created with.
     * @return this filter's dispatcher flushType
     */
    public final FlushType getFlushType() {
        return this.flushType;
    }

    private boolean isIncludedCommon(final Agent agent) {
        return this.isFlushingAgent(agent)
                && this.isDispatcherTransportURI(agent)
                && this.isDispatcherHeaders(agent)
                && this.isEnabled(agent);
    }

    private boolean isIncludedHierarchical(final Agent agent) {
        return this.isIncludedCommon(agent)
                && !this.isResourceOnly(agent);
    }

    private boolean isIncludedResourceOnly(final Agent agent) {
        return this.isIncludedCommon(agent)
                && this.isResourceOnly(agent);
    }

    /**
     * Checks if the agent is enabled.
     *
     * @param agent Agent to check
     * @return true if the agent is enabled
     */
    private boolean isEnabled(final Agent agent) {
        return agent.isEnabled();
    }

    /**
     * Checks if the agent has a "flush" serialization type.
     *
     * @param agent Agent to check
     * @return true if the Agent's serialization type is "flush"
     */
    private boolean isFlushingAgent(final Agent agent) {
        return StringUtils.equals(SERIALIZATION_TYPE, agent.getConfiguration().getSerializationType());
    }

    /**
     * Checks if the agent has a valid transport URI set.
     *
     * @param agent Agent to check
     * @return true if the Agent's transport URI is in the proper form
     */
    private boolean isDispatcherTransportURI(final Agent agent) {
        final String transportURI = agent.getConfiguration().getTransportURI();

        return (StringUtils.startsWith(transportURI, HTTP)
                || StringUtils.startsWith(transportURI, HTTPS));
    }

    /**
     * Checks if the agent has a valid dispatcher headers.
     *
     * @param agent Agent to check
     * @return true if the Agent's headers contain the proper values
     */
    private boolean isDispatcherHeaders(final Agent agent) {
        final ValueMap properties = agent.getConfiguration().getProperties();
        final String[] headers =  properties.get(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[]{});

        for (final String header : headers) {
            if (StringUtils.startsWith(header, CQ_ACTION_HEADER)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the agent has valid CQ-Scope-Action: ResourceOnly header.
     *
     * @param agent Agent to check
     * @return true if the Agent's headers contain the expected values
     */
    private boolean isResourceOnly(final Agent agent) {
        final ValueMap properties = agent.getConfiguration().getProperties();
        final String[] headers =  properties.get(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[]{});

        for (final String header : headers) {
            if (StringUtils.equals(header, CQ_SCOPE_ACTION_HEADER)) {
                return true;
            }
        }

        return false;
    }
}
