import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class test {
	
	public static File trusted = new File ("C:\\Users\\T0204775\\Documents\\MRU Config Files\\project_trusted");
	public static File untrusted = new File ("C:\\Users\\T0204775\\Documents\\MRU Config Files\\project_untrusted");

	public static File fileChooser (String dialog)
	{
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		jfc.setDialogTitle(dialog);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		int returnValue = jfc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			System.out.println(selectedFile.getPath());
			return selectedFile;
		}
		else
		{
			return null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		File trustedRMS, untrustedRMS, trustedTXT, trustedXML, untrustedXML;
		int i;
		
		trusted = fileChooser("Selected Trusted Config Directory...");
		untrusted = fileChooser("Selected Untrusted Config Directory...");
		
		/*if (new File(trusted.getPath() + "\\MRU0").exists() && new File(untrusted.getPath() + "\\MRU0").exists())
		{
			i = 0;
		}
		else
		{
			i = 1;
		}

		while (new File(trusted.getPath() + "\\MRU" + i).exists() && new File(untrusted.getPath() + "\\MRU" + i).exists())
		{
			if (new File((trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt")).exists())
			{
				trustedTXT = new File (trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
				System.out.println(trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
			}
			
			if (new File((trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml")).exists())
			{
				trustedXML = new File (trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
				System.out.println(trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
			}
			
			if (new File((untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml")).exists())
			{
				trustedTXT = new File (untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
				System.out.println(untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
			}
			
			System.out.println();

			i++;
		}*/
	}
}






/**/


/*if (currentLine.contains("<left-ip>"))
{
	IPLine = currentLine.trim();
	leftIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
}
if ((currentLine).contains("<left-id-ip>"))
{
	IPLine = currentLine.trim();
	leftIDIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
}
if ((currentLine).contains("<left-subnet>"))
{
	IPLine = currentLine.trim();
	leftSubnet = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("/"));
	leftSubnetRange = IPLine.substring(IPLine.indexOf("/") + 1, IPLine.indexOf("</"));
}




boolean isMatch = true;
		char[] subnetmask = IPtoBinary("255.255.128.0");
		char[] ip1 = IPtoBinary("172.22.144.1");
		char[] ip2 = IPtoBinary("172.22.152.1");
		int range = 0;
		
		for (int x  = 0; x < subnetmask.length; x++)
		{
			if (subnetmask[x] == '1')
			{
				range++;
			}
		}
		
		System.out.println(range);
		
		for (int i = 0; i < range; i++)
		{
			if (ip1[i] != ip2[i])
			{
				isMatch = false;
			}
		}
		System.out.println(isMatch);
		
		
		
		
		
		
		public static char[] IPtoBinary (String IP) throws Exception
	{
		InetAddress octIP = InetAddress.getByName(IP);
		String binary = "", tempbin;
		
		byte[] bytes = octIP.getAddress();
		for (byte b : bytes)
		{
			tempbin = (Integer.toBinaryString(b & 0xFF));
			while (tempbin.length() != 8)
			{
				tempbin = '0' + tempbin;
			}
			binary += tempbin;
		}
		
		
		System.out.println(binary);
		
		return (binary.toCharArray());
	}
		*/
