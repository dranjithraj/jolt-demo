package com.bazaarvoice.jolt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Servlet implementation class PullRequest
 */
@WebServlet("/pullrequest")
public class PullRequest extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private GitService gitService;

	/**
	 * @throws IOException
	 * @see HttpServlet#HttpServlet()
	 */
	public PullRequest() {
		super();
		// TODO : Move to singleton
		this.gitService = new GitService(ApplicationProperties.GIT_BASE_REPO_PATH);
		try {
			gitService.getHostedRepository("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
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

		String result = this.raisePR(branchName, GitConstants.GIT_REPOSITORY_NAME, JsonUtils.toPrettyJsonString(input),
				JsonUtils.toPrettyJsonString(spec), modelName);
		
		// Add PR details in the response
		response.getWriter().println(result);
	}

	private Repository findRepositoryByName(String repositoryName) throws IOException, URISyntaxException {

		return this.gitService.getHostedRepository("");
	}
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {		
		String modelName = req.getParameter("modelName");
		String specFileName = "configs/" + modelName + "_config.json";
		String inputFileName = "payloads/"+ modelName + "_payload.json";
		String filePath = ApplicationProperties.BASE_REPO_PATH + "/" + "resources/freshservice/";
		File specFile = new File(filePath + specFileName);
		File inputFile = new File(filePath + inputFileName);
		
		PrintWriter out = resp.getWriter();
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		System.out.println(inputFile);
		out.print(String.format("{\"file_exisit\" : %b}",specFile.exists() || inputFile.exists()));
		out.close();
	}

	protected String raisePR(String branchName, String repoName, String input, String spec, String modelName) {
		//
		try {
			String specFileName = "configs/" + modelName + "_config.json";
			String inputFileName = "payloads/"+ modelName + "_payload.json";
			
			File f = new File(specFileName);
			if(f.exists() && !f.isDirectory()) { 
			   
			}
			
			Repository r = findRepositoryByName(repoName);
			Git git = Git.wrap(r);
			if (git.branchList().call().stream().anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName))) {
				return "Branch name Already exist";
			}
			//Git Reset
			StashCreateCommand stashCommand = git.stashCreate();
		
			
			CheckoutCommand checkoutMasterCommand = git.checkout();
			checkoutMasterCommand.setName("master");
			PullCommand pullCommand = git.pull();
			
			
			// Create Branch
			CreateBranchCommand createBranchCommand = git.branchCreate();
			createBranchCommand.setName(branchName);

			// Git checkout
			CheckoutCommand checkoutCommand = git.checkout();
			checkoutCommand.setName(branchName);

			// File write
			Callable<Boolean> fileWrite = () -> {				
				String filePath = ApplicationProperties.BASE_REPO_PATH + "/" + "resources/" + ApplicationProperties.FRESHSERVICE_FOLDER;
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
			pushCommand.setRemote(ApplicationProperties.REMOTE_NAME);

			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(stashCommand).get();
			executor.submit(checkoutMasterCommand).get();
			executor.submit(pullCommand).get();
			
			executor.submit(createBranchCommand).get();
			//TODO : Add logger
			executor.submit(checkoutCommand).get();
			executor.submit(fileWrite).get();
			executor.submit(addCommand).get();
			executor.submit(commitCommand).get();
			executor.submit(pushCommand).get();

			r.close();
			// Git push
			
			String response = raisePullrequest(branchName);
			 
			return response;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return modelName;
	}
	
	
    private static String raisePullrequest(String branchName) throws RuntimeException{  
        

        StringBuilder strBuf = new StringBuilder();  
        
        HttpURLConnection conn=null;
        BufferedReader reader=null;
        try{  
            //Declare the connection to weather api url
            URL url = new URL(ApplicationProperties.BASE_GIT_REPO_URL);  
            conn = (HttpURLConnection)url.openConnection();  
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Authorization", "Bearer "+ ApplicationProperties.APIKEY);
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            String postContent = "{\"title\":\"New Feature onboard\",\"body\":\" <Model> Onboarding !\",\"head\":\""+ApplicationProperties.BRANCH_OWNER+":"+branchName+"\",\"base\":\"master\"}";
            byte[] postData       = postContent.getBytes( StandardCharsets.UTF_8 );
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            	   wr.write(postData);
        	}
            
            if (conn.getResponseCode() != 201) {
                throw new RuntimeException("HTTP POST Request Failed with Error code : "
                              + conn.getResponseCode());
            }
            //Read the content from the defined connection
            //Using IO Stream with Buffer raise highly the efficiency of IO
	        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
            String output = null;  
            while ((output = reader.readLine()) != null)  
                strBuf.append(output);  
        }catch(MalformedURLException e) {  
            e.printStackTrace();   
        }catch(IOException e){  
            e.printStackTrace();   
        }
        finally
        {
            if(reader!=null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(conn!=null)
            {
                conn.disconnect();
            }
        }

        return strBuf.toString();  
    }
	private boolean fileWrite(String path, String fileName, String content) {
		try {
			FileWriter myWriter = new FileWriter(path + "/" + fileName);
			myWriter.write(content);
			myWriter.close();
			System.out.println("Successfully wrote to the file. " + fileName);
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		return false;
	}

	private void initSession() {
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
							((CredentialItem.StringType) item).setValue(ApplicationProperties.PASS_PHRASE);
						}
						return true;
					}
				};
				UserInfo userInfo = new GitUserInfo(ApplicationProperties.PASS_PHRASE);
				session.setUserInfo(userInfo);
				session.setConfig("StrictHostKeyChecking", "no");
				
			}
		};
		SshSessionFactory.setInstance(sessionFactory);
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
