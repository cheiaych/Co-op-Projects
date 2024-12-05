//LogReader v1.0
//Jordan Hui 15/02/2019
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class LogReader {

	public static File output;
	
	public static File chainageTable;
	public static List<String> toChainage = new ArrayList<>();
	
	public static List<String> log = new ArrayList<>();
	public static String dateStr, vcc;
	public static Date date;
	public static DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
	public static DateFormat stf = new SimpleDateFormat("HH:mm:ss");
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		//Finding first TSNUM_to_Chainage file
		File homeDir = new File(".\\");
		File[] TSNUM2Chainage = null;
		
		//Uses first TSNUM_to_Chainage.csv in program dir, ends appliaction if none are found
		boolean toChainageExists = checkForFiles(homeDir, "_TSNUM_to_Chainage.csv").length > 0;
		if (!toChainageExists)
		{
			System.out.println("*_TSNUM_to_Chainage.csv file does not exist in application directory");
			return;
		}
		TSNUM2Chainage = homeDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
		        return name.endsWith("_TSNUM_to_Chainage.csv");
		    }
		});
		
		//Uses first Station_and_Chainage.xlsx in program dir, ends application if none are found
		boolean stationChainageExists = checkForFiles(homeDir, "_Stations_and_Chainage.xlsx").length > 0;
		if (!stationChainageExists)
		{
			System.out.println("*_Stations_and_Chainage.xlsx file does not exist in application directory");
			return;
		}
		
		//Sets TSNUM2Chainage to first _TSNUM_to_Chainage.csv in same folder as application
		//System.out.println(TSNUM2Chainage[0].getPath());
		chainageTable  = TSNUM2Chainage[0];
		
		String logFolder;
		File dir = null;
		
		//Reading Korea_TSNUM_to_Chainage.csv and adding the data to the toChainage arraylist
		Scanner chainageTableReader = new Scanner(chainageTable).useDelimiter(",");
		
		while (chainageTableReader.hasNextLine())
		{
			toChainage.add(chainageTableReader.nextLine());
		}
		
		chainageTableReader.close();
		
		//Getting log directory
		Scanner input = new Scanner(System.in);
		System.out.println("Enter the directory of the Log folder: ");
		logFolder = fileChooser("Choose log containing directory...").getAbsolutePath();
		
		//Creates Output.csv and adds headers
		output = new File(".\\Output.csv");
		//System.out.println(output.getAbsolutePath());
		
		FileWriter fw = new FileWriter(output, false);
		PrintWriter pw = new PrintWriter(fw);
		pw.println("VCC,Date,Time,VOBC,Train Number,Position,Time Difference Between Time-Out and Last Known Position (Seconds),Direction,Chainage (Meters),Last Known Station");
		pw.close();
		
		dir = new File(logFolder);
	
		//Checks if any txt files exist in selected directory, exits if there aren't
		boolean txtFilesExist = checkForFiles(dir, ".txt").length > 0;
		if (!txtFilesExist)
		{
			System.out.println("No log files in selected directory");
			return;
		}
		//Array of files to hold list of log files in directory
		File[] listofLogs = dir.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".txt");
		    	}
			}
		);
		
		//Gets VCCs to read
		System.out.print("Enter the VCC numbers you want to read logs for, seperated by commas (ie: 1,2,3): ");
		String vccString = input.nextLine().replaceAll("\\s+", "");
		//System.out.println(vccString);
		String[] vccs = vccString.split(",");
		
		for(String vccNo : vccs)
		{
		//Checks for each VCC log file in the directory, gets date and VCC number from filename, replaces all * with whitespace, then calls the readLog method for the file
			for(File file : listofLogs)
			{
				dateStr = "";
				if (file.getName().contains("VCC" + vccNo))
				{
					vcc = file.getName().substring(0, 4);
					dateStr = file.getName().substring(5,9) + "/" +  file.getName().substring(9,11) + "/" + file.getName().substring(11,13);
					date = sdf.parse(dateStr);
					
					Scanner logReader = new Scanner(file);
					
					while (logReader.hasNextLine())
					{
						log.add(logReader.nextLine().replace('*',  ' '));
					}
						
					readLog();
					logReader.close();
				}
			}
		}
		
		//Runs XLSXCreator
		System.out.println("Creating excel with graphs");
		Process XLSXCreator = new ProcessBuilder("XLSXCreator.exe", output.getAbsolutePath()).start();
		
		XLSXCreator.waitFor();
		//Notifies user when the program is finished
		System.out.println("Done");
		
		input.close();
		fw.close();
		pw.close();
	}	
	
	//Checks for files containing the search string in their filename
	public static File[] checkForFiles(File dir, String search)
	{
		return dir.listFiles(new FileFilter() {
	        public boolean accept(File pathname) {
	            return pathname.getName().toLowerCase().contains(search.toLowerCase());
	        }
	    });
	}
	
	//Opens file browser window
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
	
	public static void readLog() throws Exception
	{
		//Arrays of Strings to store whitespace separated versions of a line in a log
		String[] currentLine, searchLine, chainageTableLine;
		
		//Holds the list of track sections for a loop from the TSNUM_to_Chainage.csv
		String chainageTableTrackSec;
		
		//Strings to store data before outputting it to the csv
		String time, posTime, train, vobc, direction = null, position = "-1", station = "";
		
		//Formatted versions of time and posTime stored in a Date object
		Date timeFormatted, posTimeFormatted;
		
		//Difference in time between time of the Time-Out Failure and the time of the closest reported position
		long timeDiff;
		
		//Integers to store the amount of lines to the closest reported positions before and after the Time-Out Failure Report, Track Section and Loop Distance from the position, and the Associated Track Section, End of Track Section and the Position from the Chainage table
		int backPos = -1, forPos = -1, trackSec, loopDist, tableTrackSec, tableEndOfSec;
		
		//Double to store the calculated Chainage in meters
		double chainage = 0 , tablePosition;
		
		//boolean determining whether or not the position is included in the line reporting the Time-Out Failure
		boolean posOnLine = false;
		
		//Runs for every txt file in the log directory
		for(int i = 0; i < log.size(); i++) 
		{
			position = "-1"; 
			station = "";
			backPos = -1;
			forPos = -1;
			chainage = 0;
			
			//Check every line for a Time-Out Failure report; only CPU1's report is used to prevent repeat reports
			if (log.get(i).contains("TIME-OUT FAILURE") && log.get(i).contains("CPU1") && log.get(i).contains("VOBC") && log.get(i).contains("PAR = 2501"))
			{
				System.out.println(log.get(i));
				//Splits the line at every whitespace and stores it in the currentLine array
				currentLine = log.get(i).split("\\s+");
				
				//Train Number
				//Finds the index of the phrase "TRAIN" and takes the next element as the train number
				train = currentLine[Arrays.asList(currentLine).indexOf("TRAIN") + 1];
				
				//Time of Report
				//Takes the second element of the whitespace-separated array and stores it as the time of the Time-Out Failure report, then formats it to be stored in a Date object
				time = currentLine[1];
				timeFormatted = stf.parse(time);
				
				//VOBC
				//If the Time-Out Failure report line contains information on which VOBC Timed-Out, it records the VOBC's number, otherwise it is assumed both VOBCs Timed-Out, and the train's number is used as stored to represent this
				if (log.get(i).contains("VOBC"))
				{
					vobc = currentLine [Arrays.asList(currentLine).indexOf("VOBC") + 1];
				}
				else
				{
					vobc = train;
				}
				
				//Position
				//Checks if the Time-Out report line contains information on the train's position, and searches of the closest reported position of it does not
				if (log.get(i).contains("POS"))
				{
					//Takes an 8 character substring 4 characters after the phrase "POS" as the position
					position = log.get(i).substring(log.get(i).indexOf("POS") + 4, (log.get(i).indexOf("POS") + 12));
					posTime = log.get(i).substring(8, 16);
					//Saves that the position is stored on the same line as the Time-Out Failure
					posOnLine = true;
				}
				else
				{
					posOnLine = false;
					for (int a = i; a > 0; a--) //searches for closest reported position of train until beginning of log
					{
						//Determines lines to closest previously reported location for the train
						searchLine = log.get(a).trim().split("\\s+");	
						if (searchLine.length > 5 && searchLine[2].equals(train) && !log.get(a).contains("SR"))
						{
							backPos = a;
		
							break;
						}
					}
					for (int b = i; b < log.size(); b++)
					{
						//Determines lines to closest reported location after the Time-Out Report for the train
						searchLine = log.get(b).split("\\s+");
						if (searchLine.length > 11 && searchLine[2].equals(train) && !log.get(b).contains("SR"))
						{
							forPos = b;
							break;
						}
					}
					
					// Determines which is the closest log line to the time-out line, then takes an 8 character substring from char 34 in the log line as the position
					//System.out.println(forPos + " and " + backPos);
					if (((i - backPos) > (forPos - i)) && forPos != -1)
					{
						position = log.get(forPos).substring(34, 42);
						posTime = log.get(forPos).substring(8, 16); 
					}
					else if (((i - backPos) < (forPos - i)) && backPos != -1)
					{
						position = log.get(backPos).substring(34, 42);
						posTime = log.get(backPos).substring(8, 16);
					}
					else //If both are the same amount of lines away from the report, it uses the earlier one
					{
						position = log.get(backPos).substring(34, 42);
						posTime = log.get(backPos).substring(8, 16);
					}
				}
				
				//Determines the time between the Time-Out Report and the closest report in milliseconds and converts it to seconds
				posTimeFormatted = stf.parse(posTime);
				timeDiff = (posTimeFormatted.getTime() - timeFormatted.getTime()) / 1000;
				
				//Parses the Associated Track Section and the Loop Distance from the position
				trackSec = Integer.parseInt(position.replaceAll("\\s+", "").split("[\\/\\-]")[0]);
				loopDist = Integer.parseInt(position.replaceAll("\\s+", "").split("[\\/\\-]")[1]);
				
				//Direction
				//If the position contains '/' then the train was traveling up (GD0), if it contains '-', it was traveling down (GD1)
				if (position.contains("-"))
				{
					direction = "GD1";
				}
				else if (position.contains("/"))
				{
					direction = "GD0";
				}
				
				//Chainage
				//Searches for the loop containing the track section to calculate the Chainage in meters, see VCC Chainage Calculation for equation
				for (int k = 1; k < toChainage.size(); k++)
				{
					chainageTableLine = toChainage.get(k).split(",");
					chainageTableTrackSec = chainageTableLine[3];
					//Checks if row contains track section
					if (chainageTableTrackSec.contains(Integer.toString(trackSec)) && direction.equals(chainageTableLine[2]))
					{
						//Checks if row is GD0 or GD1
						if(direction.equals("GD0"))
						{
							//System.out.println(toChainage.get(k));
							//System.out.println(chainageTableLine[1] + ',' + chainageTableLine[2]);
							tablePosition = Double.parseDouble(chainageTableLine[5]);
							chainage = tablePosition + (loopDist * 6.25);
							//System.out.println(tablePosition + " and " + loopDist);
						}
						else
						{
							//System.out.println(toChainage.get(k));
							//System.out.println(chainageTableLine[1] + ',' + chainageTableLine[2]);
							tablePosition = Double.parseDouble(chainageTableLine[5]);
							chainage = tablePosition - (loopDist * 6.25);
							//System.out.println(tablePosition + " and " + loopDist);
						}
						
					}
					
					/*tableTrackSec = Integer.parseInt(chainageTableLine[2]);
					tableEndOfSec = Integer.parseInt(chainageTableLine[3]);
					if (chainageTableLine[1].equals(direction))
					{
						if (trackSec >= tableTrackSec && trackSec <= tableEndOfSec)
						{
							tablePosition = Integer.parseInt(chainageTableLine[4]);
							
							chainage = tablePosition + (loopDist * 6.25);
						}
					}*/
				}
				
				//Station
				//If last station isn't on the time-out line, finds log lines with matching train number, then checks for station data. Station is left blank if there is none
				if (posOnLine == false)
				{
					//if next line forwards is closer than next line backwards
					if (((i - backPos) > (forPos - i)) && forPos != -1)
					{
						if(log.get(forPos).contains("STN-"))
						{
							station = log.get(forPos).substring(log.get(forPos).indexOf("STN") + 4, log.get(forPos).indexOf("STN") + 8);
						}
					}
					//if next line backwards is closer than next line forwards
					else if (((i - backPos) < (forPos - i)) && backPos != -1)
					{
						if(log.get(backPos).contains("STN-"))
						{
							station = log.get(backPos).substring(log.get(backPos).indexOf("STN") + 4, log.get(backPos).indexOf("STN") + 8);
						}
					}
					else
					{
						//Uses next line backwards if the forward and backward line are the same amount of lines away
						if(log.get(backPos).contains("STN-"))
						{
							station = log.get(backPos).substring(log.get(backPos).indexOf("STN") + 4, log.get(backPos).indexOf("STN") + 8);
						}
					}
				}
				//if last station is on same line as time-out, gets last station
				else
				{
					for (int l = i; l < log.size() && l > 0; l--)
					{
						searchLine = log.get(l).split("\\s+");
						if (searchLine.length > 11 && searchLine[2].equals(train))
						{
							if(log.get(l).contains("STN-"))
							{
								station = log.get(l).substring(log.get(l).indexOf("STN") + 4, log.get(l).indexOf("STN") + 8);
							}
							break;
						}
					}
				}
				
				
				//Outputs all data to the console
				System.out.print(vcc + " " + sdf.format(date) + " " + stf.format(timeFormatted) + " " + train + " " + vobc + " " + position + " " + stf.format(posTimeFormatted) + " " + timeDiff + " " + trackSec + " " + loopDist + " " + direction + " " + chainage + " " + station);	
				System.out.println();
				
				FileWriter fw = new FileWriter(output, true);
				PrintWriter pw = new PrintWriter(fw);
				//Outputs the required data to the output csv
				pw.println(vcc + ',' + new SimpleDateFormat("yyyy/MM/dd").format(date) + ',' + stf.format(timeFormatted) + ',' + vobc + ',' + train + ',' + position + ',' + timeDiff + ',' + direction + ',' + chainage + ',' + station);
				//pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", vcc,date,stf.format(timeFormatted),vobc,train,position,timeDiff,direction,chainage,station);
				pw.close();
			}
		}	
		log.clear();
	}
}
