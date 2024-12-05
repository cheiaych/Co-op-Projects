using System;
using Excel = Microsoft.Office.Interop.Excel;
using System.IO;

//Converts ConfigTestOutput.csv and PortForwardOutput.csv to xlsx then merges them into Output.xlsx
namespace CSVtoXLSX2
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine(args[0] + " " + args[1]);
            if (File.Exists(args[0]) && File.Exists(args[1]))
            {
                //Gets directories to ConfigTestOutput.csv and PortForwardOutput.csv from arguements 
                String ConfigTest = args[0];
                String PortForward = args[1];
                String dir = System.IO.Directory.GetParent(ConfigTest).FullName;

                Console.WriteLine(dir);

                //New Excel Application
                Microsoft.Office.Interop.Excel.Application app = new Excel.Application();
                Microsoft.Office.Interop.Excel.Workbook xlWorkbook;

                Console.WriteLine(ConfigTest + " " + PortForward);

                //Adding ConfigTestOutput.csv and PortForwardOutput.csv to 
                app.Workbooks.Add(ConfigTest);
                app.Workbooks.Add(PortForward);

                //Deletes old xlsx files
                if (File.Exists(dir + "\\ConfigTestOutput.xlsx"))
                {
                    File.Delete(dir + "\\ConfigTestOutput.xlsx");
                }

                if (File.Exists(dir + "\\PortForwardOutput.xlsx"))
                {
                    File.Delete(dir + "\\PortForwardOutput.xlsx");
                }

                if (File.Exists(dir + "\\Output.xlsx"))
                {
                    File.Delete(dir + "\\Output.xlsx");
                }

                //Saves ConfigTestOutput.csv and PortForwardOutput.csv as xlsx files
                for (int i = 1; i <= app.Workbooks.Count; i++)
                {
                    Console.WriteLine(System.IO.Path.GetFileNameWithoutExtension(app.Workbooks[i].Name));
                    app.Workbooks[i].SaveAs(dir + "\\" + System.IO.Path.GetFileNameWithoutExtension(app.Workbooks[i].Name) + ".xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
                }

                if (File.Exists(dir + "\\output.xlsx"))
                {
                    xlWorkbook = app.Workbooks.Open(dir + "\\output.xlsx", Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing);
                }
                else
                {
                    xlWorkbook = app.Workbooks.Add();
                }

                //Adds ConfigTestOutput.xlsx and PortForwardOutput.xlsx to output.xlsx
                app.Workbooks[1].Worksheets[1].Copy(xlWorkbook.Worksheets[1]);
                app.Workbooks[2].Worksheets[1].Copy(xlWorkbook.Worksheets[2]);

                //Sorting the data after row 2 in PortForwardOutput.xlsx by MRU number (first 2 rows are headers)
                Excel.Worksheet sheet = xlWorkbook.Worksheets[2];
                Excel.Range sortRange = sheet.Range["A3", sheet.UsedRange.SpecialCells(Excel.XlCellType.xlCellTypeLastCell)];
                sortRange.Select();
                sheet.Sort.SortFields.Clear();
                sheet.Sort.SortFields.Add(sortRange.Columns["B"], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                var sort = sheet.Sort;
                sort.SetRange(sortRange);
                sort.Header = Excel.XlYesNoGuess.xlYes;
                sort.MatchCase = false;
                sort.Orientation = Microsoft.Office.Interop.Excel.XlSortOrientation.xlSortColumns;
                sort.SortMethod = Microsoft.Office.Interop.Excel.XlSortMethod.xlPinYin;
                sort.Apply();

                //Changing Cell Colours in PortForwardOutput.xlsx
                Console.WriteLine(xlWorkbook.Worksheets[2].UsedRange.Columns.Count);
                for (int j = 3; j < xlWorkbook.Worksheets[2].UsedRange.Columns.Count; j++)
                {
                    Console.WriteLine(j);
                    //Console.WriteLine(xlWorkbook.Worksheets[2].Cells[1, j].Value);
                    if (xlWorkbook.Worksheets[2].Cells[1, j].Value != null)
                    {
                        xlWorkbook.Worksheets[2].Cells[1, j].Interior.Color = Excel.XlRgbColor.rgbLightGreen;
                    }
                }

                int m = 3;
                while (m < xlWorkbook.Worksheets[2].UsedRange.Columns.Count)
                {                   
                    xlWorkbook.Worksheets[2].Cells[2, m].Interior.Color = Excel.XlRgbColor.rgbLightBlue;
                    xlWorkbook.Worksheets[2].Cells[2, m + 1].Interior.Color = Excel.XlRgbColor.rgbLightBlue;
                    m = m + 2;
                    xlWorkbook.Worksheets[2].Cells[2, m].Interior.Color = Excel.XlRgbColor.rgbLightPink;
                    xlWorkbook.Worksheets[2].Cells[2, m + 1].Interior.Color = Excel.XlRgbColor.rgbLightPink;
                    m = m + 2;
                }

                Console.WriteLine(xlWorkbook.Worksheets[2].UsedRange.Rows.Count + " " + xlWorkbook.Worksheets[2].UsedRange.Columns.Count);

                /*Console.WriteLine();
                for (int k = 3; k <= xlWorkbook.Worksheets[2].UsedRange.Rows.Count; k++)
                {
                    for (int l = 3; l <= xlWorkbook.Worksheets[2].UsedRange.Columns.Count; l++)
                    {
                        Console.WriteLine(k + " " + l);
                        if (xlWorkbook.Worksheets[2].Cells[k,l].Value != null)
                        {
                            xlWorkbook.Worksheets[2].Cells[k,l].Interior.Color = Excel.XlRgbColor.rgbRed;
                        }
                    }
                }*/


                //Deletes empty sheets from output.xlsx
                foreach (Excel.Worksheet delSheet in xlWorkbook.Worksheets)
                {
                    if (delSheet.UsedRange.Count < 2)
                    {
                        delSheet.Delete();
                    }
                }

                //Saves output.xlsx
                xlWorkbook.SaveAs(dir + "\\Output.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
                xlWorkbook.Close();
                    
                app.Quit();
                System.Runtime.InteropServices.Marshal.ReleaseComObject(xlWorkbook);
                System.Runtime.InteropServices.Marshal.ReleaseComObject(app);
            }
            else
            {
                Console.WriteLine("Files not found");
            }
        }
    }
}

