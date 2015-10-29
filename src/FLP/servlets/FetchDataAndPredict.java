package FLP.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

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
import org.json.JSONObject;

import FLP.models.Post;

@WebServlet("/FetchDataAndPredict")
public class FetchDataAndPredict extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ArrayList<Post> posts = new ArrayList<>();
    private String token;

    public FetchDataAndPredict() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Cookie[] cookies = request.getCookies();
		for(Cookie c: cookies)
		{
			if(c.getName().equals("token"))
				token = c.getValue();
			
		}
		response.getWriter().println(token);
		
		String GET_URL = "https://graph.facebook.com/v2.5/me/posts?fields=likes,message_tags,application,caption,description,from,link,message,name,picture,place,properties,source,status_type,story,to,type,with_tags&limit=100&access_token=" + token;
		response.getWriter().println(GET_URL);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(GET_URL);
	    CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

	    String readResponse;
	    readResponse = reader.readLine();
	    try {
			JSONObject dump = new JSONObject(readResponse);
			JSONArray posts_dump = new JSONArray();
			posts_dump =(JSONArray)dump.get("data");
			response.getWriter().println(posts_dump);
			int len = posts_dump.length();
//			System.out.println(len);
			for(int i=0;i<len;i++)
			{
				JSONObject temp = (JSONObject)posts_dump.get(i);
				Post post = new Post(temp);
				posts.add(post);
				response.getWriter().println("Likes on"+temp+": " + post.getLikes());
			}
		} catch (Exception e) {
			System.out.println("Invalid JSON Object!");
		}
	    
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
