package com.bazaarvoice.jolt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.StashCreateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;

import com.bazaarvoice.jolt.helper.GitConstants;
import com.bazaarvoice.jolt.helper.GitService;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * The Servlet implementation class PullRequest This Servlet is doing IO and
 * Github command that may take little time to respond for post request Caution
 * : This IO operation may End up failing when concurrent requests hits the this
 * Servlet Post request. In Future Async Request Queuing would give proper
 * result.
 */
@WebServlet("/pullrequest")
public class PullRequest extends HttpServlet {
	private final Logger logger = Logger.getLogger(getClass().getName());
	private static final long serialVersionUID = 1L;
	
	private static final String REFS_HEADS = "refs/heads/";
	
	private GitConfigurations gitConfigurations;
	private GitService gitService;
	/**
	 * @throws IOException
	 * @see HttpServlet#HttpServlet()
	 */
	public PullRequest() {
		super();
				
	}

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
		try {
			loadProperties();
		} catch (IOException e) {
			logger.warning("Servlet init failed" + e.getMessage());
			e.printStackTrace();
		}
		initSession();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		String inputString, specString, modelName, branchName;
		try {
			inputString = req.getParameter("input");

			specString = req.getParameter("spec");
			modelName = req.getParameter("modelName");

			branchName = req.getParameter("branchName");

		} catch (Exception e) {
			response.getWriter().println("Could not url-decode the inputs.\n");
			return;
		}
		Object input, spec;

		try {
			input = JsonUtils.jsonToObject(inputString);
		} catch (Exception e) {
			response.getWriter().println("Could not parse the 'input' JSON.\n");
			return;
		}

		try {
			spec = JsonUtils.jsonToObject(specString);
		} catch (Exception e) {
			response.getWriter().println("Could not parse the 'spec' JSON.\n");
			return;
		}

