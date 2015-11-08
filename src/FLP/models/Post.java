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
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.math3.linear.*;


public class Post {
	//Features
	
	private int with_tags=0;
	private int message_tags=0;
	
	//One Hot Encoding application type 
	private int Quora = 0;
	private int iOS = 0;
	private int Instagram = 0;
	private int Twitter = 0;
	private int Youtube = 0;
	private int OtherApp = 0;
	
	private int caption = 0;
	private int description = 0;
	private int message = 0;
	
	private int place = 0;
	
	//One Hot Encoding status type
	private int mobile_status_update = 0;
	private int added_photos = 0;
	private int added_video = 0;
	private int shared_story = 0;
	private int wall_post = 0;
	private int other_status_type = 0;
	
	//One Hot Encoding post type
	private int link = 0;
	private int status = 0;
	private int photo = 0;
	private int video = 0;
	private int other = 0;
	
	private int created_hour_of_day = 0;
	private int created_day_of_week = 0;
	
	private int likes=0;
	
	private ArrayList<String> LikesList = new ArrayList<>();
	
	public Post(JSONObject post) throws ClientProtocolException, IOException
	{
		try
		{
			getLikes(post.getJSONObject("likes"));
			if(post.has("with_tags")) setWithTags(post.getJSONObject("with_tags"));
			if(post.has("message_tags")) setMessageTags(post.getJSONArray("message_tags"));
			if(post.has("application")) setApplication(post.getJSONObject("application"));
			if(post.has("caption")) setCaption();
			if(post.has("description")) setDescription();
			if(post.has("message")) setMessage();
			if(post.has("place")) setPlace();
			if(post.has("status_type")) setStatusType((String)post.get("status_type"));
			if(post.has("type")) setType((String)post.get("type"));
			setTimeParams((String)post.get("created_time"));
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
			
			for(int j=0; j < likes_dump.getJSONArray("data").length(); j++)
			{
				LikesList.add(likes_dump.getJSONArray("data").getJSONObject(j).getString("id"));
			}
			
			while(paging.has("next"))
			{
				likes_dump = sendReq((String)paging.get("next"));
				paging = likes_dump.getJSONObject("paging");
				likes += likes_dump.getJSONArray("data").length();
				
				for(int j=0; j < likes_dump.getJSONArray("data").length(); j++)
				{
					LikesList.add(likes_dump.getJSONArray("data").getJSONObject(j).getString("id"));
				}
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	private void setWithTags(JSONObject with) throws JSONException
	{
		this.with_tags = with.getJSONArray("data").length();
	}
	
	private void setMessageTags(JSONArray msg_tags)
	{
		this.message_tags = msg_tags.length();
	}
	
	private void setApplication(JSONObject app) throws JSONException
	{
		switch((String)app.get("name"))
		{
			case "Quora": this.Quora = 1; break;
			case "iOS": this.iOS = 1; break;
			case "Instagram": this.Instagram = 1; break;
			case "Twitter": this.Twitter = 1; break;
			case "Youtube": this.Youtube = 1; break;
			default: this.OtherApp = 1; break;
		}
	}
	
	private void setCaption()
	{
		this.caption = 1;
	}
	
	private void setDescription()
	{
		this.description = 1;
	}
	
	private void setMessage()
	{
		this.message = 1;
	}

	private void setPlace()
	{
		this.place = 1;
	}
	
	private void setStatusType(String stat_type)
	{
		switch(stat_type)
		{
			case "mobile_status_update": this.mobile_status_update = 1; break;
			case "added_photos": this.added_photos = 1; break;
			case "added_video": this.added_video = 1; break;
			case "shared_story": this.shared_story = 1; break;
			case "wall_post": this.wall_post = 1; break;
			default: this.other_status_type = 1; break;
		}
	}
	
	private void setType(String type)
	{
		switch(type)
		{
			case "link": this.likes = 1; break;
			case "status": this.status = 1; break;
			case "photo": this.photo = 1; break;
			case "video": this.video = 1; break;
			default: this.other = 1; break;
		}
	}
	
	private void setTimeParams(String time) throws ParseException
	{
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		Date date = format.parse(time);
		
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		
		this.created_day_of_week = c.get(Calendar.DAY_OF_WEEK);
		this.created_hour_of_day = date.getHours();
	}
	
	public int getMessage_tags() {
		return message_tags;
	}

	public int getQuora() {
		return Quora;
	}

	public int getiOS() {
		return iOS;
	}

	public int getInstagram() {
		return Instagram;
	}

	public int getTwitter() {
		return Twitter;
	}

	public int getYoutube() {
		return Youtube;
	}

	public int getOtherApp() {
		return OtherApp;
	}

	public int getCaption() {
		return caption;
	}

	public int getDescription() {
		return description;
	}

	public int getMessage() {
		return message;
	}

	public int getPlace() {
		return place;
	}

	public int getMobile_status_update() {
		return mobile_status_update;
	}

	public int getAdded_photos() {
		return added_photos;
	}

	public int getAdded_video() {
		return added_video;
	}

	public int getShared_story() {
		return shared_story;
	}

	public int getWall_post() {
		return wall_post;
	}

	public int getOther_status_type() {
		return other_status_type;
	}

	public int getLink() {
		return link;
	}

	public int getStatus() {
		return status;
	}

	public int getPhoto() {
		return photo;
	}

	public int getVideo() {
		return video;
	}

	public int getOther() {
		return other;
	}

	public int getCreated_hour_of_day() {
		return created_hour_of_day;
	}

	public int getCreated_day_of_week() {
		return created_day_of_week;
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
	
	public ArrayList<String> getLikesList(){
		return LikesList;
	}
	
}
