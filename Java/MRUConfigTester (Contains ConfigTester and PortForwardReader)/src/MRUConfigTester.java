//MRU Config Tester
//Jordan Hui 01/03/2019
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import org.apache.poi.hssf.usermodel.*;

public class MRUConfigTester {

	//Code for the file chooser window
	public static File fileChooser (String dialog, File dir)
	{
		JFileChooser jfc = new JFileChooser(dir);
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

		//Gets trusted and untrusted directories
		File fcDir = FileSystemView.getFileSystemView().getHomeDirectory();
		File trusted = fileChooser("Choose trusted configuration file directory...", fcDir);
		
		fcDir = new File(trusted.getParent());
		File untrusted = fileChooser("Choose untrusted configuration file directory...", fcDir);
		String dir = trusted.getParent();
		
		//Runs PortForwardReader
		System.out.println("Port Forward Tester:");
		ConfigTester.run(trusted, untrusted);
		
		System.out.println();
		
		//Runs ConfigTester
		System.out.println("Configuration Validator:");
		PortForwardReader.run(trusted);
		
		//Runs CSVtoXLSX
		System.out.println("Creating Excel");
		System.out.println(System.getProperty("user.dir"));
		try 
		{
			Process CSV2Excel = new ProcessBuilder("CSVtoXLSX.exe", dir + "\\ConfigTestOutput.csv", dir + "\\PortForwardOutput.csv").start();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		System.out.println("Done");
	}

}
