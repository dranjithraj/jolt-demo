package com.bazaarvoice.jolt;

import java.util.Properties;

public class GitConfigurations {
	public static final String GIT_BASE_REPO_PATH = "/.git";
	
	private final String apiUrl;
	private final String apiKey;
	private final String baseBranchOwner;
	private final String sshkeyPassphrase;
	private final String baseBranchName;
	private final String gitPushBaseRepoBranch;
	private final String localRepoPath;
	private final String specFolder;
	
	public String getApiUrl() {
		return apiUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBaseBranchOwner() {
		return baseBranchOwner;
	}

	public String getSshkeyPassphrase() {
		return sshkeyPassphrase;
	}

	public String getBaseBranchName() {
		return baseBranchName;
	}

	public String getGitPushBaseRepoBranch() {
		return gitPushBaseRepoBranch;
	}

	public String getLocalRepoPath() {
		return localRepoPath;
	}

	public String getSpecFolder() {
		return specFolder;
	}

	public GitConfigurations(Properties gitProperties) {
		this.apiUrl = gitProperties.getProperty("git.api.base.repo.url");
		this.apiKey = gitProperties.getProperty("git.apikey");
		this.baseBranchOwner =  gitProperties.getProperty("git.branch.owner");
		this.sshkeyPassphrase = gitProperties.getProperty("git.sshkey.passphrase");
		this.baseBranchName = gitProperties.getProperty("git.base.repo.remote.name");
		this.gitPushBaseRepoBranch = gitProperties.getProperty("git.push.base.repo.branch");
		this.localRepoPath = gitProperties.getProperty("git.local.repo.path");
		this.specFolder = gitProperties.getProperty("spec.folder");
	}
	
}
