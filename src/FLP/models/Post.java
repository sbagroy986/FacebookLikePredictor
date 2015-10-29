package FLP.models;


import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.math3.linear.*;


public class Post {
	private int with_tags=0;
	private int likes=0;
	
	public Post(JSONObject post) throws ClientProtocolException, IOException
	{
		try
		{
			getLikes(post.getJSONObject("likes"));
			System.out.println("Likes on this post:" + this.likes);
		}
		catch(Exception e)
		{
			
		}
		
	}
	
	private JSONObject sendReq(String URL) throws ClientProtocolException, IOException
	{
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(URL);
	    CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

	    String readResponse;
	    readResponse = reader.readLine();
	    JSONObject dump=new JSONObject();
	    try {
			dump = new JSONObject(readResponse);
	    }
	    catch(Exception e)
	    {
	    	System.out.println("JSON Error!");
	    }
		return dump;
	}
	
	private void getLikes(JSONObject likes_dump) throws ClientProtocolException, IOException
	{
		try
		{
			JSONObject paging = likes_dump.getJSONObject("paging");
			likes += likes_dump.getJSONArray("data").length();
			while(paging.has("next"))
			{
				likes_dump = sendReq((String)paging.get("next"));
				paging = likes_dump.getJSONObject("paging");
				likes += likes_dump.getJSONArray("data").length();
			}
		}
		catch(Exception e)
		{
			
		}
	}

	public int getWith_tags() {
		return with_tags;
	}

	public void setWith_tags(int with_tags) {
		this.with_tags = with_tags;
	}

	public int getLikes() {
		return likes;
	}

	public void setLikes(int likes) {
		this.likes = likes;
	}
	
}