		String result;
		try {
			result = this.raisePR(branchName, GitConstants.GIT_REPOSITORY_NAME, JsonUtils.toPrettyJsonString(input),
					JsonUtils.toPrettyJsonString(spec), modelName);
		} catch (Exception e) {
			result = "Exception : " + e.getMessage();
			e.printStackTrace();
		}		
		response.getWriter().println(result);
	}

	private Repository findRepositoryByName(String repositoryName) throws IOException, URISyntaxException {

		return this.gitService.getHostedRepository("");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String modelName = req.getParameter("modelName");
		String specFileName = "configs/" + modelName + "_config.json";
		String inputFileName = "payloads/" + modelName + "_payload.json";
		String filePath = gitConfigurations.getLocalRepoPath() + "/" + "resources/freshservice/";
		File specFile = new File(filePath + specFileName);
		File inputFile = new File(filePath + inputFileName);

		PrintWriter out = resp.getWriter();
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		System.out.println(inputFile);
		out.print(String.format("{\"file_exisit\" : %b}", specFile.exists() || inputFile.exists()));
		out.close();
	}

	protected String raisePR(String branchName, String repoName, String input, String spec, String modelName) throws Exception {
		//
		Repository r = findRepositoryByName(repoName);
		Git git = Git.wrap(r);
		// Git checkout master
		CheckoutCommand checkoutMasterCommand = git.checkout();
		checkoutMasterCommand.setName(gitConfigurations.getBaseBranchName());
		
		try {
			String specFileName = "configs/" + modelName + "_config.json";
			String inputFileName = "payloads/" + modelName + "_payload.json";

			

			if (git.branchList().call().stream().anyMatch(ref -> ref.getName().equals(REFS_HEADS + branchName))) {
				return "Branch name Already exist";
			}

			// Git Reset
			StashCreateCommand stashCommand = git.stashCreate();
			
			// Git Pull
			PullCommand pullCommand = git.pull();

			// Create Branch
			CreateBranchCommand createBranchCommand = git.branchCreate();
			createBranchCommand.setName(branchName);

			// Git checkout
			CheckoutCommand checkoutCommand = git.checkout();
			checkoutCommand.setName(branchName);

			// File write
			Callable<Boolean> fileWrite = () -> {
				String filePath = gitConfigurations.getLocalRepoPath()+ "/" + "resources/"
						+ gitConfigurations.getSpecFolder();
				fileWrite(filePath, inputFileName, input);
				fileWrite(filePath, specFileName, spec);
				return true;
			};

			// Git add
			AddCommand addCommand = git.add();
			addCommand.addFilepattern(".");

			// Git commit
			CommitCommand commitCommand = git.commit();
			commitCommand.setMessage("Onboard " + modelName);

			// Git Push
			PushCommand pushCommand = git.push();
			pushCommand.getPushDefault();
			pushCommand.setRemote(gitConfigurations.getBaseBranchName());

			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(stashCommand).get();
			logger.info("Stash command submitted");
			executor.submit(checkoutMasterCommand).get();
			logger.info("Checkout command submitted");
			executor.submit(pullCommand).get();
			logger.info("Pull command submitted");

			executor.submit(createBranchCommand).get();
			logger.info("Create command submitted");
			executor.submit(checkoutCommand).get();
			logger.info("Checkout command submitted");

			executor.submit(fileWrite).get();
			logger.info("Spec generation and Payload create File Write task submitted");

			executor.submit(addCommand).get();
			logger.info("Git Add command submitted");

			executor.submit(commitCommand).get();
			logger.info("Git Commit command submitted");

			executor.submit(pushCommand).get();
			logger.info("Git Push command submitted");			
			
			// Raise Pull request through API
			String response = executor.submit(raisePullrequest(branchName)).get();
			logger.info("Raise pull request command submitted");
					
			return response;
		} catch (InterruptedException | ExecutionException e) {
			throw e;
		} catch (GitAPIException e) {						
			throw e;
		} 
		//TODO : Clear the newly created branch;
	}

	private Callable<String> raisePullrequest(String branchName) throws RuntimeException {

		Callable<String> raisePull = () -> {
			StringBuilder strBuf = new StringBuilder();

			HttpURLConnection conn = null;
			BufferedReader reader = null;
			try {
				// Declare the connection to weather api url
				URL url = new URL(gitConfigurations.getApiUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Accept", "application/vnd.github+json");
				conn.setRequestProperty("Authorization", "Bearer " + gitConfigurations.getApiKey());
				conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
				String title = "New Feature onboard";
				String body = "<Model> Onboarding !";
				String postContent = String.format(
						"{\"title\":\"%s\",\"body\":\" %s\",\"head\":\"%s:%s\",\"base\":\"%s\"}", title, body,
						gitConfigurations.getBaseBranchOwner(), branchName, gitConfigurations.getBaseBranchName());
				logger.info(postContent);
				byte[] postData = postContent.getBytes(StandardCharsets.UTF_8);
				try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
					wr.write(postData);
				}

				if (conn.getResponseCode() != 201) {
					throw new RuntimeException("HTTP POST Request Failed with Error code : " + conn.getResponseCode());
				}
				// Read the content from the defined connection
				// Using IO Stream with Buffer raise highly the efficiency of IO
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
				String output = null;
				while ((output = reader.readLine()) != null)
					strBuf.append(output);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (conn != null) {
					conn.disconnect();
				}
			}

			return strBuf.toString();
		};
		return raisePull;
	}

	private boolean fileWrite(String path, String fileName, String content) {
		try {
			FileWriter myWriter = new FileWriter(path + "/" + fileName);
			myWriter.write(content);
			myWriter.close();
		} catch (IOException e) {
			logger.warning("An error occurred.");
			e.printStackTrace();
		}
		return false;
	}

	private void initSession() {
		this.gitService = new GitService(gitConfigurations.getLocalRepoPath() + GitConfigurations.GIT_BASE_REPO_PATH );
		try {
			gitService.getHostedRepository("");
		} catch (IOException e) {
			e.printStackTrace();
		}
		JschConfigSessionFactory sessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure(OpenSshConfig.Host hc, Session session) {
				CredentialsProvider provider = new CredentialsProvider() {
					@Override
					public boolean isInteractive() {
						return false;
					}

					@Override
					public boolean supports(CredentialItem... items) {
						return true;
					}

					@Override
					public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
						for (CredentialItem item : items) {
							((CredentialItem.StringType) item).setValue(gitConfigurations.getSshkeyPassphrase());
						}
						return true;
					}
				};
				UserInfo userInfo = new GitUserInfo(gitConfigurations.getSshkeyPassphrase());
				session.setUserInfo(userInfo);
				session.setConfig("StrictHostKeyChecking", "no");

			}
		};
		SshSessionFactory.setInstance(sessionFactory);		
	}
	private void loadProperties() throws IOException {
		InputStream is = getClass().getResourceAsStream("git.properties");
		Properties properties = new Properties();
		properties.load(is);
		gitConfigurations = new GitConfigurations(properties);
	}
}

class GitUserInfo implements UserInfo {
	String password = null;
	String passPhrase = null;

	GitUserInfo(String passPhrase) {
		this.passPhrase = passPhrase;
	}

	@Override
	public String getPassphrase() {
		return passPhrase;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String passwd) {
		password = passwd;
	}

	@Override
	public boolean promptPassphrase(String message) {
		return true;
	}

	@Override
	public boolean promptPassword(String message) {
		return true;
	}

	@Override
	public boolean promptYesNo(String message) {
		return true;
	}

	@Override
	public void showMessage(String message) {
		// TODO Auto-generated method stub

	}
}
