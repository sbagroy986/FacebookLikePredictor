package FLP.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
    private String user_name;

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
			GET_URL = "https://graph.facebook.com/v2.5/me?fields=id,picture,name&access_token="+token;
			httpGet = new HttpGet(GET_URL);
			httpResponse = httpClient.execute(httpGet);
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			readResponse = reader.readLine();
			dump = new JSONObject(readResponse);
			
			pic_url = dump.getJSONObject("picture").getJSONObject("data").getString("url");
			user_id = dump.getString("id").toString();
			user_name = dump.getString("name");

			
			int len = posts_dump.size();
//			response.getWriter().println("Total number of posts: " + len);
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
	  
	    HashMap<String,Integer> top10 = LinearRegression.getTop10();

	    
	    Collections.shuffle(posts);
	    
	    response.getWriter().print("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><meta name=\"description\" content=\"\"><meta name=\"author\" content=\"\"><title>Results &#8226; AP Project</title><link rel=\"shortcut icon\" href=\"http://www.google.com/s2/favicons?domain=www.iiitd.ac.in\" type=\"image/x-icon\"><!-- Bootstrap Core CSS --><link href=\"./assets/css/bootstrap.min.css\" rel=\"stylesheet\"><!-- Custom CSS --><link href=\"./assets/css/small-business.css\" rel=\"stylesheet\"><!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries --><!-- WARNING: Respond.js doesn't work if you view the page via file:// --><!--[if lt IE 9]><script src=\"https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js\"></script><script src=\"https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js\"></script><![endif]--></head><body><!-- Page Content --><div class=\"container\"><!-- Heading Row --><div class=\"row\"><div class=\"col-md-8\"><h1>Results</h1><p>Using " + posts.size() +" posts obtained from the Facebook graph API, we give you the following results:</p><br/>");
		
	    
	    Double[] accuracy = LinearRegression.getAccuracy();
	    
	    response.getWriter().println("<br/>            </div>            <div class='col-md-12' style='height:650px;'>                <div id='chart_div' style='height:600px;'></div>            </div>        </div>        <hr>        <div class='row'>            <div class='col-lg-12'>                <div class='well text-center'>                    <div id='three' style=\"text-align:center\"> Accuracy for predicted likes within a 3 like margin : <b>" + new BigDecimal(accuracy[0] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='five' style=\"text-align:center\"> Accuracy for predicted likes within a 5 like margin : <b>" + new BigDecimal(accuracy[1]).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='ten' style=\"text-align:center\"> Accuracy for predicted likes within a 10 like margin : <b>" + new BigDecimal(accuracy[2] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='fif' style=\"text-align:center\"> Accuracy for predicted likes within a 15 like margin : <b>" + new BigDecimal(accuracy[3] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                </div>            </div>        </div>        <div class='row' id='posts'>");
	    int count_posts = 0;
	    for(int i=1;count_posts<=15;i++)
	    {
			try {
				if(posts.get(i).getRawData().has("message") || posts.get(i).getRawData().has("caption") || posts.get(i).getRawData().has("description"))
				{
					count_posts++;
					response.getWriter().print("<div class='col-md-8'>    <div class='panel panel-default'>    	<div class='panel-heading'>    		<h5>Post ID: <a href='https://www.facebook.com/"+posts.get(i).getRawData().getString("id")+"'>"+ posts.get(i).getRawData().getString("id") + "</a></h5>    	</div>  <div class='panel-body'><p><img src='"+ pic_url+"' height='70px' width='70px;' class='img-circle pull-left' style='margin-right:20px; margin-bottom:1px;'> <a href='https://www.facebook.com/" + user_id  +"'>" + user_name + "</a></p>");
					if(posts.get(i).getRawData().has("message"))
						response.getWriter().print(posts.get(i).getRawData().getString("message"));
					else if(posts.get(i).getRawData().has("caption"))
						response.getWriter().print(posts.get(i).getRawData().getString("caption"));
					else if(posts.get(i).getRawData().has("description"))
						response.getWriter().print(posts.get(i).getRawData().getString("description"));
					response.getWriter().print("<hr><div style='text-align:center'>Actual likes:&nbsp;"+ posts.get(i).getLikes() + "&nbsp;<img src='http://www.brandsoftheworld.com/sites/default/files/styles/logo-thumbnail/public/102011/like_icon.png?itok=nkurUMlZ' height='18px' width='18px' style='margin-bottom:6px;margin-left:2px;'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Predicted likes:&nbsp;" + posts.get(i).getPredictedLikes() + "&nbsp;<img src='https://image.freepik.com/free-icon/thumb-up-to-like-on-facebook_318-37196.png' height='18px' width='18px' style='margin-bottom:6px;margin-left:2px;'> </div>					</div>	     </div></div>");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }		
	    response.getWriter().println("</div>        <footer>            <div class='row'>                <div class='col-lg-12'>		            <p>CSE 121 &copy; AP</p>                </div>            </div>        </footer>    </div>    <script src='./assets/js/jquery.js'></script>    <script src='./assets/js/bootstrap.min.js'></script>	<script src='https://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js'></script>	<script type='text/javascript' src=\"https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1.1','packages':['bar']}]}\"></script>");
	    
	    response.getWriter().println("<script type='text/javascript'>	window.onload = function() {	};    google.setOnLoadCallback(drawChart);    function decode_utf8(s) { 	 return decodeURIComponent(escape(s));	}    function drawChart() {	");
	    response.getWriter().println("var d = [ ['People','Number of likes'],");
	    int temp_count=0;
	    for(String s: top10.keySet())
	    {
	    	temp_count++;
	    	response.getWriter().println("[\"" + s+"\","+top10.get(s)+"]");
	    	if(temp_count!=15)
	    		response.getWriter().println(",");
	    }
	    response.getWriter().println("]; var data = google.visualization.arrayToDataTable(d);	var options = {		  chart: {			 title: 'Top 15 expected likers',			 subtitle: 'Based on the number of previous posts liked',		  },		  bar: { groupWidth: '90%' },		  bars: 'horizontal',	   };	   var chart = new google.charts.Bar(document.getElementById('chart_div'));	   chart.draw(data, options);	 }	  </script></body></html>");
	    
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
