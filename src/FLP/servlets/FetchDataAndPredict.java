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
	private ArrayList<Post> posts;
    private String token;
    private String pic_url;
    private String user_id;
    private String user_name;

    public FetchDataAndPredict() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		posts = null;
		posts = new ArrayList<>();
		token = null;
		
		Cookie[] cookies = request.getCookies();
		for(Cookie c: cookies)
		{
			if(c.getName().equals("token"))
				{
					token = c.getValue();
				}
			
		}
		
		String GET_URL = "https://graph.facebook.com/v2.5/me/posts?fields=likes,message_tags,application,caption,created_time,description,from,link,message,name,picture,place,properties,source,status_type,story,to,type,with_tags&limit=100&access_token=" + token;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(GET_URL);
	    CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

	    String readResponse;
	    ArrayList<JSONObject> posts_dump = new ArrayList<>();
	    readResponse = reader.readLine();
	    int paging_count=0;
	    try {
			JSONObject dump = new JSONObject(readResponse);
			addPosts(posts_dump,dump.getJSONArray("data"));
			
			while(dump.has("paging") && dump.getJSONObject("paging").has("next") && paging_count<5)
			{
				paging_count++;
				System.out.println(paging_count);
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
	    
	    ArrayList<Post> train_dataset = new ArrayList<>();
	    Collections.shuffle(posts);
	    int count_posts = 0;
	    for(int i=0;count_posts < 0.2*posts.size();i++)
	    {
	    	if(posts.get(i).getRawData().has("message") || posts.get(i).getRawData().has("caption") || posts.get(i).getRawData().has("description"))
	    	{
	    		train_dataset.add(posts.get(i));
	    		posts.remove(i);
	    		count_posts++;
	    	}
	    }
	    
	    try {
			LinearRegression.setLinearRegression(posts,response,token);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    LinearRegression.display(posts,response);
	  
	    HashMap<String,Integer> top10 = LinearRegression.getTop10();
	    
	    response.getWriter().print("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><meta name=\"description\" content=\"\"><meta name=\"author\" content=\"\"><title>Results &#8226; AP Project</title><link rel=\"shortcut icon\" href=\"http://www.google.com/s2/favicons?domain=www.iiitd.ac.in\" type=\"image/x-icon\"><!-- Bootstrap Core CSS --><link href=\"./assets/css/bootstrap.min.css\" rel=\"stylesheet\"><!-- Custom CSS --><link href=\"./assets/css/small-business.css\" rel=\"stylesheet\"><!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries --><!-- WARNING: Respond.js doesn't work if you view the page via file:// --><!--[if lt IE 9]><script src=\"https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js\"></script><script src=\"https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js\"></script><![endif]--></head><body><!-- Page Content -->    <div class='container'> <div class='row'><div class='col-md-12'>    <h1>Procedure</h1>    <p>Implemented a basic Machine Learning algorithm to predict likes on a post, using the previous posts of a user.Since the algorithm had to be self-implemented (i.e, we could not use third-party ML libraries), we have used simple Linear Regression for this project.</p>     		<p style='text-align:center;'>h<sub>&#920;</sub>(x) = &#920;<sub>0</sub> + &#920;<sub>1</sub>x<sub>1</sub> + &#920;<sub>2</sub>x<sub>2</sub> + .... + &#920;<sub>n</sub>x<sub>n</sub> = X&#920;<sup>T</sup></p>     		<p>Here, h<sub>&#920;</sub> is our hypothesis function. X is a mxn matrix where m is the number of data samples (number of posts, in this case) and n is the number of features. &#920; is a 1xn matrix which contains parameters (&#920;<sub>1</sub>,&#920;<sub>2</sub>...&#920;<sub>n</sub>) corresponding to each feature. This gives us h<sub>&#920;</sub>, an mx1 matrix which we cross-reference with the actual likes of the m corresponding posts to calculate accuracy.</p>     		<p>We are extracting 14 features for each post. This list is (by no means) exhaustive. It's possible to go into much more detail but since we're short on time, we'll just cover these basic features: </p>     		<div class='container'>     			<div class='row'>     				<div class='col-md-4'>			     		<ul>								<li>People in (with the user) the post</li>								<li>People tagged in the message</li>								<li>Source of post (Categorical/One Hot Encoded)</li>								<li>Presence of message</li>								<li>Presence of description</li>								<li>Presence of caption</li>								<li>Length of message</li>			     		</ul>     					     				</div>     				<div style='col-md-4'>			     		<ul>								<li>Presence of location</li>								<li>Type of post (Categorical/One Hot Encoded)</li>								<li>Type of status (Categorical/One Hot Encoded)</li>								<li>Created hour of day</li>								<li>Created day of week</li>								<li>Number of hashtags</li>								<li>Number of emotes</li>			     		</ul>     					     				</div>     			</div>     		</div></div> </div>  <hr><div class=\"container\"><!-- Heading Row --><div class=\"row\"><div class=\"col-md-8\"><h1>Results</h1><p>Using " + posts.size() +" posts obtained from the Facebook graph API, we give you the following results:</p><br/>");
		
	    
	    Double[] accuracy = LinearRegression.getAccuracy();
	    
	    response.getWriter().println("<br/>            </div>            <div class='col-md-12' style='height:650px;'>                <div id='chart_div' style='height:600px;'></div>            </div><div class='col-md-6'> <div id='curve_chart' style='width: 650px; height: 500px'></div></div><div class='col-md-5' style='height:597px;'>  <ul class='nav nav-tabs list-inline' style='margin-top:55px'>  <li class='active'><a data-toggle='tab' href='#menu0'>3 likes</a></li>  <li><a data-toggle='tab' href='#menu1'>5 likes</a></li>  <li><a data-toggle='tab' href='#menu2'>10 likes</a></li>  <li><a data-toggle='tab' href='#menu3'>15 likes</a></li></ul><div class='tab-content'><div id='menu0' class='tab-pane fade active'>  <div id='donutchart-3' style='width: 700px; height: 500px;'></div></div><div id='menu1' class='tab-pane fade active'>  <div id='donutchart-5' style='width: 700px; height: 500px;'></div></div><div id='menu2' class='tab-pane fade active'>  <div id='donutchart-10' style='width: 700px; height: 500px;'></div></div><div id='menu3' class='tab-pane fade active'>  <div id='donutchart-15' style='width: 700px; height: 500px;'></div></div></div> </div> </div>       <hr>        <div class='row'>            <div class='col-lg-12'>                <div class='well text-center'>                    <div id='three' style=\"text-align:center\"> Accuracy for predicted likes within a 3 like margin : <b>" + new BigDecimal(accuracy[0] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='five' style=\"text-align:center\"> Accuracy for predicted likes within a 5 like margin : <b>" + new BigDecimal(accuracy[1]).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='ten' style=\"text-align:center\"> Accuracy for predicted likes within a 10 like margin : <b>" + new BigDecimal(accuracy[2] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                    <div id='fif' style=\"text-align:center\"> Accuracy for predicted likes within a 15 like margin : <b>" + new BigDecimal(accuracy[3] ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%</b></div>                </div>            </div>        </div>        <div class='row' id='posts'>");
	    LinearRegression.testTheta(train_dataset);
	    for(int i=0;i<train_dataset.size();i++)
	    {
			try {
					count_posts++;
					response.getWriter().print("<div class='col-md-8'>    <div class='panel panel-default'>    	<div class='panel-heading'>    		<h5>Post ID: <a href='https://www.facebook.com/"+train_dataset.get(i).getRawData().getString("id")+"'>"+ train_dataset.get(i).getRawData().getString("id") + "</a></h5>    	</div>  <div class='panel-body'><p><img src='"+ pic_url+"' height='70px' width='70px;' class='img-circle pull-left' style='margin-right:20px; margin-bottom:1px;'> <a href='https://www.facebook.com/" + user_id  +"'>" + user_name + "</a></p>");
					if(train_dataset.get(i).getRawData().has("message"))
						response.getWriter().print(train_dataset.get(i).getRawData().getString("message"));
					else if(train_dataset.get(i).getRawData().has("caption"))
						response.getWriter().print(train_dataset.get(i).getRawData().getString("caption"));
					else if(train_dataset.get(i).getRawData().has("description"))
						response.getWriter().print(train_dataset.get(i).getRawData().getString("description"));
					response.getWriter().print("<hr><div style='text-align:center'>Actual likes:&nbsp;"+ train_dataset.get(i).getLikes() + "&nbsp;<img src='http://www.brandsoftheworld.com/sites/default/files/styles/logo-thumbnail/public/102011/like_icon.png?itok=nkurUMlZ' height='18px' width='18px' style='margin-bottom:6px;margin-left:2px;'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Predicted likes:&nbsp;" + train_dataset.get(i).getPredictedLikes() + "&nbsp;<img src='https://image.freepik.com/free-icon/thumb-up-to-like-on-facebook_318-37196.png' height='18px' width='18px' style='margin-bottom:6px;margin-left:2px;'> </div>					</div>	     </div></div>");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }		
	    response.getWriter().println("</div>        <footer>            <div class='row'>                <div class='col-lg-12'>		            <p>CSE 121 &copy; AP</p>                </div>            </div>        </footer>    </div>    <script src='./assets/js/jquery.js'></script>    <script src='./assets/js/bootstrap.min.js'></script>	<script src='https://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js'></script>	<script type='text/javascript' src=\"https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1.1','packages':['bar']}]}\"></script><script type='text/javascript' src=\"https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1','packages':['corechart']}]}\"></script>");
	    
	    response.getWriter().println("<script type='text/javascript'>	window.onload = function() {	};    google.setOnLoadCallback(drawChart);    function decode_utf8(s) { 	 return decodeURIComponent(escape(s));	}    function drawChart() {	");
	    response.getWriter().println("var d = [ ['People','Number of likes'],");
	    int temp_count=0;
	    for(String s: top10.keySet())
	    {
	    	temp_count++;
	    	response.getWriter().println("[\"" + s+"\","+top10.get(s)+"]");
	    	if(temp_count!=top10.keySet().size())
	    		response.getWriter().println(",");
	    }
	    response.getWriter().print("]; var data = google.visualization.arrayToDataTable(d);	var options = {		  chart: {			 title: 'Top 15 expected likers',			 subtitle: 'Based on the number of previous posts liked',		  },		  bar: { groupWidth: '90%' },		  bars: 'horizontal',	   };	   var chart = new google.charts.Bar(document.getElementById('chart_div'));	   chart.draw(data, options);");
	    response.getWriter().print("        var line_data = google.visualization.arrayToDataTable([['Year', 'Actual', 'Predicted'],");
	    for(int i=0;i<posts.size();i++)
	    {
	    	response.getWriter().println("['',"+ posts.get(i).getLikes() +","+posts.get(i).getPredictedLikes()+"]");
	    	if(i!= posts.size()-1) response.getWriter().println(",");
	    }
	    response.getWriter().println("]);                var options = {                  title: 'Actual vs Predicted',                  curveType: 'function',                  legend: { position: 'bottom' }                };                var chart = new google.visualization.LineChart(document.getElementById('curve_chart'));                chart.draw(line_data, options);");
	    response.getWriter().println("var pie_data = google.visualization.arrayToDataTable([  ['Correct', 'Incorrect'],");
	    response.getWriter().println("['Correct'," + accuracy[1]+"],['Incorrect',"+(100-accuracy[1])+"]");
	    //	    "['Correct',     80],  ['Incorrect',      20]"
	    response.getWriter().println("]);var options = {  title: 'Accuracy (5 Likes Margin)',  pieHole: 0.4,};var chart = new google.visualization.PieChart(document.getElementById('donutchart-5'));chart.draw(pie_data, options);");
	    response.getWriter().println("pie_data = google.visualization.arrayToDataTable([  ['Correct', 'Incorrect'],");
	    response.getWriter().println("['Correct'," + accuracy[0]+"],['Incorrect',"+(100-accuracy[0])+"]");
	    //	    "['Correct',     80],  ['Incorrect',      20]"
	    response.getWriter().println("]);var options = {  title: 'Accuracy (3 Likes Margin)',  pieHole: 0.4,};var chart = new google.visualization.PieChart(document.getElementById('donutchart-3'));chart.draw(pie_data, options);");
	    response.getWriter().println("pie_data = google.visualization.arrayToDataTable([  ['Correct', 'Incorrect'],");
	    response.getWriter().println("['Correct'," + accuracy[2]+"],['Incorrect',"+(100-accuracy[2])+"]");
	    //	    "['Correct',     80],  ['Incorrect',      20]"
	    response.getWriter().println("]);var options = {  title: 'Accuracy (10 Likes Margin)',  pieHole: 0.4,};var chart = new google.visualization.PieChart(document.getElementById('donutchart-10'));chart.draw(pie_data, options);");
	    response.getWriter().println("pie_data = google.visualization.arrayToDataTable([  ['Correct', 'Incorrect'],");
	    response.getWriter().println("['Correct'," + accuracy[3]+"],['Incorrect',"+(100-accuracy[3])+"]");
	    //	    "['Correct',     80],  ['Incorrect',      20]"
	    response.getWriter().println("]);var options = {  title: 'Accuracy (15 Likes Margin)',  pieHole: 0.4,};var chart = new google.visualization.PieChart(document.getElementById('donutchart-15'));chart.draw(pie_data, options);");
	    
	    response.getWriter().print("document.getElementById('menu0').className += ' in';}	  </script></body></html>");
	    
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
