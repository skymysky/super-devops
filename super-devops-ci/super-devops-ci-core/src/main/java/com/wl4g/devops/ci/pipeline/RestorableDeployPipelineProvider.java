/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
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
package com.wl4g.devops.ci.pipeline;

import com.wl4g.devops.ci.core.context.PipelineContext;
import com.wl4g.devops.common.exception.ci.NotFoundBackupAssetsFileException;
import com.wl4g.devops.support.cli.command.DestroableCommand;
import com.wl4g.devops.support.cli.command.LocalDestroableCommand;

import java.io.File;

import static com.wl4g.devops.ci.utils.PipelineUtils.ensureDirectory;
import static com.wl4g.devops.tool.common.codec.FingerprintUtils.getMd5Fingerprint;

/**
 * Recoverable deployment pipeline provider based on physical backup (local
 * disk/cloud service).
 * 
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年10月12日
 * @since
 */
public abstract class RestorableDeployPipelineProvider extends GenericDependenciesPipelineProvider {

	public RestorableDeployPipelineProvider(PipelineContext context) {
		super(context);
	}

	@Override
	protected void postBuiltModulesDependencies() throws Exception {
		// Source code fingerprint.
		setSourceFingerprint(vcsAdapter.getLatestCommitted(getContext().getProjectSourceDir()));

		// Assets file fingerprint.
		String assetsFilename = config.getAssetsFullFilename(getContext().getProject().getAssetsPath(),
				getContext().getAppCluster().getName());
		File assetsFile = new File(getContext().getProjectSourceDir() + assetsFilename);
		if (assetsFile.exists()) {
			setAssetsFingerprint(getMd5Fingerprint(assetsFile));
		}

		// Handling backup
		handleDiskBackupAssets();

		// Deploying to remote instances.
		startupExecuteRemoteDeploying();
	}

	/**
	 * Roll-back
	 */
	@Override
	public void rollback() throws Exception {
		// Obtain backup assets file.
		File backAssetsFile = new File(config.getWorkspace() + "/" + getContext().getTaskHistory().getRefId() + "/"
				+ config.getTarFileNameWithTar(getContext().getAppCluster().getName()));
		// Check backup assets file.
		if (!backAssetsFile.exists()) {
			throw new NotFoundBackupAssetsFileException(String.format("Not found backup assets file: %s", backAssetsFile));
		}

		// Direct using backup disk.
		rollbackBackupAssets();

		// Deploying to remote instances.
		startupExecuteRemoteDeploying();
	}

	/**
	 * Handling assets backup to disk, The default implements is to copy the
	 * asset files to the local shared disk. </br>
	 * For example, the docker based deployment should be backed up to the
	 * docker server image repository.
	 * 
	 * @throws Exception
	 */
	protected void handleDiskBackupAssets() throws Exception {
		Integer taskHisId = getContext().getTaskHistory().getId();
		String assetsFilename = config.getAssetsFullFilename(getContext().getProject().getAssetsPath(),
				getContext().getAppCluster().getName());
		String tarFileName = config.getTarFileNameWithTar(getContext().getAppCluster().getName());
		String targetPath = getContext().getProjectSourceDir() + assetsFilename;
		String backupPath = config.getJobBackupDir(taskHisId).getAbsolutePath() + "/" + tarFileName;

		// Ensure backup directory.
		ensureDirectory(config.getJobBackupDir(taskHisId).getAbsolutePath());

		// Copy assets files to backup dir.
		String command = String.format("cp -Rf %s %s", targetPath, backupPath);
		File jobLogFile = config.getJobLog(taskHisId);
		// TODO timeoutMs?
		DestroableCommand cmd = new LocalDestroableCommand(String.valueOf(taskHisId), command, null, 300000L)
				.setStdout(jobLogFile).setStderr(jobLogFile);
		pm.execWaitForComplete(cmd);
	}

	/**
	 * Roll-back backup assets files.
	 * 
	 * @throws Exception
	 */
	protected void rollbackBackupAssets() throws Exception {
		Integer taskHisRefId = getContext().getRefTaskHistory().getId();
		String tarFileName = config.getTarFileNameWithTar(getContext().getAppCluster().getName());
		String backupPath = config.getJobBackupDir(taskHisRefId).getAbsolutePath() + tarFileName;
		String assetsFilename = config.getAssetsFullFilename(getContext().getProject().getAssetsPath(),
				getContext().getAppCluster().getName());
		String target = getContext().getProjectSourceDir() + assetsFilename;

		// Copy backup assets to build dir.
		String command = String.format("cp -Rf %s %s", backupPath, target);
		// TODO timeoutMs/jobLogFile?
		File jobLogFile = config.getJobLog(taskHisRefId);
		DestroableCommand cmd = new LocalDestroableCommand(command, null, 300000L).setStdout(jobLogFile).setStderr(jobLogFile);
		pm.execWaitForComplete(cmd);
	}

}