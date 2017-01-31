/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.ide.vscode.commons.cloudfoundry.client.CFBuildpack;
import org.springframework.ide.vscode.commons.cloudfoundry.client.CFServiceInstance;
import org.springframework.ide.vscode.commons.cloudfoundry.client.ClientRequests;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 
 * Wrapper around a {@link ClientRequests} that may contain cached information
 * like buildpacks
 *
 */
public class CFTarget {

	private final CFClientParams params;
	private final ClientRequests requests;
	private final String targetName;

	/*
	 * Cached information
	 */
	private final LoadingCache<String, List<CFBuildpack>> buildpacksCache;
	private final LoadingCache<String, List<CFServiceInstance>> servicesCache;
	private Throwable lastCFFailure;

	public CFTarget(String targetName, CFClientParams params, ClientRequests requests) {
		this.params = params;
		this.requests = requests;
		this.targetName = targetName;
		CacheLoader<String, List<CFServiceInstance>> servicesLoader = new CacheLoader<String, List<CFServiceInstance>>() {

			@Override
			public List<CFServiceInstance> load(String key) throws Exception {
				/* Cache of services does not use keys, as the whole cache
				 * gets wiped clean on any new call to CF.
				 */
				return runAndCheckForFailure(() -> requests.getServices());
			}
		};
		this.servicesCache = CacheBuilder.newBuilder()
				.expireAfterAccess(CFTargetCache.SERVICES_EXPIRATION, TimeUnit.SECONDS).build(servicesLoader);

		CacheLoader<String, List<CFBuildpack>> buildpacksLoader = new CacheLoader<String, List<CFBuildpack>>() {

			@Override
			public List<CFBuildpack> load(String key) throws Exception {
				/* Cache does not use keys, as the whole cache
				 * gets wiped clean on any new call to CF.
				 */
				return runAndCheckForFailure(() -> requests.getBuildpacks());
			}
		};
		this.buildpacksCache = CacheBuilder.newBuilder()
				.expireAfterAccess(CFTargetCache.TARGET_EXPIRATION, TimeUnit.HOURS).build(buildpacksLoader);
	}
	
	protected <T> T runAndCheckForFailure(Callable<T> callable) throws Exception {
		this.lastCFFailure = null;
		try {
			return callable.call();
		} catch (Exception e) {
			if (isAcceptableCFFailure(e)) {
				this.lastCFFailure = e;
			}
			throw e;
		}
	}
	
	public boolean hasCFFailure() {
		return this.lastCFFailure != null;
	}
	
	protected boolean isAcceptableCFFailure(Throwable e) {
		// TODO: this is too broad. Be more specific: e.g. look for IOException, etc..
		return e != null;
	}

	public CFClientParams getParams() {
		return params;
	}

	public List<CFBuildpack> getBuildpacks() throws Exception {
		// Use the target name as the "key" , since Guava cache doesn't allow null keys
		// However, the key is not really used when fetching buildpacks, as we are not caching
		// buildpacks per target here. This class only represents ONE target, so it will only
		// ever have one key
		String key = getName();
		return this.buildpacksCache.get(key);
	}

	public List<CFServiceInstance> getServices() throws Exception {
		/* services don't use keys, as they get wiped clean on each refresh
		 * . That said, the cache doesn't allow a null key, so use the target name as the "key"
		 * 
		 */
		String key = getName();
		return this.servicesCache.get(key);
	}

	public ClientRequests getClientRequests() {
		return requests;
	}

	public String getName() {
		return this.targetName;
	}

	@Override
	public String toString() {
		return "CFClientTarget [params=" + params + ", targetName=" + targetName + "]";
	}
}
