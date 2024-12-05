//Jordan Hui
//17/04/2019
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Excel = Microsoft.Office.Interop.Excel;

//Reads data from Edge and Segment text files in gdg folder and places it in a spreadsheet
namespace GuidewayDataReader
{
    class Program
    {
        static void Main(string[] args)
        {
            List<String[]> EdgeData = new List<String[]>();
            List<String[]> SegData = new List<String[]>();
            List<String> SegDataIndex = new List<String>();
            String edgeDataFile, segDataFile, currentLine;
            String[] lineSplit;

            //Gets directory to Edge and Segment data files
            Console.WriteLine("Edge Data directory: ");
            edgeDataFile = Console.ReadLine().Replace("\"", "");
            Console.WriteLine("Segment Data directory: ");
            segDataFile = Console.ReadLine().Replace("\"", "");

            Console.WriteLine("Running...");

            //Reads Edge Data
            StreamReader edgeReader = new StreamReader(edgeDataFile);
            while ((currentLine = edgeReader.ReadLine()) != null)
            {
                //Reads lines that don't start with whitespace and aren't comments
                if (!currentLine[0].Equals("") && !currentLine[0].Equals("#") && Char.IsDigit(currentLine[0]))
                {
                    //Gets Edge ID(id), Track Segment ID(segment), Source Chainage in Feet(source_chainage(ft)), and Source Chainage in Meters(source_chainage(m))
                    lineSplit = currentLine.Split();
                    EdgeData.Add(new string[] { lineSplit[0], lineSplit[1], lineSplit[3], lineSplit[4] });
                }
            }

            //Reads Segment Data
            StreamReader segReader = new StreamReader(segDataFile);
            while ((currentLine = segReader.ReadLine()) != null)
            {
                //Reads lines that don't start with whitespace and aren't comments
                if (!currentLine[0].Equals("") && !currentLine[0].Equals("#") && Char.IsDigit(currentLine[0]))
                {
                    //Gets Track ID(id), Track Segment ID(Mnemonic), and Segment Chainage Direction(Segment_Chainage_Dir)
                    lineSplit = currentLine.Split();
                    SegData.Add(new string[] { lineSplit[0], lineSplit[1], lineSplit[2] });
                    //List to find index of line with segment ID
                    SegDataIndex.Add(lineSplit[1]);
                }
            }

            //Creates new Excel sheet
            Excel.Application xlApp = new Excel.Application();
            xlApp.Visible = false;

            xlApp.Workbooks.Add();
            xlApp.Workbooks[1].Worksheets.Add();

            Excel.Worksheet sheet = xlApp.Workbooks[1].Worksheets[1];

            //Sets headers
            sheet.Cells[1, 1].Value = "Edge ID";
            sheet.Cells[1, 2].Value = "Track Segment";
            sheet.Cells[1, 3].Value = "Source Chainage (ft)";
            sheet.Cells[1, 4].Value = "Source Chainage (m)";
            sheet.Cells[1, 5].Value = "Chainage Direction";

            int ind;
            //Adds data from EdgeData to sheet, then finds the Track Segment ID in SegData to determine the Segment Chainage Direction (INCR or DECR)
            for (int i = 0; i < EdgeData.Count; i++)
            {
                sheet.Cells[i + 2, 1].Value = EdgeData[i][0];
                sheet.Cells[i + 2, 2].Value = EdgeData[i][1];
                sheet.Cells[i + 2, 3].Value = EdgeData[i][2];
                sheet.Cells[i + 2, 4].Value = EdgeData[i][3];

                //Finds index of Edge ID in SegData by using SegDataIndex
                if ((ind = SegDataIndex.IndexOf(EdgeData[i][1])) != -1)
                {
                    sheet.Cells[i + 2, 5].Value = SegData[ind][2];
                }
            }

            Console.WriteLine("Done");

            //Saves sheet in same folder as Edge Data text file and quits
            try
            {
                xlApp.Workbooks[1].SaveAs(System.IO.Directory.GetParent(edgeDataFile.Replace("\"", "")).FullName + "\\GuidewayData.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
            }
            catch
            {

            }

            xlApp.Quit();
        }
    }
}
