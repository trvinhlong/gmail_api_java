import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;
import java.sql.*;
import java.util.Enumeration;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import java.nio.charset.Charset;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.charset.Charset;


public class GmailQuickstart {
    // Application name
    private static final String APPLICATION_NAME = "Gmail Utility";

    // Directory to store user credentials for this application
    //private static final java.io.File DATA_STORE_DIR = new java.io.File(
    //    System.getProperty("user.home"), ".credentials/gmail-api-quickstart");
	private static final java.io.File DATA_STORE_DIR = new java.io.File("", ".credentials/gmail-api-quickstart");

    // Global instance of the {@link FileDataStoreFactory}
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // Global instance of the HTTP transport
    private static HttpTransport HTTP_TRANSPORT;

    // Global instance of the scopes required by this quickstart
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.MAIL_GOOGLE_COM);
	
	private static Connection DB_CONNECTION = null;
	
	private static final String DB_URL = "jdbc:mysql://localhost/sactory_dev?characterEncoding=UTF-8&user=root&password=";
	
	private static final String DB_USERNAME = "root";
	
	private static final String DB_PASSWORD = "";
	
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
            GmailQuickstart.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Gmail client service.
     * @return an authorized Gmail client service
     * @throws IOException
     */
    public static Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
	
	/**
	 * Read email messages by lable list
	 * @return a message list
	 */
	public static List<Message> listMessagesWithLabels(Gmail service, String userId, 
		List<String> labelIds) throws IOException {
		ListMessagesResponse response = service.users().messages().list(userId).setLabelIds(labelIds).execute();
		List<Message> messages = new ArrayList<Message>();
		List<Message> currentPageMessages = response.getMessages();
		while (currentPageMessages != null) {
			for (Message message : currentPageMessages) {
				Message gmailMessage = service.users().messages().get("me", message.getId()).execute();
				messages.add(gmailMessage);
			}		
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken();
				response = service.users().messages().list(userId).setLabelIds(labelIds)
					.setPageToken(pageToken).execute();
				currentPageMessages = response.getMessages();	
			} else {
				break;
			}
		}	
		return messages;
	}
	
	public static void connectToDB() {
		try {
			Class.forName ("com.mysql.jdbc.Driver");
			DB_CONNECTION = DriverManager.getConnection(DB_URL);	
		} catch (Exception e) {
			System.out.println ("Can not connect to database");
		}
	}

	public static void saveDataToDB(String s) {
	    try {
			connectToDB(); 
			byte[] b = s.getBytes("UTF-8");
			String s1 = new String(b, "UTF8");
			PreparedStatement preparedStatement = DB_CONNECTION
			.prepareStatement("insert into  anken values (?, ?)");
      
			preparedStatement.setString(1, (UUID.randomUUID().hashCode() + "").replaceAll("-", ""));
			preparedStatement.setString(2, s);
			preparedStatement.executeUpdate();
			preparedStatement.close ();
	   } catch (Exception e) {
		   System.err.println ("Cannot connect to database server");
		   System.err.println (e.getMessage());
	   } finally {
		   if (DB_CONNECTION != null) {
			   try {
				   DB_CONNECTION.close ();
				   System.out.println ("Database connection terminated");
			   } catch (Exception e) { /* ignore close errors */ }
		   }
	   }
	}
	
    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        // Print the labels in the user's account.
        String user = "me";
        ListLabelsResponse listResponse =
            service.users().labels().list(user).execute();
			
        List<Label> labels = listResponse.getLabels();
		
		
		List<String> labelList = Arrays.asList("UNREAD");
		List<Message> messageToRead = listMessagesWithLabels(service, user, labelList);
		File file = new File("filename.txt");

		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		//BufferedWriter bw = new BufferedWriter(fw);	
		
			
		String s = "";
		for (Message message : messageToRead) {			
			MessagePart contentPart = message.getPayload().getParts().get(0);	
			//System.out.println(contentPart.getBody().getData().getBytes().toString());
			//bw.write(new String(Base64.decodeBase64(contentPart.getBody().getData().getBytes())));
			//s = new String(Base64.decodeBase64(contentPart.getBody().getData().getBytes()));
			s = new String(Base64.decodeBase64(contentPart.getBody().getData().getBytes()), "UTF-8");
			//s = Charset.forName("UTF-8").encode(s);
		}		
		saveDataToDB("20150824_01_クローバーラボ_ゆるドラシル_バナー制作");
		
		String s2 = "【案件タイトル】";
		String s3 = "【依頼日時】";
		
		//bw.write(s.substring(s.indexOf(s2) + s2.length(), s.indexOf(s3)));
		//bw.write("ã‚¯ãƒ­ãƒ¼ãƒãƒ¼ãƒ©ãƒœ");
		//bw.close();
		System.out.println(s.substring(s.indexOf(s2) + s2.length(), s.indexOf(s3)));
		System.out.println("20150824_01_クローバーラボ_ゆるドラシル_バナー制作");
		s = s.substring(s.indexOf(s2) + s2.length(), s.indexOf(s3));
		saveDataToDB(s);
		
        if (labels.size() == 0) {
            System.out.println("No labels found.");
        } else {
            System.out.println("Labels:");
            for (Label label : labels) {
                System.out.printf("- %s\n", label.getName());
				System.out.printf("- %s\n", label.getId());
            }
        }
    }

}