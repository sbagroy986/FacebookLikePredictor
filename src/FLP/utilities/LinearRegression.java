package FLP.utilities;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import FLP.models.Post;

public class LinearRegression {
	private static RealMatrix Y;
	private static RealMatrix theta;
	private static RealMatrix X;
	private static RealMatrix ans;
	private static int m;
	
	public static void setLinearRegression(ArrayList<Post> posts,HttpServletResponse response) throws IOException
	{
		m = posts.size();
		setY(posts);
		setX(posts,response);
		NormalEqn();
		calcAns();
	}
	
	public static void setY(ArrayList<Post> posts)
	{
		double[] y = new double[m];
		for(int i=0;i<m;i++)
		{
			y[i] = posts.get(i).getLikes();
		}
		Y = new Array2DRowRealMatrix(y);
		System.out.println(Y);
//		Y = Y.transpose();
	}
	
	public static void setX(ArrayList<Post> posts,HttpServletResponse response) throws IOException
	{
		double[][] x = new double[m][];
		
		for(int i=0;i<m;i++)
		{
			double[] temp = new double[9];
			
			temp[0] = 1;
			temp[1] = posts.get(i).getWith_tags();
			temp[2] = posts.get(i).getMessage_tags();
			
//			temp[3] = posts.get(i).getQuora();
//			temp[4] = posts.get(i).getiOS();
//			temp[5] = posts.get(i).getInstagram();
//			temp[6] = posts.get(i).getTwitter();
//			temp[7] = posts.get(i).getYoutube();
//			temp[8] = posts.get(i).getOtherApp();
			
			temp[3] = posts.get(i).getCaption();
			temp[4] = posts.get(i).getDescription();
			temp[5] = posts.get(i).getMessage();
			
			temp[6] = posts.get(i).getPlace();
			
//			temp[13] = posts.get(i).getMobile_status_update();
//			temp[14] = posts.get(i).getAdded_photos();
//			temp[15] = posts.get(i).getAdded_video();
//			temp[16] = posts.get(i).getShared_story();
//			temp[17] = posts.get(i).getWall_post();
//			temp[18] = posts.get(i).getOther_status_type();
			
//			temp[19] = posts.get(i).getLink();
//			temp[20] = posts.get(i).getStatus();
//			temp[21] = posts.get(i).getPhoto();
//			temp[22] = posts.get(i).getVideo();
//			temp[23] = posts.get(i).getOther();
			
			temp[7] = posts.get(i).getCreated_hour_of_day();
			temp[8] = posts.get(i).getCreated_day_of_week();
		
			x[i] = temp;
		}
		X = new Array2DRowRealMatrix(x);
		response.getWriter().println(X);
	}
	
	public static double CostFunction(double[][] h, double[] y)
	{
		double sqrErr = 0;
		for(int i=0;i<y.length;i++)
		{
			
			sqrErr += (h[i][0] - y[i])*(h[i][0] - y[i]);
			System.out.println("h:" + h[i][0] + " y:" + y[i] + " ERR:" + sqrErr + " ==== Iteration: " + i);
		}
		return (sqrErr/m)/2;
	}
	
	public static void calcAns()
	{
		ans = X.multiply(theta);
	}
	
	public static void NormalEqn()
	{
		RealMatrix temp = X.transpose().multiply(X);
//		System.out.println(Y.getRowDimension() + " x " + Y.getColumnDimension());
//		System.out.println(temp.getRowDimension() + " x " + temp.getColumnDimension());
//		System.out.println(X.getRowDimension() + " x " + X.getColumnDimension());
//		System.out.println( new LUDecomposition(temp).getDeterminant());
		RealMatrix tempInverse = new LUDecomposition(temp).getSolver().getInverse();
		theta = tempInverse.multiply(X.transpose()).multiply(Y);
	}
	
	public static void display(HttpServletResponse response) throws OutOfRangeException, IOException
	{
		double[][] y = Y.getData();
		double[][] a = ans.getData();
		double[] form = new double[m];
		for(int i=0;i<m;i++)
			form[i]=y[i][0];
		double cost =  CostFunction(a,form);
		double count_15=0,count_10=0;
		System.out.println("COST : " + cost);
		for(int i=0;i<m;i++)
		{
			int flag=0;
			if(a[i][0] < (y[i][0] + 15) && a[i][0] > (y[i][0]-15))
				{
					count_15++;
					flag=1;
				}
			if(a[i][0] < (y[i][0] + 10) && a[i][0] > (y[i][0]-1))
			{
				count_10++;
				flag=1;
			}
			
			response.getWriter().println("Actual: " + y[i][0] + " | Predicted: " + a[i][0] + "||| RESULT: " + flag);
		}
		response.getWriter().println("Accuracy for 15 margin: "  + ((double)(count_15/(double)(m)))*100 );
		response.getWriter().println("Accuracy for 10 margin: "  + ((double)(count_10/(double)(m)))*100 );
	}
	
}
