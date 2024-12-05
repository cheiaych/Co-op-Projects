//XLSXCreator v1.0
//Jordan Hui 15/02/2019
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Excel = Microsoft.Office.Interop.Excel;

namespace XLSXCreator
{
    class Program
    {
        //Converts csvs to XLSX and saves
        static void ConvertToXLSX (String CSVPath, String savePath, Excel.Application app)
        {
            //Adds Output.csv to Workbooks
            app.Workbooks.Add(CSVPath);
            foreach (Excel.Worksheet ws in app.Workbooks[1].Worksheets)
            {
                //Deletes blank sheets
                if (ws.UsedRange.Count < 2)
                {
                    ws.Delete();
                }
            }
            //Saves Output.csv as Output.xlsx
            app.Workbooks[1].SaveAs(savePath + "\\Output.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
        }

        //Seperates XLSX into GD0 and GD1 worksheets
        static void SplitGD (String XLSXPath, String savePath, Excel.Application app)
        {
            app.Workbooks.Open(XLSXPath);

            //Sets sheet as the Output then sorts by GD0 and GD1
            Excel.Worksheet sheet = app.Workbooks[1].Worksheets[1];
            sheet.UsedRange.Select();
            sheet.Sort.SortFields.Clear();
            sheet.Sort.SortFields.Add(sheet.UsedRange.Columns["H"], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
            var sort = sheet.Sort;
            sort.SetRange(sheet.UsedRange);
            sort.Header = Excel.XlYesNoGuess.xlYes;
            sort.MatchCase = false;
            sort.Orientation = Microsoft.Office.Interop.Excel.XlSortOrientation.xlSortColumns;
            sort.SortMethod = Microsoft.Office.Interop.Excel.XlSortMethod.xlPinYin;
            sort.Apply();

            //Adds new worksheet, changes first worksheet name to GD0 and second worksheet name to GD1
            app.Workbooks[1].Worksheets.Add(After: app.Workbooks[1].Worksheets[1]);
            app.Workbooks[1].Worksheets[1].Name = "GD0";
            app.Workbooks[1].Worksheets[2].Name = "GD1";

            //Copies headers from GD0 to GD1
            Excel.Range headers = app.Workbooks[1].Worksheets[1].Range("A1", "A1").EntireRow;
            headers.Copy(app.Workbooks[1].Worksheets[2].Range("A1", "A1").EntireRow);

            //Finding last filled row of GD0 and GD1
            Excel.Range GD0Last = app.Workbooks[1].Worksheets[1].Cells.SpecialCells(Excel.XlCellType.xlCellTypeLastCell, Type.Missing);
            int GD0LastRow = GD0Last.Row;

            Excel.Range GD1Last = app.Workbooks[1].Worksheets[2].Cells.SpecialCells(Excel.XlCellType.xlCellTypeLastCell, Type.Missing);
            int GD1LastRow = GD1Last.Row + 1;

            //Moves rows containing GD1 to the GD1 worksheet
            Excel.Range cutRow;
            for (int i = 1; i <= GD0LastRow; i++)
            {
                if ((String)app.Workbooks[1].Worksheets[1].Cells(i, 8).Value == "GD1")
                {
                    cutRow = app.Workbooks[1].Worksheets[1].Range("A" + i, "A" + i).EntireRow;
                    cutRow.Cut(app.Workbooks[1].Worksheets[2].Range("A" + GD1LastRow, "A" + GD1LastRow).EntireRow);
                    GD1LastRow++;
                }
            }

            //Saves changes to worksheets
            app.Workbooks[1].SaveAs(savePath + "\\Output.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
        }

        //Generates the Time outs to Station graphs
        static void CreateGraphs(String XLSXPath, String SCPath, String savePath, Excel.Application app)
        {
            Console.WriteLine(SCPath);

            //Start of track chainage
            int minChainage = -475;

            //Opens Output.xlsx as Workbook[1] and _Stations_and_Chainage.xlsx as Workbook[2]
            app.Workbooks.Open(XLSXPath);
            app.Workbooks.Open(SCPath);

            //Copies the data from the GD0 and GD1 sheets in _Station_and_Chainage.xlsx to their respective sheets in Output.xlsx
            Excel.Range GD0Last = app.Workbooks[2].Worksheets[1].Cells.SpecialCells(Excel.XlCellType.xlCellTypeLastCell, Type.Missing);
            Excel.Range GD0Range = app.Workbooks[2].Worksheets[1].Range("A1", GD0Last);
            GD0Range.Copy(app.Workbooks[1].Worksheets[1].Range("L1", "M1"));
            app.Workbooks[1].Worksheets[1].Cells[2, 15] = minChainage;

            Excel.Range GD1Last = app.Workbooks[2].Worksheets[2].Cells.SpecialCells(Excel.XlCellType.xlCellTypeLastCell, Type.Missing);
            Excel.Range GD1Range = app.Workbooks[2].Worksheets[2].Range("A1", GD1Last);
            GD1Range.Copy(app.Workbooks[1].Worksheets[2].Range("L1", "M1"));
            app.Workbooks[1].Worksheets[2].Cells[2, 15] = minChainage;

            //Adding formulas to the GD0 sheet of Output.xlsx
            Excel.Range cellN0, cellO0, cellP0;
            for (int i = 2; i <= GD0Last.Row; i++)
            {
                //Counts number of time-outs that happened between the chainages in columns O and P
                cellN0 = app.Workbooks[1].Worksheets[1].Range["N" + i];
                //Original formula: cellN0.Formula = "=COUNTIFS(I:I,\" < \"&P2,I:I,\" > \"&O2)";
                cellN0.Formula = "=COUNTIFS(I:I,\"<\"&P" + i + ",I:I,\">\"&O" + i + ")";

                //Calculates the chainages for the points between each station in order to find which station each time-out happened closest to
                cellO0 = app.Workbooks[1].Worksheets[1].Range["O" + (i + 1)];
                cellP0 = app.Workbooks[1].Worksheets[1].Range["P" + i];
                if (app.Workbooks[1].Worksheets[1].Cells(i + 1, 13).Value2 != null)
                {
                    //Original formula: cellO0.Formula = "=P2";
                    cellO0.Formula = "=P" + i;

                    //Original formula: cellP0.Formula = "=M2+((M3-M2)/2)";
                    cellP0.Formula = "=M" + i + "+((M" + (i + 1) + "-M" + i + ")/2)";
                }
                else
                {
                    //Adds 500m to the chainage of the last station
                    //Original formula: cellP0.Formula = "=M28+500";
                    cellP0.Formula = "=M" + i + "+500";
                }
            }

            //Adding formulas to the GD1 sheet of Output.xlsx
            Excel.Range cellN1, cellO1, cellP1;
            for (int i = 2; i <= GD1Last.Row; i++)
            {
                //Counts number of time-outs that happened between the chainages in columns O and P
                cellN1 = app.Workbooks[1].Worksheets[2].Range["N" + i];
                //cellN1.Formula = "=COUNTIFS(I:I,\" < \"&P2,I:I,\" > \"&O2)";
                cellN1.Formula = "=COUNTIFS(I:I,\"<\"&P" + i + ",I:I,\">\"&O" + i + ")";

                //Calculates the chainages for the points between each station in order to find which station each time-out happened closest to
                cellO1 = app.Workbooks[1].Worksheets[2].Range["O" + (i + 1)];
                cellP1 = app.Workbooks[1].Worksheets[2].Range["P" + i];
                if (app.Workbooks[1].Worksheets[2].Cells(i + 1, 13).Value2 != null)
                {
                    //Original formula: cellO1.Formula = "=P2";
                    cellO1.Formula = "=P" + i;

                    //Original formula: cellP1.Formula = "=M2+((M3-M2)/2)";
                    cellP1.Formula = "=M" + i + "+((M" + (i + 1) + "-M" + i + ")/2)";
                }
                else
                {
                    //Adds 500m to the chainage of the last station
                    //Original formula: cellP1.Formula = "=M28+500";
                    cellP1.Formula = "=M" + i + "+500";
                }
            }

            //GD0 Chart
            //Creates GD0 chart in Output.xlsx
            Excel.ChartObjects GD0Charts = (Excel.ChartObjects)app.Workbooks[1].Worksheets[1].ChartObjects();
            Excel.ChartObject GD0ChartObj = (Excel.ChartObject)GD0Charts.Add(825, 10, 500, 500);
            Excel.Chart GD0Chart = GD0ChartObj.Chart;

            //Sets column N as the data for the chart
            Excel.Range GD0DataRange = ((Excel.Worksheet)app.Workbooks[1].Worksheets[1]).get_Range("N2", "N" + GD0Last.Row);
            GD0Chart.SetSourceData(GD0DataRange);

            //Sets column M as the Data for the X-axis in the chart
            Excel.Range GD0AxisData = ((Excel.Worksheet)app.Workbooks[1].Worksheets[1]).get_Range("L2", "L" + GD0Last.Row);
            Excel.Axis GD0xAxis = (Excel.Axis)GD0Chart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
            GD0xAxis.CategoryNames = GD0AxisData;

            //Adds title and removes legend from chart
            GD0Chart.HasTitle = true;
            GD0Chart.ChartTitle.Text = "Time-Outs to Closest Station";
            GD0Chart.HasLegend = false;
            GD0Chart.ChartType = Excel.XlChartType.xlColumnClustered;

            //GD1 Chart
            //Creates GD1 chart in Output.xlsx
            Excel.ChartObjects GD1Charts = (Excel.ChartObjects)app.Workbooks[1].Worksheets[2].ChartObjects();
            Excel.ChartObject GD1ChartObj = (Excel.ChartObject)GD1Charts.Add(825, 10, 500, 500);
            Excel.Chart GD1Chart = GD1ChartObj.Chart;

            //Sets column N as the data for the chart
            Excel.Range GD1DataRange = ((Excel.Worksheet)app.Workbooks[1].Worksheets[2]).get_Range("N2", "N" + GD1Last.Row);
            GD1Chart.SetSourceData(GD1DataRange);

            //Sets column M as the Data for the X-axis in the chart
            Excel.Range GD1AxisData = ((Excel.Worksheet)app.Workbooks[1].Worksheets[2]).get_Range("L2", "L" + GD1Last.Row);
            Excel.Axis GD1xAxis = (Excel.Axis)GD1Chart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
            GD1xAxis.CategoryNames = GD1AxisData;

            //Adds title and removes legend from chart
            GD1Chart.HasTitle = true;
            GD1Chart.ChartTitle.Text = "Time-Outs to Closest Station";
            GD1Chart.HasLegend = false;
            GD1Chart.ChartType = Excel.XlChartType.xlColumnClustered;

            //Saves Workbook[1] changes to Output.xlsx
            app.Workbooks[1].SaveAs(savePath + "\\Output.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
        }

        static void Main(string[] args)
        {
            //Argument is filepath to Output.csv
            //Begins new excel application
            Microsoft.Office.Interop.Excel.Application app = new Excel.Application();
            app.DisplayAlerts = false;
            String CSVPath = args[0];
            String parentPath = System.IO.Directory.GetParent(CSVPath).FullName;
            String XLSXPath = parentPath + "\\Output.xlsx";
            //Uses first _Stations_and_Chainage.xlsx file in same folder as application
            String StationsChainagePath = Directory.GetFiles(parentPath, "*_Stations_and_Chainage.xlsx")[0];

            //Catches errors to ensure app is closed if errors occur 
            try {
                ConvertToXLSX(CSVPath, parentPath, app);
                SplitGD(XLSXPath, parentPath, app);
                CreateGraphs(XLSXPath, StationsChainagePath, parentPath, app);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }

            app.Quit();
        }
    }
}
