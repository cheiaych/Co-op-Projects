//ConfigTester
//Jordan Hui 01/03/2019
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class ConfigTester {

	public static File trusted;
	public static File untrusted;
	
	public static File trustedconfigtxt; //Trusted side port forwarding config file
	public static File trustedconfigxml; //Trusted side config file
	public static File untrustedconfigxml; //Untrusted side config file
	public static File output;
	
	
	//Class for the packet that is input by the user
	public static class Packet
	{
		static String DestIP;
		static String SrcIP;
		static String DestPort;
		static String SrcPort;
		static String RcvID;
		static String DestID;
		
		Packet (String DIp, String SIp, String DPort, String SPort, String RId)
		{
			DestIP = DIp;
			SrcIP = SIp;
			DestPort = DPort;
			SrcPort = SPort;
			RcvID = RId;
		}
		
		Packet (String DPort, String DId)
		{
			DestPort = DPort;
			DestID = DId;
		}
		
		void setDest (String ip, String port)
		{
			DestIP = ip;
			DestPort = port;
		}
	}
	
	//Class for Broadcast Destinations from trusted Config.txt
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
	
	//Class for Zone Controllers from trusted Config.txt
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
	
	//Class for connection from trusted.xml
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
	//Values from untrusted Config.xml
	public static String XMLBroadIPRange, leftIP, leftIDIP, tcvPartLinkAddr, tcvPartNetMask, leftSubnet, leftSubnetRange, tcvPartAddr;
	public static String TrustInternalIP, TrustExternalIP, TrustExternalPort;
	public static String UntrustTcvBridgeAddr;
	public static int portIndex, destIndex;
	public static int connectionNo;
	public static String findConError;
	
//////////////////////////////////////////////////////////////////
//																//
//						END OF DEFINITIONS						//
//																//
//////////////////////////////////////////////////////////////////
	
	//Reads trusted Config.txt
	public static void trustedConfigTxtReader() throws FileNotFoundException
	{
		Scanner txtReader = new Scanner (trustedconfigtxt);
		String currentLine = ";";
		String[] splitLine;
		Boolean filter, bigEndian;
		Port tempPort;
		
		while (txtReader.hasNextLine())
		{
			//If line begins with 'P', read as Broadcast Destination
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

				//While next line has text and doesn't start with 'P', read as a Zone Controller line
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
			//Reading External IP and Port and Internal IP lines
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
	
	//Reads trusted Config.xml
	public static void trustedConfigXmlReader() throws Exception
	{
		String currentLine, IPLine = "", IPLine2;
		boolean routeFound = false;
		Scanner xmlReader = new Scanner(trustedconfigxml);
		
		while (xmlReader.hasNextLine())
		{
			//Gets broadcast IP
			if ((currentLine = xmlReader.nextLine()).contains("TCV_TRAIN_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				XMLBroadIPRange = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			//Gets left-ip, left-id-ip, and left-subnet
			if (currentLine.contains("<local>"))
			{
				IPLine = xmlReader.nextLine().trim();
				leftIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
				
				IPLine = xmlReader.nextLine().trim();
				leftIDIP = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
				
				//Separates range from subnet
				IPLine = xmlReader.nextLine().trim();
				leftSubnet = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("/"));
				leftSubnetRange = IPLine.substring(IPLine.indexOf("/") + 1, IPLine.indexOf("</"));
			}
			//Gets TCV Partner Link Address
			if (currentLine.contains("TCV_PARTNER_LINK_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartLinkAddr = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			//Gets TCV Partner Link Network Mask
			if (currentLine.contains("TCV_PARTNER_LINK_NETMASK"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartNetMask = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			//Gets TCV Partner IP Address
			if (currentLine.contains("TCV_PARTNER_ADDR"))
			{
				IPLine = xmlReader.nextLine().trim();
				tcvPartAddr = IPLine.substring(IPLine.indexOf(">") + 1, IPLine.indexOf("</"));
			}
			//Gets connection information
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
						//Seperates range from subnet
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
	
	//Reads untrusted Config.xml
	public static void untrustedConfigXmlReader () throws Exception
	{
		String currentLine;
		Scanner xmlReader = new Scanner(untrustedconfigxml);
		
		while (xmlReader.hasNextLine())
		{
			//Gets TCV Bridge Address
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

	//Checks if trusted config.txt, trusted config.xml, and untrusted config.xml meet all the configuration rules. returns true if they do, returns false and prints error if they don't. See Packet Filtering Rule.txt for details on the rules
	public static boolean ruleChecker(Packet P, int index, PrintWriter pw) throws Exception
	{
		boolean rule = false;
		String out;
		
		System.out.println("Checking if Trusted MRU Broadcast IP: " + XMLBroadIPRange + " Matches Trusted Port Fowarder Internal IP: " + TrustInternalIP);
		if (XMLBroadIPRange.equals(TrustInternalIP))
		{
			System.out.println("Checking if Trusted MRU left-IP: " + leftIP + ", Trusted MRU left-id-ip: " + leftIDIP + " and Trusted MRU TCV_PARTNER_LINK_ADDR: " + tcvPartLinkAddr + " Are Matching");
			if (leftIP.equals(leftIDIP) && leftIP.equals(tcvPartLinkAddr))
			{	
				System.out.println("Checking if Trusted Port Forwarder External IP: " + TrustExternalIP + " is in Trusted MRU left-subnet: " + leftSubnet + '/' + leftSubnetRange);
				if (subnetRange(P, leftSubnet, TrustExternalIP, leftSubnetRange))
				{
					System.out.println("Finding New Destination in Port Fowarder Configuration Using Packet Destination Port: " + P.DestPort + " and Packet Destination ID: " + P.DestID);
					if ((rule = findNewDest(P) == true))
					{
						System.out.println("Finding Connection Route using Packet's Destination IP: " + P.DestIP);
						if (findConnection(P) == true)
						{
							System.out.println("Checking if TCV_PARTNER_LINK_ADDR: " + tcvPartLinkAddr + " and MRU Config Connection right-ip: " + Connections.get(connectionNo).rightip + " are in Subnet: " + tcvPartNetMask + " (TCV_PARTNER_LINK_NETMASK)");
							if (subnetRange(P, tcvPartLinkAddr, Connections.get(connectionNo).rightip, subnetMaskToRange(tcvPartNetMask.toCharArray())))
							{
								System.out.println("Matching Untrusted XML TCV Bridge Address: " + UntrustTcvBridgeAddr + " to Trusted XML tcvPartAddress: " + tcvPartAddr);
								if (UntrustTcvBridgeAddr.equals(tcvPartAddr))
								{
									System.out.println("New Packet Info: Destination ID: " + P.DestID + " Destination IP: " + P.DestIP + " Destination Port: " + P.DestPort);
									out = (P.DestID + "," + P.DestIP + "," + P.DestPort);
								}
								else
								{
									System.out.print("ERROR: 'TCV_PARTNER_ADDR does not match TCV_BRIDGE_ADD'");
									out = ("ERROR: 'TCV_PARTNER_ADDR does not match TCV_BRIDGE_ADD'");
									rule = false;

								}
									
							}
							else
							{
								System.out.print("ERROR: 'right-ip and TCV_PARTNER_LINK_ADDR are not in subnet TCV_PARTNER_LINK_NETMASK'");
								out = ("ERROR: 'right-ip and TCV_PARTNER_LINK_ADDR are not in subnet TCV_PARTNER_LINK_NETMASK'");
								rule = false;

							}
						}
						else
						{
							System.out.print("ERROR: '" + findConError + "'");
							out = ("ERROR: '" + findConError + "'");
							rule = false;

						}
					}
					else
					{
						System.out.print("ERROR: 'Destination port and ID not found in trusted config.txt'");
						out = ("ERROR: 'Destination port and ID not found'");
						rule = false;

					}
				}
				else
				{
					System.out.print("ERROR: 'External IP is in not Left Subnet Range'");
					out = ("ERROR: 'External IP is in not Left Subnet Range'");
					rule = false;

				}
			}
			else
			{
				System.out.print("ERROR: 'LeftIP LeftIDIP and TCV Partner Link Address do not match'");
				out = ("ERROR: LeftIP LeftIDIP and TCV Partner Link Address do not match");
				rule = false;

			}
		}	
		else
		{
			System.out.print("ERROR: 'BroadcastIP does not match Internal IP (Eth.0 Trusted Range)'");
			out = ("ERROR: BroadcastIP does not match Internal IP (Eth.0 Trusted Range)");
			rule = false;

		}
		pw.println(index + "," + out);
		return rule;
	}
	
	//Finds Broadcast Destination and Zone Controller in trusted Config.txt that match the input packet
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
	
	//Finds activated route in Trusted config.xml that matches the input packet
	private static boolean findConnection(Packet P) throws Exception
	{
		int matchConnect = 0, index = -1;
		
		for (int i = 0; i < Connections.size(); i++)
		{
			//System.out.println(Connections.get(i).rightsubnet + '/' + Connections.get(i).rightsubrange + ' ' + P.DestIP);
			if (Connections.get(i).activated.equals("route") && subnetRange(P, Connections.get(i).rightsubnet, P.DestIP, Connections.get(i).rightsubrange))
			{
				index = i;
				matchConnect++;
			}
			//System.out.println("Matching connections: " + matchConnect);
		}
		if (matchConnect > 1)
		{
			findConError = ("Multiple matching active connections in trusted config.xml");
			matchConnect = 0;
			return false;
		}
		else if (matchConnect == 0)
		{
			findConError = ("No matching connections in trusted config.xml or Destination IP is not in Right Subnet Range");
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
	
//////////////////////////////////////////////////////////////////
//																//
//							UTILITIES							//
//																//
//////////////////////////////////////////////////////////////////
	
	//Method for file chooser
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
	
	//Converts an IP address to binary 
	public static char[] IPtoBinary (String IP) throws Exception
	{
		//Converts string to Inet IP address
		InetAddress octIP = InetAddress.getByName(IP);
		String binary = "", tempbin;
		
		//System.out.println(IP);
		
		//Converts octIP to binary
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
	
	//Checks if a binary IP is within a subnet range with a String range
	private static boolean subnetRange (Packet P, String subnet, String host, String range) throws Exception
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
	
	//Checks if a binary IP is within a subnet range with an Int range
	private static boolean subnetRange (Packet P, String subnet, String host, int range) throws Exception
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
	
	//Gets the subnet range from a binary subnet mask
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
	
	//Outputs all the information from Trusted config.txt, as well the Broadcast IP range from trusted config.xml
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
	
	//Outputs data from trusted config.txt and trusted config.xml, as well as the input packet, converted packet, and the converted packet's destination
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
	
	public static void run(File t, File u) throws Exception {
		
		trusted = t;
		untrusted = u;
		output = new File (trusted.getParentFile().getAbsolutePath() + "\\ConfigTestOutput.csv");
		
		Scanner input = new Scanner(System.in);
		String Port, ID, trustedFilePath, untrustedFilePath;
		int i;
		boolean noErrors = true;
		
		//Gets packet info from user
		System.out.print("Enter a destination port: ");
		Port = input.nextLine();
		System.out.print("Enter a destination ID: ");
		ID = input.nextLine();

		System.out.println();
		
		//Starts at either MRU0 or MRU1 depending on which is first in the directory
		if (new File(trusted.getPath() + "\\MRU0").exists() && new File(untrusted.getPath() + "\\MRU0").exists())
		{
			i = 0;
		}
		else
		{
			i = 1;
		}
		
		PrintWriter pw = new PrintWriter(output, "UTF-8");
		
		//Outputs information on input packet
		pw.println("Destination Port: " + Port + ",Destination ID: " + ID);
		pw.println("MRU,New Destination ID,New Destination IP,New Destination Port");
		
		//Runs for each MRU, will end if MRU numbers aren't consecutive
		while (new File(trusted.getPath() + "\\MRU" + i).exists() && new File(untrusted.getPath() + "\\MRU" + i).exists())
		//while (i <= 2)
		{
			Ports = new ArrayList<Port>();
			Connections = new ArrayList<Connection>();
			
			Packet P = new Packet (Port, ID);
			
			System.out.println("Configs for MRU" + i);
			
			//Gets trusted config.txt, trusted config.xml, and untrusted config.xml and reads them
			if (new File((trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt")).exists())
			{
				trustedconfigtxt = new File (trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
				//System.out.println(trusted.getPath() + "\\MRU" + i + "\\Config-MRU" + i + "_trusted.txt");
			}
			else
			{
				System.out.println("Could not find Config-MRU" + i + "_trusted.txt for MRU" + i);
				System.exit(0);
			}
			
			if (new File((trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml")).exists())
			{
				trustedconfigxml = new File (trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
				//System.out.println(trusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_trusted.xml");
			}
			else
			{
				System.out.println("Could not find config-MRU" + i + "_trusted.xml for MRU" + i);
				System.exit(0);
			}
			
			if (new File((untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml")).exists())
			{
				untrustedconfigxml = new File (untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
				//System.out.println(untrusted.getPath() + "\\MRU" + i + "\\config-MRU" + i + "_untrusted.xml");
			}
			else
			{
				System.out.println("Could not find config-MRU" + i + "_untrusted.xml for MRU" + i);
				System.exit(0);
			}
			
			//Reads the trusted and untrusted Config XML files and the trusted Config TXT
			trustedConfigXmlReader();
			trustedConfigTxtReader();
			untrustedConfigXmlReader();
			
			//printAll();
			
			//Outputs if there are errors in configuration files
			if (!ruleChecker(P, i, pw))
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
		pw.close();
		input.close();
	}	
}
