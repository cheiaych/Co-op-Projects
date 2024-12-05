//PortForwardReader
//Jordan Hui 01/03/2019
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class PortForwardReader {

	public static File trusted;
	
	static String[] directories;
	
	public static File PortForwardtxt; //Trusted side port forwarding config file
	public static File output;
	
	static ArrayList<MRU> MRUS = new ArrayList<MRU>();
	static ArrayList<Destination> FirstDest;
	
	public static int noOfMRU;
	
	//class for each Config file
	public static class MRU
	{
		String Name, internalIP, externalIP, externalPort;
		ArrayList<Destination> Destinations = new ArrayList<Destination>();
		
		MRU(String name)
		{
			Name = name;
		}
	}
	
	//class for Broadcast Destinations
	public static class Destination
	{
		String InternalPort, BroadIP, BroadPort, StartByte, NumBytes;
		Boolean Filter, BigEndian;
		ArrayList<ZoneController> DestChange = new ArrayList<ZoneController>();
		
		Destination (String IntPort, String BroIp, String BroPort, Boolean Filt, String StrByte, String NBytes, Boolean Endian)
		{
			InternalPort = IntPort;
			BroadIP = BroIp;
			BroadPort = BroPort;
			Filter = Filt;
			StartByte = StrByte;
			NumBytes = NBytes;
			BigEndian = Endian;
		}
		
		Destination (String intPort)
		{
			InternalPort = intPort;
		}
	}
	
	//class for new Zone Controller destination after port forward
	public static class ZoneController
	{
		String ZCID, ZCIP, ZCPort;
		
		ZoneController (String Id, String Ip, String Port)
		{
			ZCID = Id;
			ZCIP = Ip;
			ZCPort = Port;
		}	
	}
	
	//////////////////////////////////////////////////////////////////////////////
	//																			//
	//							End Of Declarations								//
	//																			//
	//////////////////////////////////////////////////////////////////////////////
	
	//Reads config file and adds data to MRUs arraylist
	public static void PortForwardReader(MRU m) throws Exception
	{
		Scanner txtReader = new Scanner (PortForwardtxt);
		ArrayList<String> tempAL = new ArrayList<String>();
		String currentLine = ";", tempKey, tempVal;
		String[] splitLine;
		Boolean filter, bigEndian;
		Destination tempDes;
		
		//System.out.println(m.Name);
		//System.out.println(PortForwardtxt.getName());
		while (txtReader.hasNextLine())
		{
			//If line starts with P, read it as Broadcast Destination line
			if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'P')
			{
				splitLine = currentLine.split("\\s+");
				//Converting 0 and 1 values foe Filter and BigEndian to true or false
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
				//Hold the data in a temporary Broadcast Destination object
				tempDes = new Destination(splitLine[1], splitLine[2], splitLine[3], filter, splitLine[5], splitLine[6], bigEndian);

				currentLine = txtReader.nextLine();

				//Reads every line after the Broadcast Destination line that isn't empty and doesn't start with a P as a Zone Controller for that Broadcast Destination
				while (txtReader.hasNextLine() && ((currentLine.trim().isEmpty()) || (currentLine.charAt(0) != 'P')))
				{
					if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) != ';')
					{
						splitLine = currentLine.split("\\s+");	
						//Adds Zone Controller to arrayList of Zone Controllers in temporary Broadcast Destination
						tempDes.DestChange.add(new ZoneController(splitLine[0] , splitLine[1], splitLine[2]));
					}
					currentLine = txtReader.nextLine();
				}
				//Adds temporary Broadcast Destination to arrayList of Broadcast Destinations
				m.Destinations.add(tempDes);
			}
			else
			{
				//If line starts with E, read as External Ip and Port line
				if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'E')
				{
					splitLine = currentLine.split("\\s+");
					m.externalIP = splitLine[1];
					m.externalPort = splitLine[2];
				}
				
				//If line starts with I, read as Internal Ip line
				if (!(currentLine.trim().isEmpty()) && currentLine.charAt(0) == 'I')
				{
					splitLine = currentLine.split("\\s+");
					m.internalIP = splitLine[1];
				}		
				currentLine = txtReader.nextLine();
			}
		}
		
		//System.out.println("End of File " + PortForwardtxt.getName());
		txtReader.close();
	}
	
	//Find the first occurrence of each Broadcast Destination, as well as each New Destination under them, and add them to FirstDest
	private static ArrayList<Destination> FirstDestReader(ArrayList<MRU> MRUS)
	{
		int d;
		Destination tempDest;
		
		//First Destination arrayList
		ArrayList<Destination> FD = new ArrayList<>();
		
		for (int i = 0; i < MRUS.size(); i++)
		{
			for (int j = 0; j < MRUS.get(i).Destinations.size(); j++)
			{
				//if FirstDest is empty, add the first Broadcast Destination to FD
				if (FD.size() == 0)
				{
					tempDest = new Destination(MRUS.get(i).Destinations.get(j).InternalPort, MRUS.get(i).Destinations.get(j).BroadIP, MRUS.get(i).Destinations.get(j).BroadPort, MRUS.get(i).Destinations.get(j).Filter, MRUS.get(i).Destinations.get(j).StartByte, MRUS.get(i).Destinations.get(j).NumBytes, MRUS.get(i).Destinations.get(j).BigEndian);
					tempDest.DestChange = MRUS.get(i).Destinations.get(j).DestChange;
					FD.add(tempDest);
				}
				else
				{
					//If Broadcast Destination isn't in FD, add it
					if ((d = DestArrayContainsInternalPort(MRUS.get(i).Destinations.get(j).InternalPort, FD)) == -1)
					{
						tempDest = new Destination(MRUS.get(i).Destinations.get(j).InternalPort, MRUS.get(i).Destinations.get(j).BroadIP, MRUS.get(i).Destinations.get(j).BroadPort, MRUS.get(i).Destinations.get(j).Filter, MRUS.get(i).Destinations.get(j).StartByte, MRUS.get(i).Destinations.get(j).NumBytes, MRUS.get(i).Destinations.get(j).BigEndian);
						tempDest.DestChange = MRUS.get(i).Destinations.get(j).DestChange;
						FD.add(tempDest);
					}
					else
					{
						//If Broadcast Destination is in FD, check if all the Zone Controllers associated with that Broadcast Destination are in the FD array. If not, it adds it to the associated Broadcast Destination
						for (int k = 0; k < MRUS.get(i).Destinations.get(j).DestChange.size(); k++)
						{
							if (DestChangeContainsID(d, MRUS.get(i).Destinations.get(j).DestChange.get(k).ZCID, FD) == -1)
							{
								FD.get(d).DestChange.add(MRUS.get(i).Destinations.get(j).DestChange.get(k));
							}
						}	
					}
				}
			}
		}
		//Returns First Destination arrayList
		return FD;
	}
	
	//Checks if FD contains a Broadcast Destination using the Broadcast Destination's internal port
	private static int DestArrayContainsInternalPort(String intport, ArrayList<Destination> FD)
	{
		for (int i = 0; i < FD.size(); i++)
		{
			if ((FD.get(i).InternalPort).equals(intport))
			{
				return i;
			}
		}
		return -1;
	}
	
	//Checks if a BroadcastDestination in the FD contains a Zone Controller
	private static int DestChangeContainsID(int destIndex, String ZCID, ArrayList<Destination> FD)
	{
		for (int i = 0; i < FD.get(destIndex).DestChange.size(); i++)
		{
			if (FD.get(destIndex).DestChange.get(i).ZCID.equals(ZCID))
			{
				return i;
			}
		}
		return -1;
	}
	
	//Prints all the data from the MRUs arrayList, as well as FirstDest
	public static void PrintAll()
	{			
		for (int a = 0; a < MRUS.size(); a++)
		{
			System.out.println("\n\n\n" + MRUS.get(a).Destinations.size());
			System.out.println(MRUS.get(a).Name);
			System.out.println(MRUS.get(a).internalIP);
			System.out.println(MRUS.get(a).externalIP + ' ' + MRUS.get(a).externalPort);
			for (int i = 0; i < MRUS.get(a).Destinations.size(); i++)
			{
				System.out.println (MRUS.get(a).Destinations.get(i).InternalPort + ' ' + MRUS.get(a).Destinations.get(i).BroadIP + ' ' + MRUS.get(a).Destinations.get(i).BroadPort + ' ' + MRUS.get(a).Destinations.get(i).Filter + ' ' + MRUS.get(a).Destinations.get(i).StartByte + ' ' + MRUS.get(a).Destinations.get(i).NumBytes + ' ' + MRUS.get(a).Destinations.get(i).BigEndian);
				for (int j = 0; j < MRUS.get(a).Destinations.get(i).DestChange.size(); j++)
				{
					System.out.println(MRUS.get(a).Destinations.get(i).DestChange.get(j).ZCID + " " + MRUS.get(a).Destinations.get(i).DestChange.get(j).ZCIP + " " + MRUS.get(a).Destinations.get(i).DestChange.get(j).ZCPort);
				}
			}
		}
		
		System.out.println("\n\n\nFirstDest\n" + FirstDest.size());
		for (int i = 0; i < FirstDest.size(); i++)
		{
			System.out.println (i + " " + FirstDest.get(i).InternalPort);
			for (int j = 0; j < FirstDest.get(i).DestChange.size(); j++)
			{
				System.out.println(FirstDest.get(i).DestChange.get(j).ZCID + " " + FirstDest.get(i).DestChange.get(j).ZCIP + " " + FirstDest.get(i).DestChange.get(j).ZCPort);
			}
		}
	}
	
	/*public static void CheckAll()
	{
		for (int a = 0; a < MRUS.size(); a++)
		{
			System.out.println(MRUS.get(a).Name);
			for (int i = 0; i < MRUS.get(a).Destinations.size(); i++)
			{
				System.out.println(MRUS.get(a).Destinations.get(i).InternalPort);
				System.out.println (dynamicMRUs.get(MRUS.get(a).Destinations.get(i).InternalPort));
				for (int j = 0; j < dynamicMRUs.get(MRUS.get(a).Destinations.get(i).InternalPort).size(); j++)
				{
					System.out.print(ContainsPort(MRUS.get(a).Destinations.get(i).DestChange, dynamicMRUs.get(MRUS.get(a).Destinations.get(i).InternalPort).get(j)));
					System.out.print(' ');
				}
				System.out.println();
			}
		}
	}*/

	//Outputs the data to csv
	public static void PrintToCSV() throws Exception
	{	
		int index = 0;
		PrintWriter pw = new PrintWriter(output, "UTF-8");
		String destinationPorts = "Destination Port,,", zcs = "Application ID,,", line;
		Destination tempDest = null;
		
		//Creates the 2 header rows
		for (int x = 0; x < FirstDest.size(); x++)
		{
			//System.out.println(FirstDest.get(x).InternalPort);
			destinationPorts += FirstDest.get(x).InternalPort;
			for (int y = 0; y < FirstDest.get(x).DestChange.size(); y++)
			{
				destinationPorts += ",,";
				zcs += FirstDest.get(x).DestChange.get(y).ZCID + ",IP: " + FirstDest.get(x).DestChange.get(y).ZCIP + " Port: " + FirstDest.get(x).DestChange.get(y).ZCPort + ',';
			}
		}
		
		//adds the header rows to the csv
		pw.println(destinationPorts);
		pw.println(zcs);
		
		
		//checks if each MRU config.txt contains the Broadcast Destinations and Zone Controllers from the First Destination arrayList
		for (int a = 0; a < MRUS.size(); a++)
		{
			line = "MRU,";
			line += (MRUS.get(a).Name);
			line += ",";
						
			for (int i = 0; i < FirstDest.size(); i++)
			{
				for (int j = 0; j < FirstDest.get(i).DestChange.size(); j++)
				{
					tempDest = FindDest(FirstDest.get(i).InternalPort.toString(), MRUS.get(a).Destinations);
					
					//If Broadcast Destination in First Destination is in config.txt, print empty cell to csv, if not, print FALSE
					if ((index = ContainsPort(tempDest.DestChange, FirstDest.get(i).DestChange.get(j).ZCID)) != -1)
					{
						//line += "True";
						line += ",";
						//If Broadcast Destination in First Destination is in config.txt, print empty cell to csv, if not, print FALSE
						if(matchIPandPort(FirstDest.get(i).DestChange.get(j), tempDest.DestChange.get(index)))
						{
							//line += "True";
						}
						else
						{
							line += "False";
						}
						line += ",";
					}
					else
					{
						line += "False";
						line += ",";
						line += "False";
						line += ",";
					}					
				}
			}
			//Adds row to csv
			pw.println(line);
		}
		pw.close();
	}
	
	//Finds a Broadcast Destination in the First Destination arrayList using a Broadcast Destination ID
	private static Destination FindDest(String x, ArrayList<Destination> d)
	{
		for (int i = 0; i < d.size(); i++)
		{
			if (d.get(i).InternalPort.toString().equals(x))
			{
				return d.get(i);
			}
		}
		return new Destination("", "", "", false,"" ,"",false);
	}
	
	//Checks if any of the Zone Controllers under a Broadcast Destination contains a certain Destination ID
	private static int ContainsPort(ArrayList<ZoneController> Dest, String ZCID2)
	{	
		for (int i = 0; i < Dest.size(); i++)
		{
			if (Integer.parseInt(Dest.get(i).ZCID) == Integer.parseInt(ZCID2))
			{
				return i;
			}
		}
		return -1;
	}
	
	//Returns true if Destination ID and Destination IP in a Zone Controller match another Zone Controller's ID and IP
	private static boolean matchIPandPort(ZoneController zc1, ZoneController zc2)
	{
		return (zc1.ZCIP.equals(zc2.ZCIP) && zc1.ZCPort.equals(zc2.ZCPort));
	}
	
	
	//The main method
	public static void run(File t) throws Exception {
		// TODO Auto-generated method stub
		
		trusted = t;
		output = new File (trusted.getParentFile().getAbsolutePath() + "\\PortForwardOutput.csv");
		
		//Gets list of directories under trusted directory
		directories = trusted.list(new FilenameFilter() {
			  @Override
			  public boolean accept(File current, String name) {
			    return new File(current, name).isDirectory();
			  }
			});
		
		int i, index = 0;
		
		//reads the config.txt from each MRU and adds the data to the MRUs arrayList
		for (String dir : directories)
		{
			i = Integer.parseInt(dir.replaceAll("[^\\d.]", ""));
			PortForwardtxt = new File(trusted + "\\" + dir + "\\Config-MRU" + i + "_trusted.txt");
			MRUS.add(new MRU ("MRU " + i));
			System.out.println("\n\nMRU" + i + "\n\n" + index + "\n\n");
			PortForwardReader(MRUS.get(index));
			index++;
		}
		
		//Gets the first occurrence of each Broadcast Destination and Zone Controller and adds it to the FirstDest arrayList
		FirstDest = FirstDestReader(MRUS);
		
		index = 0;
		MRUS = new ArrayList<MRU>();
		
		//re-reads the config.txt from each MRU and adds the data to the MRUs arrayList (there is a bug that overwrites the MRUs arrayList that occurs when the FirstDest arrayList is made. Re-making the list fixes this)
		for (String dir : directories)
		{
			i = Integer.parseInt(dir.replaceAll("[^\\d.]", ""));
			PortForwardtxt = new File(trusted + "\\" + dir + "\\Config-MRU" + i + "_trusted.txt");
			MRUS.add(new MRU (Integer.toString(i)));
			System.out.println("\n\nMRU" + i + "\n\n" + index + "\n\n");
			PortForwardReader(MRUS.get(index));
			index++;
		}
			
		//Prints the data from MRUs and FirstDest, then creates the csv
		PrintAll();
		//CheckAll();
		//System.out.println(dynamicMRUs.keySet());
		PrintToCSV();
	}
}
