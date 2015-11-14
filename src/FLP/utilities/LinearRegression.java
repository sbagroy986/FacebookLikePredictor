package FLP.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import FLP.models.Post;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class LinearRegression {
	private static String token;
	private static Matrix Y;
	private static Matrix theta;
	private static Matrix X;
	private static Matrix ans;
	private static int m;
	private static HashMap<String,Integer> freq = new HashMap<>();
	private static HashMap<String,Integer> top_10 = new HashMap<>();
	
	public static double MACHEPS = 2E-16;
	private static Double[] accuracy = new Double[4];
	
	public static void updateMacheps() {
		  MACHEPS = 1;
		  do
		   MACHEPS /= 2;
		  while (1 + MACHEPS / 2 != 1);
	}
	
	
	public static void setLinearRegression(ArrayList<Post> posts,HttpServletResponse response, String tok) throws IOException, JSONException
	{
		token = tok;
		m = posts.size();
		setY(posts);
		setX(posts,response);
		NormalEqn();
		calcFreq(posts);
		calcAns();
	}
	
	public static void setY(ArrayList<Post> posts)
	{
		double[][] y = new double[m][];
		for(int i=0;i<m;i++)
		{
			double[] temp = new double[1];
			temp[0] = posts.get(i).getLikes();
			y[i] = temp;
		}
		Y = new Matrix(y);
	}
	
	public static Matrix pinv(Matrix x) {
		  int rows = x.getRowDimension();
		  int cols = x.getColumnDimension();
		  if (rows < cols) {
		   Matrix result = pinv(x.transpose());
		   if (result != null)
		    result = result.transpose();
		   return result;
		  }
		  SingularValueDecomposition svdX = new SingularValueDecomposition(x);
		  if (svdX.rank() < 1)
		   return null;
		  double[] singularValues = svdX.getSingularValues();
		  double tol = Math.max(rows, cols) * singularValues[0] * MACHEPS;
		  double[] singularValueReciprocals = new double[singularValues.length];
		  for (int i = 0; i < singularValues.length; i++)
		   if (Math.abs(singularValues[i]) >= tol)
		    singularValueReciprocals[i] =  1.0 / singularValues[i];
		  double[][] u = svdX.getU().getArray();
		  double[][] v = svdX.getV().getArray();
		  int min = Math.min(cols, u[0].length);
		  double[][] inverse = new double[cols][rows];
		  for (int i = 0; i < cols; i++)
		   for (int j = 0; j < u.length; j++)
		    for (int k = 0; k < min; k++)
		     inverse[i][j] += v[i][k] * singularValueReciprocals[k] * u[j][k];
		  return new Matrix(inverse);
	}
	 
	public static void setX(ArrayList<Post> posts,HttpServletResponse response) throws IOException
	{
		double[][] x = new double[m][];
		
		for(int i=0;i<m;i++)
		{
			double[] temp = new double[27];
			
			temp[0] = 1;
			temp[1] = posts.get(i).getWith_tags();
			temp[2] = posts.get(i).getMessage_tags();
			
			temp[3] = posts.get(i).getQuora();
			temp[4] = posts.get(i).getiOS();
			temp[5] = posts.get(i).getInstagram();
			temp[6] = posts.get(i).getTwitter();
			temp[7] = posts.get(i).getYoutube();
			temp[8] = posts.get(i).getOtherApp();
			
			temp[9] = posts.get(i).getCaption();
			temp[10] = posts.get(i).getDescription();
			temp[11] = posts.get(i).getMessage();
			
			temp[12] = posts.get(i).getPlace();
			
			temp[13] = posts.get(i).getMobile_status_update();
			temp[14] = posts.get(i).getAdded_photos();
			temp[15] = posts.get(i).getAdded_video();
			temp[16] = posts.get(i).getShared_story();
			temp[17] = posts.get(i).getWall_post();
			temp[18] = posts.get(i).getOther_status_type();
			
			temp[19] = posts.get(i).getLink();
			temp[20] = posts.get(i).getStatus();
			temp[21] = posts.get(i).getPhoto();
			temp[22] = posts.get(i).getVideo();
			temp[23] = posts.get(i).getOther();
			
			temp[24] = posts.get(i).getCreated_hour_of_day();
			temp[25] = posts.get(i).getCreated_day_of_week();
			
//			temp[26] = posts.get(i).getNo_of_emotes();
//			temp[27] = posts.get(i).getNo_of_hts();
//			temp[28] = posts.get(i).getLength_of_details();
			temp[26] = posts.get(i).getLength_of_msg();
			
			x[i] = temp;
		}
		X = new Matrix(x);
	}
	
	public static double CostFunction(double[][] h, double[] y)
	{
		double sqrErr = 0;
		for(int i=0;i<y.length;i++)
		{
			
			sqrErr += (h[i][0] - y[i])*(h[i][0] - y[i]);
		}
		return (sqrErr/m)/2;
	}
	
	public static void calcAns()
	{
		ans = X.times(theta);
	}
	
	public static void NormalEqn()
	{
		Matrix temp = X.transpose().times(X);
		Matrix tempInverse = pinv(temp);
		theta = tempInverse.times(X.transpose()).times(Y);
	}
	
	public static void display(ArrayList<Post> posts,HttpServletResponse response) throws OutOfRangeException, IOException
	{
		double[][] y = Y.getArray();
		double[][] a = ans.getArray();
		double[] form = new double[m];
		for(int i=0;i<m;i++)
			{
				form[i]=y[i][0];
				posts.get(i).setPredictedLikes((int)a[i][0]);
			}
		double cost =  CostFunction(a,form);
		double count_15=0,count_10=0,count_5=0,count_3=0;
		System.out.println("COST : " + cost);
		for(int i=0;i<m;i++)
		{
			int flag=0;
			if(a[i][0] < (y[i][0] + 15) && a[i][0] > (y[i][0]-15))
				{
					count_15++;
					flag=1;
				}
			if(a[i][0] < (y[i][0] + 10) && a[i][0] > (y[i][0]-10))
			{
				count_10++;
				flag=1;
			}
			if(a[i][0] < (y[i][0] + 5) && a[i][0] > (y[i][0]-5))
			{
				count_5++;
				flag=1;
			}
			if(a[i][0] < (y[i][0] + 3) && a[i][0] > (y[i][0]-3))
			{
				count_3++;
				flag=1;
			}
			
//			response.getWriter().println("Actual: " + y[i][0] + " | Predicted: " + a[i][0] + "||| RESULT: " + flag);
		}
//		response.getWriter().println();
//		response.getWriter().println();
//		response.getWriter().println("Accuracy for 15 margin: "  + ((double)(count_15/(double)(m)))*100 );
//		response.getWriter().println("Accuracy for 10 margin: "  + ((double)(count_10/(double)(m)))*100 );
//		response.getWriter().println("Accuracy for 5 margin: "  + ((double)(count_5/(double)(m)))*100 );
//		response.getWriter().println("Accuracy for 3 margin: "  + ((double)(count_3/(double)(m)))*100 );
//		response.getWriter().println();
//		response.getWriter().println();
//		response.getWriter().println("Top 15 expected likes: ");
//		for(String s: top_10.keySet())
//		{
//			response.getWriter().println(s + " : " + top_10.get(s));
//		}
		count_15 = ((double)(count_15/(double)(m)))*100;
		count_10 = ((double)(count_10/(double)(m)))*100;
		count_5 = ((double)(count_5/(double)(m)))*100;
		count_3 = ((double)(count_3/(double)(m)))*100;
		accuracy[0] = count_3;
		accuracy[1] = count_5;
		accuracy[2] = count_10;
		accuracy[3] = count_15;
	}
	
	
	public static void calcFreq(ArrayList<Post> posts) throws ClientProtocolException, IOException, JSONException
	{
		int temp;
		for(Post p: posts)
		{
			System.out.println(p.getLikesList().size());
			for(String id: p.getLikesList())
			{
				
				if(freq.containsKey(id))
				{
					temp = freq.get(id);
					temp++;
					freq.put(id, temp);
				}
				else
				{
					freq.put(id, 1);
				}
			}
		}
		
		System.out.println("Size of likes: " +  freq.keySet().size());
		HashMap<Integer,String> freqInversed = new HashMap<>();
		for (Entry<String, Integer> entry : freq.entrySet())
	        freqInversed.put(entry.getValue(), entry.getKey());
//		Map<Integer, String> freqInversed = freq.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		ArrayList<String> top_ids = new ArrayList<>();
		ArrayList<Integer> t = new ArrayList<Integer>(freq.values());
		Collections.sort(t);
		int size = t.size();
		for(int j=size-1;top_ids.size() < 15;j--)
		{
				for(String s: freq.keySet())
				{
					if(freq.get(s).equals(t.get(j)) && !top_ids.contains(s))
						{
							top_ids.add(s);
						}
				}


		}
		

		for(String id: top_ids)
		{
			String GET_URL = "https://graph.facebook.com/v2.5/" + id +"?access_token="+token;
			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(GET_URL);
		    CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		    String readResponse;
		    readResponse = reader.readLine();
		    JSONObject dump = new JSONObject(readResponse);
		    top_10.put(dump.getString("name"),freq.get(id));
		}
		
	}
	
	public static HashMap<String,Integer> getTop10()
	{
		return top_10;
	}
	
	public static Double[] getAccuracy()
	{
		return accuracy;
	}
	
}
