/*
 * Copyright 2017 ~ 2025 the original author or authors.
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
package com.wl4g.devops.scm.client.enviroment;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple plain text serializable encapsulation of a named source of key-value
 * pairs. Basically a DTO for {@link ScmPropertySource}, but also applicable
 * outside the domain of a Spring application.
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年6月2日
 * @since
 */
public class ScmPropertySource {

	private String name;

	private Map<?, ?> source;

	@JsonCreator
	public ScmPropertySource(@JsonProperty("name") String name, @JsonProperty("source") Map<?, ?> source) {
		this.name = name;
		this.source = source;
	}

	public String getName() {
		return this.name;
	}

	public Map<?, ?> getSource() {
		return this.source;
	}

	@Override
	public String toString() {
		return "PropertySource [name=" + this.name + "]";
	}

}