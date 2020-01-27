import java.lang.*;

public class TryCatch
{
	public static void main(String[] args)
	{
		try
		{
			System.out.println(args[0]);
		}
		catch(ArrayIndexOutOfBoundsException ex)
		{
			ex.printStackTrace();
		}
	}
}