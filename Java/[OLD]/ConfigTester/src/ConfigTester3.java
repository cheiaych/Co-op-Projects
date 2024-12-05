import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class ConfigTester3 {

	public static File trusted;
	public static File untrusted;
	
	public static File trustedconfigtxt; //Trusted side port forwarding config file
	public static File trustedconfigxml; //Trusted side config file
	public static File untrustedconfigxml; //Untrusted side config file
	
	public static class Packet
	{
		static String DestIP;
		static String SrcIP;
		static String DestPort;
		static String SrcPort;
		static String RcvID;
		static String DestID;
		static String IntPort;
		
		Packet (String DIp, String SIp, String DPort, String SPort, String RId)
		{
			DestIP = DIp;
			SrcIP = SIp;
			DestPort = DPort;
			SrcPort = SPort;
			RcvID = RId;
		}
		
		Packet (String DIp, String DPort)
		{
			DestPort = DPort;
			DestIP = DIp;
		}

		
		void setDest (String ip, String port)
		{
			DestIP = ip;
			DestPort = port;
		}
		
		void setDest (String iPort, String ip, String dPort)
		{
			DestIP = ip;
			DestPort = dPort;
			IntPort = iPort;
		}
	}
	
	public static class Port
	{
		String InternalPort, BroadIP, BroadPort, StartByte, NumBytes;
		Boolean Filter, BigEndian;
		List<ZoneController> DestChange = new ArrayList<ZoneController>();
		
		Port (String IntPort, String BroIp, String BroPort, Boolean Filt, String StrByte, String NBytes, Boolean Endian)
		{
			InternalPort = IntPort;
			BroadIP = BroIp;
			BroadPort = BroPort;
			Filter = Filt;
			StartByte = StrByte;
			NumBytes = NBytes;
			BigEndian = Endian;
		}
	}
	
	public static class ZoneController
	{
		String DestID, newDestIP, newDestPort;
		
		ZoneController (String Id, String Ip, String Port)
		{
			DestID = Id;
			newDestIP = Ip;
			newDestPort = Port;
		}
	}
	
	public static class Connection
	{
		String name, leftsubnet, leftsubrange, rightip, rightidip, rightsubnet, rightsubrange, rightglobalip, connectionmode, activated;
		
		Connection (String n, String ls, String lsr, String ri, String rii, String rs, String rsr, String rgi, String cm, String act)
		{
			name = n;
			leftsubnet = ls;
			leftsubrange = lsr;
			rightip = ri;
			rightidip = rii;
			rightsubnet = rs;
			rightsubrange = rsr;
			rightglobalip = rgi;
			connectionmode = cm;
			activated = act;
		}
		
		boolean isActivated()
		{
			if (activated.equals("route"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	public static List<Port> Ports = new ArrayList<Port>();
	public static List<Connection> Connections = new ArrayList<Connection>();
	public static String XMLBroadIPRange, leftIP, leftIDIP, tcvPartLinkAddr, tcvPartNetMask, leftSubnet, leftSubnetRange, tcvPartAddr;
	public static String TrustInternalIP, TrustExternalIP, TrustExternalPort;
	public static String UntrustTcvBridgeAddr;
	public static int portIndex, destIndex;
	public static int connectionNo;
	
	//////////////////////////////////////////////////////////////////
	//																//
	//						END OF DEFINITIONS						//
	//																//	
	//////////////////////////////////////////////////////////////////
	
	public static void trustedConfigTxtReader() throws FileNotFoundException
	{
		Scanner txtReader = new Scanner (trustedconfigtxt);
		String currentLine = ";";
		String[] splitLine;
		Boolean filter, bigEndian;
		Port tempPort;
		
		while (txtReader.hasNextLine())
		{
			if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'P')
			{
				splitLine = currentLine.split("\\s+");
				if (splitLine[4].equals("1"))
				{
					filter = true;
				}
				else
				{
					filter = false;
				}
				
				if (splitLine[7].equals("1"))
				{
					bigEndian = true;
				}
				else
				{
					bigEndian = false;
				}
				
				tempPort = new Port(splitLine[1], splitLine[2], splitLine[3], filter, splitLine[5], splitLine[6], bigEndian);
				
				currentLine = txtReader.nextLine();

				while (txtReader.hasNextLine() && ((currentLine.trim().isEmpty()) || (currentLine.charAt(0) != 'P')))
				{
					if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) != ';')
					{
						splitLine = currentLine.split("\\s+");
						tempPort.DestChange.add(new ZoneController(splitLine[0] , splitLine[1], splitLine[2]));
					}
					currentLine = txtReader.nextLine();
				}
				Ports.add(tempPort);
			}
			else
			{
				if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'E')
				{
					splitLine = currentLine.split("\\s+");
					TrustExternalIP = splitLine[1];
					TrustExternalPort = splitLine[2];
				}
				
				if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'I')
				{
					splitLine = currentLine.split("\\s+");
					TrustInternalIP = splitLine[1];
				}
				
				currentLine = txtReader.nextLine();
			}
		}
		txtReader.close();
	}
	
	public static void trustedConfigXmlReader() throws Exception
	{
		String currentLine, IPLine = "", IPLine2;
		boolean routeFound = false;
		Scanner xmlReader = new Scanner(trustedconfigxml);
		
		while (xmlReader.hasNextLine())
		{
			if ((currentLine = xmlReader.nextLine()).contains("TCV_TRAIN_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				XMLBroadIPRange = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			if (currentLine.contains("<local>"))
			{
				IPLine = xmlReader.nextLine().trim();
				leftIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
				
				IPLine = xmlReader.nextLine().trim();
				leftIDIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
				
				IPLine = xmlReader.nextLine().trim();
				leftSubnet = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("/"));
				leftSubnetRange = IPLine.substring(IPLine.indexOf("/") + 1, IPLine.indexOf("</"));
			}
			if (currentLine.contains("TCV_PARTNER_LINK_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartLinkAddr = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			if (currentLine.contains("TCV_PARTNER_LINK_NETMASK"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartNetMask = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			if (currentLine.contains("TCV_PARTNER_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartAddr = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			if (currentLine.trim().equals("<connection>"))
			{
				String name = "", leftsub = "", leftsubrange = "", rightip = "", rightidip = "", rightsub = "", rightsubrange = "", rightglobal = "", connectmode = "", activated = "";
				String temp;
				while (!(currentLine = xmlReader.nextLine()).contains("</connection>"))
				{
					if (currentLine.contains("<name>"))
					{
						temp = currentLine.trim();
						name = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<left-subnet>"))
					{
						temp = currentLine.trim();
						leftsub = temp.substring(temp.indexOf(">") + 1, temp.indexOf("/"));
						leftsubrange = temp.substring(temp.indexOf("/") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<right-ip>"))
					{
						temp = currentLine.trim();
						rightip = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<right-id-ip>"))
					{
						temp = currentLine.trim();
						rightidip = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<right-subnet>"))
					{
						temp = currentLine.trim();
						rightsub = temp.substring(temp.indexOf(">") + 1, temp.indexOf("/"));
						rightsubrange = temp.substring(temp.indexOf("/") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<right-global-ip>"))
					{
						temp = currentLine.trim();
						rightglobal = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<connection-mode>"))
					{
						temp = currentLine.trim();
						connectmode = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
					if (currentLine.contains("<connection-start-up>"))
					{
						temp = currentLine.trim();
						activated = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</"));
					}
				}
				//System.out.println(name + " " + leftsub + " " + rightip + " " + rightidip + " " + rightsub + " " + rightglobal + " " + connectmode + " " + activated);
				Connections.add(new Connection(name, leftsub, leftsubrange, rightip, rightidip, rightsub, rightsubrange, rightglobal, connectmode, activated));
			}
		}
		xmlReader.close();
	}
	
	public static void untrustedConfigXmlReader () throws Exception
	{
		String currentLine;
		Scanner xmlReader = new Scanner(untrustedconfigxml);
		
		while (xmlReader.hasNextLine())
		{
			if (xmlReader.nextLine().contains("TCV_BRIDGE_ADDR"))
			{
				currentLine = xmlReader.nextLine();
				//System.out.println(currentLine);
				UntrustTcvBridgeAddr = currentLine.substring(currentLine.indexOf(">") + 1, currentLine.indexOf("</"));
			}
		}
		xmlReader.close();
	}
	
	//////////////////////////////////////////////////////////////////
	//																//
	//							RULE CHECKING						//
	//																//
	//////////////////////////////////////////////////////////////////
	
	public static boolean ruleChecker(Packet P) throws Exception
	{
		boolean rule = false;
		
		
		System.out.println("Matching Untrusted XML TCV Bridge Address: " + UntrustTcvBridgeAddr + " to Trusted XML tcvPartAddress: " + tcvPartAddr);
		if (UntrustTcvBridgeAddr.equals(tcvPartAddr))
		{
			System.out.println("Checking if TCV_PARTNER_LINK_ADDR: " + tcvPartLinkAddr + " and MRU Config Connection right-ip: " + Connections.get(connectionNo).rightip + " are in Subnet: " + tcvPartNetMask);
			if (subnetRange(tcvPartLinkAddr, Connections.get(connectionNo).rightip, subnetMaskToRange(tcvPartNetMask.toCharArray())))
			{
				System.out.println("Checking if Trusted MRU left-subnet: " + leftSubnet + '/' + leftSubnetRange + " Contains Trusted Port Forwarder External IP: " + TrustExternalIP);
				if (subnetRange(leftSubnet, TrustExternalIP, leftSubnetRange))
				{
					System.out.println("Checking if Trusted MRU Broadcast IP: " + XMLBroadIPRange + " Matches Trusted Port Fowarder Internal IP: " + TrustInternalIP);
					if (XMLBroadIPRange.equals(TrustInternalIP))
					{
						System.out.println("Checking if Destination IP: " + P.DestIP + " and Destination Port: " + P.DestPort + " Match External IP: " + TrustExternalIP + " and External Port: " + TrustExternalPort);
						if (P.DestIP.equals(TrustExternalIP) && P.DestPort.equals(TrustExternalPort))
						{
							System.out.println("Configuration files and packet are valid");
							rule = true;
						}
						else
						{
							System.out.print("Packet IP and Port do not match External IP and External Port in Port Forwarding File");
							rule = false;
							return rule;
						}
					}
					else
					{
						System.out.print("BroadcastIP matches Internal IP");
						rule = false;
						return rule;
					}
				}
				else
				{
					System.out.print("LeftIP, LeftIDIP, and TCV Partner Link Address do not match");
					rule = false;
					return rule;
				}
			}
			else
			{
				System.out.print("ERROR: 'right-ip and TCV_PARTNER_LINK_ADDR are not in same subnet'");
				rule = false;
				return rule;
			}
		}
		else
		{
			System.out.print("ERROR: 'TCV_PARTNER_ADDR does not match TCV_BRIDGE_ADD'");
			rule = false;
			return rule;
		}
		return rule;
	}
	
	private static boolean findNewDest (Packet P)
	{
		for (int i = 0; i < Ports.size(); i++)
		{
			if (P.DestPort.equals(Ports.get(i).InternalPort))
			{
				if (Ports.get(i).Filter == true)
				{
					for (int j = 0; j < Ports.get(i).DestChange.size(); j++)
					{
						if (P.DestID.equals(Ports.get(i).DestChange.get(j).DestID))
						{
							P.setDest(Ports.get(i).DestChange.get(j).newDestIP, Ports.get(i).DestChange.get(j).newDestPort);
							//System.out.println(i + ' ' + j);
							portIndex = i;
							destIndex = j;
							
							return true;
						}
					}
				}
				else
				{
					for (int j = 0; j < Ports.get(i).DestChange.size(); j++)
					{
						P.setDest(Ports.get(i).DestChange.get(j).newDestIP, Ports.get(i).DestChange.get(j).newDestPort);
						//System.out.println(i + ' ' + j);
						portIndex = i;
						destIndex = j;
						
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private static boolean findConnection(Packet P) throws Exception
	{
		int matchConnect = 0, index = -1;
		
		for (int i = 0; i < Connections.size(); i++)
		{
			//System.out.println(Connections.get(i).rightsubnet + '/' + Connections.get(i).rightsubrange + ' ' + P.DestIP);
			if (Connections.get(i).activated.equals("route") && subnetRange(Connections.get(i).rightsubnet, P.DestIP, Connections.get(i).rightsubrange))
			{
				index = i;
				matchConnect++;
			}
			//System.out.println("Matching connections: " + matchConnect);
		}
		if (matchConnect > 1)
		{
			System.out.println("Multiple matching connections");
			matchConnect = 0;
			return false;
		}
		else if (matchConnect == 0)
		{
			System.out.println("No matching connections");
			matchConnect = 0;
			return false;
		}
		else
		{
			connectionNo = index;
			matchConnect = 0;
			return true;
		}
	}
	
	private static boolean findBroadDestination(Packet P) throws Exception
	{
		for (int i = 0; i < Ports.size(); i++)
		{
			for (int j = 0; j < Ports.get(i).DestChange.size(); j++)
			{
				//System.out.println(Ports.get(i).DestChange.get(j).DestID + ' ' + Ports.get(i).DestChange.get(j).newDestIP + ' ' + Ports.get(i).DestChange.get(j).newDestPort);
				if (P.DestID.equals(Ports.get(i).DestChange.get(j).DestID) && P.DestIP.equals(Ports.get(i).DestChange.get(j).newDestIP) && P.DestPort.equals(Ports.get(i).DestChange.get(j).newDestPort))
				{
					P.setDest(Ports.get(i).InternalPort, Ports.get(i).BroadIP, Ports.get(i).BroadPort);
					portIndex = i;
					destIndex = j;
					return true;
				}
			}
		}
		return false;
	}
	
	//////////////////////////////////////////////////////////////////
	//																//
	//							UTILITIES							//
	//																//
	//////////////////////////////////////////////////////////////////
	
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
	
	public static char[] IPtoBinary (String IP) throws Exception
	{
		InetAddress octIP = InetAddress.getByName(IP);
		String binary = "", tempbin;
		
		//System.out.println(IP);
		
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
		
		//System.out.println(binary);
		
		return (binary.toCharArray());
	}
	
	private static boolean subnetRange (String subnet, String host, String range) throws Exception
	{
		char[] sub = IPtoBinary(subnet);
		char[] hostIP = IPtoBinary(host);
		
		//System.out.println(range);
		
		for (int i = 0; i < Integer.parseInt(range); i++)
		{
			if (sub[i] != hostIP[i])
			{
				return false;
			}
		}
		return true;
	}
	
	private static boolean subnetRange (String subnet, String host, int range) throws Exception
	{
		char[] sub = IPtoBinary(subnet);
		char[] hostIP = IPtoBinary(host);
		
		//System.out.println(range);
		
		for (int i = 0; i < range; i++)
		{
			if (sub[i] != hostIP[i])
			{
				return false;
			}
		}
		return true;
	}
	
	private static int subnetMaskToRange (char[] subnetmask)
	{
		int range = 0;
		for (int x  = 0; x < subnetmask.length; x++)
		{
			if (subnetmask[x] == '1')
			{
				range++;
			}
		}
		return range;
	}
	
	public static void printAll()
	{
		System.out.println(TrustInternalIP);
		System.out.println(XMLBroadIPRange);
		
		for (int i = 0; i < Ports.size(); i++)
		{
			System.out.println (Ports.get(i).InternalPort + ' ' + Ports.get(i).BroadIP + ' ' + Ports.get(i).BroadPort + ' ' + Ports.get(i).Filter + ' ' + Ports.get(i).StartByte + ' ' + Ports.get(i).NumBytes + ' ' + Ports.get(i).BigEndian);
			for (int j = 0; j < Ports.get(i).DestChange.size(); j++)
			{
				System.out.println(Ports.get(i).DestChange.get(j).DestID + " " + Ports.get(i).DestChange.get(j).newDestIP + " " + Ports.get(i).DestChange.get(j).newDestPort);
			}
		}
	}
	
	public static void printInfo(Packet P)
	{
		System.out.println("Packet Info: ");
		System.out.println(P.DestPort + ' ' + P.DestID);
		System.out.println();
		
		System.out.println("Config.txt Info: ");
		System.out.println("Internal IP: " + TrustInternalIP + " External IP: " + TrustExternalIP + " External Port: " + TrustExternalPort);
		System.out.println();
		
		System.out.println("config-trusted.xml Info: ");
		System.out.println("Broadcast Range: " + XMLBroadIPRange + " Local left-ip: " + leftIP + " Local left-id-ip: " + leftIDIP + " TCV_PARTNER_LINK_ADDR: " + tcvPartLinkAddr + " left-subnet: " + leftSubnet + '/' + leftSubnetRange);
		System.out.println();
		
		System.out.println("New Destination Info:");
		System.out.println(Ports.get(portIndex).InternalPort + ' ' + Ports.get(portIndex).BroadIP + ' ' + Ports.get(portIndex).BroadPort + ' ' + Ports.get(portIndex).Filter + ' ' + Ports.get(portIndex).StartByte + ' ' + Ports.get(portIndex).NumBytes + ' ' + Ports.get(portIndex).BigEndian);
		System.out.println(Ports.get(portIndex).DestChange.get(destIndex).DestID + " " + Ports.get(portIndex).DestChange.get(destIndex).newDestIP + " " + Ports.get(portIndex).DestChange.get(destIndex).newDestPort);
		System.out.println();
		
		System.out.println("New Packet Info: ");
		System.out.println("Destination ID: " + P.DestID + " Destination IP: " + P.DestIP + " Destination Port: " + P.DestPort);
	}
	
	//////////////////////////////////////////////////////////////////
	//																//
	//						END OF FUNCTIONS						//
	//																//
	//////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		Scanner input = new Scanner(System.in);
		String IP, Port;
		int i;
		boolean noErrors = true;
		
		System.out.println("Select the Trusted Configuration Directory");
		trusted = fileChooser("Choose trusted configuration file directory...");
		System.out.println("Select the Untrusted Configuration Directory");
		untrusted = fileChooser("Choose untrusted configuration file directory...");
		
		System.out.print("Enter a IP: ");
		IP = input.nextLine();
		System.out.print("Enter a Port: ");
		Port = input.nextLine();

		System.out.println();
		
		if (new File(trusted.getPath() + "\\MRU0").exists() && new File(untrusted.getPath() + "\\MRU0").exists())
		{
			i = 0;
		}
		else
		{
			i = 1;
		}
		
		while (new File(trusted.getPath() + "\\MRU" + i).exists() && new File(untrusted.getPath() + "\\MRU" + i).exists())
		//while (i <= 2)
		{
			Ports = new ArrayList<Port>();
			Connections = new ArrayList<Connection>();
			
			Packet P = new Packet (IP, Port);
			
			System.out.println("Configs for MRU" + i);
			
			if (new File((trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt")).exists())
			{
				trustedconfigtxt = new File (trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
				//System.out.println(trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
			}
			
			if (new File((trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml")).exists())
			{
				trustedconfigxml = new File (trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
				//System.out.println(trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
			}
			
			if (new File((untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml")).exists())
			{
				untrustedconfigxml = new File (untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
				//System.out.println(untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
			}
			
			trustedConfigXmlReader();
			trustedConfigTxtReader();
			untrustedConfigXmlReader();
			
			//printAll();
			if (!ruleChecker(P))
			{
				System.out.println(" in MRU" + i + " configuration");
				noErrors = false;
			}

			System.out.println();
			
			//printInfo(P);
			
			i++;
		}
		
		if (noErrors)
		{
			System.out.println("All configurations OK");
		}
		
		input.close();
		
	}

}
