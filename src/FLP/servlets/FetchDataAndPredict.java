package FLP.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import FLP.models.Post;
import FLP.utilities.LinearRegression;

@WebServlet("/FetchDataAndPredict")
public class FetchDataAndPredict extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ArrayList<Post> posts = new ArrayList<>();
    private String token;
    private String pic_url;
    private String user_id;

    public FetchDataAndPredict() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().println("Started HERE");
		Cookie[] cookies = request.getCookies();
		for(Cookie c: cookies)
		{
			if(c.getName().equals("token"))
				token = c.getValue();
			
		}
		response.getWriter().println(token);
		
		String GET_URL = "https://graph.facebook.com/v2.5/me/posts?fields=likes,message_tags,application,caption,created_time,description,from,link,message,name,picture,place,properties,source,status_type,story,to,type,with_tags&limit=100&access_token=" + token;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(GET_URL);
	    CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

	    String readResponse;
	    ArrayList<JSONObject> posts_dump = new ArrayList<>();
	    readResponse = reader.readLine();
	    try {
			JSONObject dump = new JSONObject(readResponse);
			addPosts(posts_dump,dump.getJSONArray("data"));
			
			while(dump.has("paging") && dump.getJSONObject("paging").has("next"))
			{
				System.out.println("here");
				httpClient = HttpClients.createDefault();
				GET_URL = dump.getJSONObject("paging").getString("next");
				httpGet = new HttpGet(GET_URL);
				httpResponse = httpClient.execute(httpGet);
				reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				readResponse = reader.readLine();
				dump = new JSONObject(readResponse);
				addPosts(posts_dump,dump.getJSONArray("data"));
			}
			
			httpClient = HttpClients.createDefault();
			GET_URL = "https://graph.facebook.com/v2.5/me?fields=id,picture&access_token="+token;
			httpGet = new HttpGet(GET_URL);
			httpResponse = httpClient.execute(httpGet);
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			readResponse = reader.readLine();
			dump = new JSONObject(readResponse);
			
			pic_url = dump.getJSONObject("picture").getJSONObject("data").getString("url");
			user_id = dump.getString("id");
			
			int len = posts_dump.size();
			response.getWriter().println("Total number of posts: " + len);
			for(int i=0;i<len;i++)
			{
				JSONObject temp = posts_dump.get(i);
				Post post = new Post(temp);
				posts.add(post);
			}
			
		} catch (Exception e) {
			System.out.println("Invalid JSON Object!");
		}
	    
	    try {
			LinearRegression.setLinearRegression(posts,response,token);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    LinearRegression.display(posts,response);
	  
	    Cookie size = new Cookie("Posts","" + posts.size());
	    HashMap<String,Integer> top10 = LinearRegression.getTop10();
	    for(String s: top10.keySet())
	    {
	    	Cookie c = new Cookie(("Histo--" + s).replaceAll("\\s+",""),top10.get(s).toString());
	    	response.addCookie(c);
	    }
	    
	    Collections.shuffle(posts);
	    
	    for(int i=1;i<=15;i++)
	    {
//	    	System.out.println(posts.get(i).getRawData().toString());
	    	Cookie post_id=null,post_msg=null,post_likes=null,post_pred=null,url=null;
	    	url = new Cookie("url",URLEncoder.encode(pic_url,"UTF-8"));
	    	response.addCookie(url);
			try {
				post_id = new Cookie("post_id_"+i,URLEncoder.encode(user_id + "_"+ posts.get(i).getRawData().getString("id").split("_")[1],"UTF-8"));
				if(posts.get(i).getRawData().has("message"))
					post_msg = new Cookie("post_msg_"+i,URLEncoder.encode(posts.get(i).getRawData().getString("message"),"UTF-8"));
				else if(posts.get(i).getRawData().has("description"))
					post_msg = new Cookie("post_msg_"+i,URLEncoder.encode(posts.get(i).getRawData().getString("description"),"UTF-8"));
				else if(posts.get(i).getRawData().has("story"))
					post_msg = new Cookie("post_msg_"+i,URLEncoder.encode(posts.get(i).getRawData().getString("story"),"UTF-8"));
				else if(posts.get(i).getRawData().has("name"))
					post_msg = new Cookie("post_msg_"+i,URLEncoder.encode(posts.get(i).getRawData().getString("name"),"UTF-8"));
				post_likes = new Cookie("post_likes_"+i,URLEncoder.encode(""+posts.get(i).getLikes(),"UTF-8"));
				post_pred = new Cookie("post_pred_"+i,URLEncoder.encode(""+posts.get(i).getPredictedLikes(),"UTF-8"));
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	response.addCookie(post_id);
	    	response.addCookie(post_likes);
	    	response.addCookie(post_pred);
	    	response.addCookie(post_msg);
	    }
	    
	    RequestDispatcher view = request.getRequestDispatcher("/results.html");
		view.forward(request, response);
	    
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public static void addPosts(ArrayList<JSONObject> dump, JSONArray posts) throws JSONException
	{
		for(int j=0;j< posts.length();j++)
		{
			dump.add(posts.getJSONObject(j));
		}
	}

}
