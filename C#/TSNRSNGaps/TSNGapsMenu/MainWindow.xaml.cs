//Jordan Hui
//17/04/2019
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using Excel = Microsoft.Office.Interop.Excel;
using System.Threading;
using System.Collections;

namespace TSNGapsMenu
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
        }

        //Merges DR files into Merged PCAP
        private void Merge_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                string arguments = "", drdir;
                Environment.CurrentDirectory = DRDir.Text.Replace("\"", "");
                if (DRDir.Text != "")
                {
                    drdir = DRDir.Text;
                    Task.Run(() =>
                    {
                        //Disables buttons and sets Progress Bar mode
                        this.Dispatcher.Invoke(() =>
                        {
                            Merge.IsEnabled = false;
                            Generate.IsEnabled = false;
                            Run.IsEnabled = false;

                            ProgBar.IsIndeterminate = true;
                        });

                        //Passes arguements to mergecap and runs it
                        Thread MergePCAPs = new Thread(delegate ()
                        {
                            String err = "";
                            ProcessStartInfo process = new ProcessStartInfo(@"C:\Program Files\Wireshark\mergecap.exe");
                            process.WindowStyle = System.Diagnostics.ProcessWindowStyle.Hidden;
                            arguments = "\"" + drdir + "\\*.*\"  -w " + "\"" + drdir + "\\merged.pcap\"";
                            process.Arguments = arguments;
                            //Gets errors from mergecap
                            process.UseShellExecute = false;
                            process.RedirectStandardError = true;
                            Process processMerge = Process.Start(process);
                            processMerge.StartInfo.RedirectStandardError = true;
                            err = processMerge.StandardError.ReadToEnd();
                            processMerge.WaitForExit();
                            Console.WriteLine("Finished Merging");

                            //Outputs errors from mergecap
                            if (!err.Equals(""))
                            {
                                MessageBox.Show(err);
                            }
                        });

                        //Waits for mergecap to finish
                        MergePCAPs.Start();
                        MergePCAPs.Join();

                        //Enables buttons and changes progress bar mode
                        this.Dispatcher.Invoke(() =>
                        {
                            Merge.IsEnabled = true;
                            Generate.IsEnabled = true;
                            Run.IsEnabled = true;

                            ProgBar.IsIndeterminate = false;
                        });
                    });
                }
            }
            catch (Exception mcerr)
            {
                MessageBox.Show(mcerr.ToString());

                //Reneables buttons
                this.Dispatcher.Invoke(() =>
                {
                    Merge.IsEnabled = true;
                    Generate.IsEnabled = true;
                    Run.IsEnabled = true;

                    ProgBar.IsIndeterminate = false;
                });
            }
        }

        //Creates CSV from merged PCAP using arguements with tshark
        private void CSVClick(object sender, RoutedEventArgs e)
        {
            //Only runs if nessecary fields are filled
            if (!rsn.Text.Equals("") && !tsn.Text.Equals("") && !rx_id.Text.Equals("") && !tx_id.Text.Equals("") && !time_field.Text.Equals("") && !protocol.Text.Equals("") && !dissector_dir.Text.Equals("") && !pcap_dir.Text.Equals(""))
            {
                //Adds arguements and directories
                string args = "";
                string rsn_field = rsn.Text;
                string tsn_field = tsn.Text;
                string rec_id = rx_id.Text;
                string trans_id = tx_id.Text;
                string time = time_field.Text;
                string frontseg = protocol.Text + "." + front_seg.Text;
                string frontoff = protocol.Text + "." + front_off.Text;

                string srcip = ip_src.Text;
                string dstip = ip_dst.Text;
                string pcapdir = pcap_dir.Text.Replace("\"", "");
                string proto = protocol.Text;

                //Checks if packets in protocol are from VOBC
                Boolean isVobc2 = false;
                Console.WriteLine(protocol.Text.Substring(0, 4));
                if (protocol.Text.Substring(0, 4).Equals("vobc"))
                {
                    isVobc2 = true;
                }

                try
                {
                    //Process for tshark
                    ProcessStartInfo process = new ProcessStartInfo(dissector_dir.Text.Replace("\"", "") + "\\tshark.bat");

                    if (!dissector_dir.Text.Equals(""))
                    {
                        Environment.CurrentDirectory = dissector_dir.Text.Replace("\"", "");
                        Task.Run(() =>
                        {
                            //Disables buttons to prevent multiple instances being run at once
                            this.Dispatcher.Invoke(() =>
                            {
                                Merge.IsEnabled = false;
                                Generate.IsEnabled = false;
                                Run.IsEnabled = false;

                                ProgBar.IsIndeterminate = true;
                            });

                            //Thread for tshark.bat
                            Thread CreateCSV = new Thread(delegate ()
                            {
                                string processIP = "", err = "";

                                trans_id = " -e " + trans_id;
                                rec_id = " -e " + rec_id;
                                tsn_field = " -e " + tsn_field;
                                rsn_field = " -e " + rsn_field;
                                time = " -e " + time;
                                frontseg = " -e " + frontseg;
                                frontoff = " -e " + frontoff;

                                string total_fields = trans_id + rec_id + tsn_field + rsn_field + time;
                                if (isVobc2)
                                {
                                    total_fields = total_fields + frontseg + frontoff;
                                }

                                if (srcip != "")
                                {
                                    processIP = " -Y ip.src==" + srcip;
                                }
                                else if (dstip != "")
                                {
                                    processIP = " -Y ip.dst==" + dstip;
                                }

                                //Creates string of arguements then passes them to tshark.bat
                                args = "-r " + "\"" + pcapdir + "\"" + " -T fields -e frame.number -e frame.time -e frame.len -e ip.src -e ip.dst " + total_fields + " -E header=y -E separator=, -E aggregator=: -E quote=d -E occurrence=f -2 -R " + proto + processIP + " > Output.csv";
                                process.Arguments = args;
                                //Gets errors from tshark
                                process.UseShellExecute = false;
                                process.RedirectStandardError = true;
                                Process processCSV = Process.Start(process);
                                err = processCSV.StandardError.ReadToEnd();
                                processCSV.WaitForExit();

                                //Outputs tshark errors excluding "The NPF driver isn't running" errors
                                if (!err.Equals(""))
                                {
                                    if (err.Contains("listing interfaces."))
                                    {
                                        if (err.IndexOf("tshark") >= 0)
                                        {
                                            err = err.Substring(err.IndexOf("tshark"));
                                        }
                                        else
                                        {
                                            err = "";
                                        }
                                    }
                                    if (!err.Equals(""))
                                    {
                                        MessageBox.Show(err);
                                    }
                                }
                            });

                            //Waits for tshark to finish running
                            CreateCSV.Start();
                            CreateCSV.Join();

                            //Reneables buttons
                            this.Dispatcher.Invoke(() =>
                            {
                                Merge.IsEnabled = true;
                                Generate.IsEnabled = true;
                                Run.IsEnabled = true;

                                ProgBar.IsIndeterminate = false;
                            });
                        });
                    }
                }
                catch (Exception CSVerr)
                {
                    //Shows error
                    MessageBox.Show(CSVerr.ToString());

                    //Reneables buttons
                    this.Dispatcher.Invoke(() =>
                    {
                        Merge.IsEnabled = true;
                        Generate.IsEnabled = true;
                        Run.IsEnabled = true;

                        ProgBar.IsIndeterminate = false;
                    });
                }
            }
        }

        //Enables or disables ChainageDataDir textbox depending on whether or not Graph By Chainage is selected
        public Boolean UseChainage = false;
        private void GraphBy(object sender, EventArgs e)
        {
            if (GraphByTime.IsChecked == true)
            {
                UseChainage = false;
                ChainageDataDir.IsEnabled = false;
            }
            else if (GraphByChainage.IsChecked == true)
            {
                UseChainage = true;
                ChainageDataDir.IsEnabled = true;
            }
        }

        //Checks if user selected Graph by time or Graph by chainage
        private void TSNGaps_Click(object sender, RoutedEventArgs e)
        {
            if (UseChainage == false)
            {
                TSNNoChainage();
            }
            else
            {
                TSNWithChainage();
            }
        }

        //Calculates packet loss based on gaps in TSN and RSN numbers and graphs them based on Header Time
        private void TSNNoChainage()
        {
            //Only runs if directory to CSV file is given
            if (CSVDir.Text != "")
            {
                //Diables buttons
                Merge.IsEnabled = false;
                Generate.IsEnabled = false;
                Run.IsEnabled = false;

                string filepath = CSVDir.Text;

                //Creates new excel application
                Excel.Application xlApp = new Excel.Application();
                xlApp.Visible = false;
                try
                {
                    Excel.Workbook book = xlApp.Workbooks.Open(filepath.Replace("\"", ""));
                    Excel.Worksheet sheet = book.Worksheets[1];

                    Excel.Range fullRange = sheet.get_Range("A1", "L" + sheet.UsedRange.Rows.Count);
                    Excel.Range noHeader = sheet.get_Range("A2", "L" + sheet.UsedRange.Rows.Count);
                    Excel.Range visibleRange;

                    //Sorts based on Source IP, Destination IP, Transmitter ID, Reciever ID, then Time to prevent gaps in row numbers while filtering
                    sheet.Sort.SortFields.Clear();
                    sheet.Sort.SortFields.Add(sheet.Columns[4], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[5], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[6], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[7], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);

                    sheet.Sort.SortFields.Add(sheet.Columns[2], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);

                    var sort = sheet.Sort;
                    sort.SetRange(sheet.UsedRange);
                    sort.Header = Excel.XlYesNoGuess.xlYes;
                    sort.MatchCase = false;
                    sort.Orientation = Microsoft.Office.Interop.Excel.XlSortOrientation.xlSortColumns;
                    sort.SortMethod = Microsoft.Office.Interop.Excel.XlSortMethod.xlPinYin;
                    sort.Apply();

                    double sheetLength = noHeader.Rows.Count;
                    double rowsProc = 0;
                    ProgBar.Value = 0;

                    //Filters for each Source IP, Destination IP, Transmitter ID, and Reciever ID combination
                    Task.Run(() =>
                    {
                        visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);
                        //IP Source Filter (col D)
                        System.Array ipSrc;
                        
                        //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                        if (visibleRange.Columns[4].Value is Array)
                        {
                            ipSrc = visibleRange.Columns[4].Value;
                        }
                        else
                        {
                            ipSrc = new String[] { visibleRange.Columns[4].Value.ToString() };
                        }
                        //Converts all values to String, then finds all distinct values
                        String[] string_ipSrc = ipSrc.OfType<object>().Select(o => o.ToString()).ToArray();
                        String[] distinct_ipSrc = string_ipSrc.Distinct().ToArray();

                        //Filters for each distinct Source IP
                        foreach (string ip_src in distinct_ipSrc)
                        {
                            //Resets filters, then refilters by Source IP
                            sheet.AutoFilterMode = false;

                            fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);
                            visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                            //IP Destination Filter (col E)
                            System.Array ipDst;
                            //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                            if (visibleRange.Columns[5].Value is Array)
                            {
                                ipDst = visibleRange.Columns[5].Value;
                            }
                            else
                            {
                                ipDst = new String[] { visibleRange.Columns[5].Value.ToString() };
                            }
                            //Converts all values to String, then finds all distinct values
                            String[] string_ipDst = ipDst.OfType<object>().Select(o => o.ToString()).ToArray();
                            String[] distinct_ipDst = string_ipDst.Distinct().ToArray();

                            //Filters for each distinct Destination IP under each Source IP
                            foreach (string ip_dst in distinct_ipDst)
                            {
                                //Resets filters, then filters by Source IP and Destination IP
                                sheet.AutoFilterMode = false;
                                fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);

                                fullRange.AutoFilter(5, ip_dst, Excel.XlAutoFilterOperator.xlFilterValues);
                                visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                //Transmitter ID Filter (col F)
                                System.Array trnsmtrID;
                                //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                                if (visibleRange.Columns[6].Value is Array)
                                {
                                    trnsmtrID = visibleRange.Columns[6].Value;
                                }
                                else
                                {
                                    trnsmtrID = new String[] { visibleRange.Columns[6].Value.ToString() };
                                }
                                //Converts all values to String, then finds all distinct values
                                String[] string_TransmitterID = trnsmtrID.OfType<object>().Select(o => o.ToString()).ToArray();
                                String[] distinct_TransmitterID = string_TransmitterID.Distinct().ToArray();

                                //Filters for each distinct transmitter ID under each Destination IP under each Source IP
                                foreach (string trnsmtr_id in distinct_TransmitterID)
                                {
                                    //Resets filters, then refilters by Source IP, Destination IP, and Transmitter ID
                                    sheet.AutoFilterMode = false;
                                    fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);
                                    fullRange.AutoFilter(5, ip_dst, Excel.XlAutoFilterOperator.xlFilterValues);

                                    fullRange.AutoFilter(6, trnsmtr_id, Excel.XlAutoFilterOperator.xlFilterValues);
                                    visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                    //Reciever ID Filter (col G)
                                    System.Array rcvrID;
                                    //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                                    if (visibleRange.Columns[7].Value is Array)
                                    {
                                        rcvrID = visibleRange.Columns[7].Value;
                                    }
                                    else
                                    {
                                        rcvrID = new String[] { visibleRange.Columns[7].Value.ToString() };
                                    }
                                    //Converts all values to String, then finds all distinct values
                                    String[] string_RecieverID = rcvrID.OfType<object>().Select(o => o.ToString()).ToArray();
                                    String[] distinct_RecieverID = string_RecieverID.Distinct().ToArray();

                                    //Filters for each distinct reciever ID under each transmitter ID under each Destination IP under each Source IP
                                    foreach (string rcvr_id in distinct_RecieverID)
                                    {
                                        Console.WriteLine(ip_src + "|" + ip_dst + "|" + trnsmtr_id + "|" + rcvr_id);

                                        fullRange.AutoFilter(7, rcvr_id, Excel.XlAutoFilterOperator.xlFilterValues);
                                        visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                        //Number of rows is equal to number of cells divided by number of columns
                                        Console.WriteLine(visibleRange.Count / 12);

                                        rowsProc += (visibleRange.Count / 12);

                                        //Only runs if there is visible data after the filters
                                        if (visibleRange.Count / 12 > 2)
                                        {
                                            visibleRange = sheet.UsedRange.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                            visibleRange.Cells[1, 14].Value = "TSN Packets Lost";
                                            visibleRange.Cells[1, 15].Value = "RSN Packets Lost";

                                            double tgap, rgap;
                                            long prevRow = 1, rangeInd = 1, frInd = 0;
                                            int[] firstRows = new int[10];

                                            int[] tsngaps = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
                                            int[] rsngaps = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };

                                            foreach (Excel.Range r in visibleRange.Rows)
                                            {
                                                Console.WriteLine(r.Row);
                                                if (rangeInd == 2)
                                                {
                                                    r.Cells[1, 14].Value = 0;
                                                    r.Cells[1, 15].Value = 0;
                                                }
                                                if (rangeInd > 2)
                                                {
                                                    //Calculates gap in TSN values. A difference of over 1 means a packet was lost. A cell is left blank if the difference is 0 or less, meaning there was either an error or the values reset to 1
                                                    if (r.Cells[1, 8].Value > visibleRange.Cells[prevRow, 8].Value)
                                                    {
                                                        tgap = (r.Cells[1, 8].Value - visibleRange.Cells[prevRow, 8].Value) - 1;
                                                        r.Cells[1, 14].Value = tgap;

                                                        //Counts occourences of losses of 1, 2, 3, 4, 5, 6, 7, and 8+ packets
                                                        switch (tgap)
                                                        {
                                                            case var expression when tgap == 0:
                                                                break;
                                                            case var expression when tgap == 1:
                                                                tsngaps[0]++;
                                                                break;
                                                            case var expression when tgap == 2:
                                                                tsngaps[1]++;
                                                                break;
                                                            case var expression when tgap == 3:
                                                                tsngaps[2]++;
                                                                break;
                                                            case var expression when tgap == 4:
                                                                tsngaps[3]++;
                                                                break;
                                                            case var expression when tgap == 5:
                                                                tsngaps[4]++;
                                                                break;
                                                            case var expression when tgap == 6:
                                                                tsngaps[5]++;
                                                                break;
                                                            case var expression when tgap == 7:
                                                                tsngaps[6]++;
                                                                break;
                                                            case var expression when tgap >= 8:
                                                                tsngaps[7]++;
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }

                                                    //Calculates gap in RSN values. A difference of over 1 means a packet was lost. A cell is left blank if the difference is 0 or less, meaning there was either an error or the values reset to 1
                                                    if (r.Cells[1, 9].Value > visibleRange.Cells[prevRow, 9].Value)
                                                    {
                                                        rgap = (r.Cells[1, 9].Value - visibleRange.Cells[prevRow, 9].Value) - 1;
                                                        r.Cells[1, 15].Value = rgap;

                                                        //Counts occourences of losses of 1, 2, 3, 4, 5, 6, 7, and 8+ packet
                                                        switch (rgap)
                                                        {
                                                            case var expression when rgap == 0:
                                                                break;
                                                            case var expression when rgap == 1:
                                                                rsngaps[0]++;
                                                                break;
                                                            case var expression when rgap == 2:
                                                                rsngaps[1]++;
                                                                break;
                                                            case var expression when rgap == 3:
                                                                rsngaps[2]++;
                                                                break;
                                                            case var expression when rgap == 4:
                                                                rsngaps[3]++;
                                                                break;
                                                            case var expression when rgap == 5:
                                                                rsngaps[4]++;
                                                                break;
                                                            case var expression when rgap == 6:
                                                                rsngaps[5]++;
                                                                break;
                                                            case var expression when rgap == 7:
                                                                rsngaps[6]++;
                                                                break;
                                                            case var expression when rgap >= 8:
                                                                rsngaps[7]++;
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }
                                                }

                                                //If there are over 10 rows of data, take find the indexes of the first 10 visible rows
                                                if (rangeInd < 11)
                                                {
                                                    firstRows[frInd] = r.Row;
                                                    frInd++;
                                                }

                                                prevRow = r.Row;
                                                rangeInd++;
                                            }

                                            //Prints TSN and RSN Gap summary if there are more than 10 rows of data. If there are less then 10 rows, there is not enough room to print the summary
                                            if (!firstRows.Contains(0))
                                            {
                                                visibleRange.Cells[firstRows[0], 17].Value = "TSN Packets Lost";
                                                visibleRange.Cells[firstRows[1], 17].Value = "1 Packet";
                                                visibleRange.Cells[firstRows[2], 17].Value = "2 Packets";
                                                visibleRange.Cells[firstRows[3], 17].Value = "3 Packets";
                                                visibleRange.Cells[firstRows[4], 17].Value = "4 Packets";
                                                visibleRange.Cells[firstRows[5], 17].Value = "5 Packets";
                                                visibleRange.Cells[firstRows[6], 17].Value = "6 Packets";
                                                visibleRange.Cells[firstRows[7], 17].Value = "7 Packets";
                                                visibleRange.Cells[firstRows[8], 17].Value = ">=8 Packets";

                                                visibleRange.Cells[firstRows[0], 18].Value = "# of Occurrences";
                                                visibleRange.Cells[firstRows[1], 18].Value = tsngaps[0];
                                                visibleRange.Cells[firstRows[2], 18].Value = tsngaps[1];
                                                visibleRange.Cells[firstRows[3], 18].Value = tsngaps[2];
                                                visibleRange.Cells[firstRows[4], 18].Value = tsngaps[3];
                                                visibleRange.Cells[firstRows[5], 18].Value = tsngaps[4];
                                                visibleRange.Cells[firstRows[6], 18].Value = tsngaps[5];
                                                visibleRange.Cells[firstRows[7], 18].Value = tsngaps[6];
                                                visibleRange.Cells[firstRows[8], 18].Value = tsngaps[7];

                                                visibleRange.Cells[firstRows[0], 20].Value = "RSN Packets Lost";
                                                visibleRange.Cells[firstRows[1], 20].Value = "1 Packet";
                                                visibleRange.Cells[firstRows[2], 20].Value = "2 Packets";
                                                visibleRange.Cells[firstRows[3], 20].Value = "3 Packets";
                                                visibleRange.Cells[firstRows[4], 20].Value = "4 Packets";
                                                visibleRange.Cells[firstRows[5], 20].Value = "5 Packets";
                                                visibleRange.Cells[firstRows[6], 20].Value = "6 Packets";
                                                visibleRange.Cells[firstRows[7], 20].Value = "7 Packets";
                                                visibleRange.Cells[firstRows[8], 20].Value = ">=8 Packets";

                                                visibleRange.Cells[firstRows[0], 21].Value = "# of Occurrences";
                                                visibleRange.Cells[firstRows[1], 21].Value = tsngaps[0];
                                                visibleRange.Cells[firstRows[2], 21].Value = tsngaps[1];
                                                visibleRange.Cells[firstRows[3], 21].Value = tsngaps[2];
                                                visibleRange.Cells[firstRows[4], 21].Value = tsngaps[3];
                                                visibleRange.Cells[firstRows[5], 21].Value = tsngaps[4];
                                                visibleRange.Cells[firstRows[6], 21].Value = tsngaps[5];
                                                visibleRange.Cells[firstRows[7], 21].Value = tsngaps[6];
                                                visibleRange.Cells[firstRows[8], 21].Value = tsngaps[7];
                                            }

                                            sheet.AutoFilter.ShowAllData();
                                            visibleRange = null;

                                            //Increments progress bar value by number of rows proccessed divided by total number of rows in CSV
                                            this.Dispatcher.Invoke(() =>
                                            {
                                                ProgBar.Value = (rowsProc / sheetLength) * 100;
                                            });
                                        }
                                    }
                                }
                            }
                        }

                        //Creates Packet Loss to Time Graph
                        Excel.ChartObjects Charts = (Excel.ChartObjects)sheet.ChartObjects();
                        Excel.ChartObject TChartObj = (Excel.ChartObject)Charts.Add(1100, 10, 1500, 250);
                        Excel.Chart TPLChart = TChartObj.Chart;

                        //Sets column M (Packets Lost) as the data for the chart
                        Excel.Range TDataRange = sheet.get_Range("N2", "N" + fullRange.Rows.Count);
                        TPLChart.SetSourceData(TDataRange);

                        //Sets column J (Thales Time Header) as the Data for the X-axis in the chart
                        Excel.Range TAxisData = sheet.get_Range("J2", "J" + fullRange.Rows.Count);
                        Excel.Axis TxAxis = (Excel.Axis)TPLChart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
                        TxAxis.CategoryNames = TAxisData;

                        TPLChart.Parent.Placement = Excel.XlPlacement.xlFreeFloating;

                        //Changes Y-axis units
                        TPLChart.Axes(Excel.XlAxisType.xlValue).MajorUnit = 5;

                        //Removes legend from chart
                        TPLChart.HasLegend = false;
                        TPLChart.ChartType = Excel.XlChartType.xlColumnClustered;

                        //Adds title
                        TPLChart.HasTitle = true;
                        TPLChart.ChartTitle.Text = "TSN Packet Loss";


                        //Creates Packet Loss to Time Graph
                        Excel.ChartObject RChartObj = (Excel.ChartObject)Charts.Add(2610, 10, 1500, 250);
                        Excel.Chart RPLChart = RChartObj.Chart;

                        //Sets column M (Packets Lost) as the data for the chart
                        Excel.Range RDataRange = sheet.get_Range("O2", "O" + fullRange.Rows.Count);
                        RPLChart.SetSourceData(RDataRange);

                        //Sets column J (Thales Time Header) as the Data for the X-axis in the chart
                        Excel.Range RAxisData = sheet.get_Range("J2", "J" + fullRange.Rows.Count);
                        Excel.Axis RxAxis = (Excel.Axis)RPLChart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
                        RxAxis.CategoryNames = RAxisData;

                        //Prevents chart from moving while filtering
                        RPLChart.Parent.Placement = Excel.XlPlacement.xlFreeFloating;

                        //Changes Y-axis units
                        RPLChart.Axes(Excel.XlAxisType.xlValue).MajorUnit = 5;

                        //Removes legend from chart
                        RPLChart.HasLegend = false;
                        RPLChart.ChartType = Excel.XlChartType.xlColumnClustered;

                        //Adds title
                        RPLChart.HasTitle = true;
                        RPLChart.ChartTitle.Text = "RSN Packet Loss";

                        //Catches Error due to Save not being selected
                        String savepath = System.IO.Directory.GetParent(filepath.Replace("\"", "")).FullName;
                        try
                        {
                            book.SaveAs(savepath + "\\TSNRSNOutput.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
                        }
                        catch
                        {

                        }
                        
                        //Closes excel application
                        xlApp.Quit();

                        //Enables buttons and resets progress bar
                        this.Dispatcher.Invoke(() =>
                        {
                            Merge.IsEnabled = true;
                            Generate.IsEnabled = true;
                            Run.IsEnabled = true;

                            ProgBar.Value = 0;
                        });
                    });
                }
                //Enables buttons, resets progress bar, and shows error
                catch (Exception TSNTimeErr)
                {
                    Merge.IsEnabled = true;
                    Generate.IsEnabled = true;
                    Run.IsEnabled = true;

                    ProgBar.Value = 0;

                    xlApp.Quit();
                    MessageBox.Show(TSNTimeErr.ToString());
                }
            }
        }

        //Calcuates chainage using the Guideway Chainage Data file
        private void FindChainage (Excel.Application xlApp)
        {
            //Opens Guideway Chainage Data file and adds the data to a list of arrays
            Excel.Worksheet ChainageData = xlApp.Workbooks[2].Worksheets[1];

            List<String[]> ChainageList = new List<String[]>();
            List<String> ChainageListIndex = new List<String>(); 

            for (int i = 2; i < ChainageData.UsedRange.Rows.Count; i++)
            {
                ChainageList.Add(new String[] { ChainageData.Cells[i, 1].Value.ToString(), ChainageData.Cells[i, 2].Value.ToString(), ChainageData.Cells[i, 3].Value.ToString(), ChainageData.Cells[i, 4].Value.ToString(), ChainageData.Cells[i, 5].Value.ToString() });
                ChainageListIndex.Add(ChainageData.Cells[i, 1].Value.ToString());
            }

            Excel.Worksheet packetData = xlApp.Workbooks[1].Worksheets[1];
            packetData.Cells[1, 13].Value = "Chainage (Meters)";

            int ind, totalRows = packetData.UsedRange.Rows.Count;
            Double chainage;

            //Finds the chainage of the beginning of Train_Front_Segment(Track Segment ID) and adds or subtracts Train_Front_Offset(cm away from beginning of segment) based on the Guideway Chainage Data
            for (int j = 2; j <= totalRows; j++)
            {
                //Console.WriteLine("FindChainage: " + j);
                if ((ind = ChainageListIndex.IndexOf(packetData.Cells[j, 11].Value.ToString())) != -1)
                {
                    Console.WriteLine(Convert.ToDouble(packetData.Cells[j, 12].Value.ToString()) + " " +  Convert.ToDouble(ChainageList[ind][3].ToString()));
                    if (ChainageList[ind][4].ToString().Equals("INCR"))
                    {
                        chainage = Convert.ToDouble(ChainageList[ind][3].ToString()) + (Convert.ToDouble(packetData.Cells[j, 12].Value.ToString()) / 100);
                        packetData.Cells[j, 13].Value = chainage;
                    }
                    else if (ChainageList[ind][4].ToString().Equals("DECR"))
                    {
                        chainage = Convert.ToDouble(ChainageList[ind][3].ToString()) - (Convert.ToDouble(packetData.Cells[j, 12].Value.ToString()) / 100);
                        packetData.Cells[j, 13].Value = chainage;
                    }
                }
            }
        }

        private void TSNWithChainage()
        {
            if (CSVDir.Text != "" && ChainageDataDir.Text != "")
            {
                //Disables buttons
                Merge.IsEnabled = false;
                Generate.IsEnabled = false;
                Run.IsEnabled = false;

                string filepath = CSVDir.Text;

                Excel.Application xlApp = new Excel.Application();
                xlApp.Visible = false;
                try
                {
                    Excel.Workbook book = xlApp.Workbooks.Open(filepath.Replace("\"", ""));
                    Excel.Worksheet sheet = book.Worksheets[1];

                    //Only packets from VOBC have train chainage data
                    if (sheet.Cells[1, 11].Value == null || sheet.Cells[1, 12].Value == null) 
                    {
                        throw new Exception("Front offset and front segment not found. Chainage can only be found for packets from VOBC.");
                    }

                    Excel.Range fullRange = sheet.get_Range("A1", "L" + sheet.UsedRange.Rows.Count);
                    Excel.Range noHeader = sheet.get_Range("A2", "L" + sheet.UsedRange.Rows.Count);
                    Excel.Range visibleRange;

                    //Sorts based on Source IP, Destination IP, Transmitter ID, Reciever ID, then Time to prevent gaps in row numbers while filtering
                    sheet.Sort.SortFields.Clear();
                    sheet.Sort.SortFields.Add(sheet.Columns[4], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[5], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[6], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);
                    sheet.Sort.SortFields.Add(sheet.Columns[7], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);

                    sheet.Sort.SortFields.Add(sheet.Columns[2], Excel.XlSortOn.xlSortOnValues, Excel.XlSortOrder.xlAscending, System.Type.Missing, Excel.XlSortDataOption.xlSortNormal);

                    var sort = sheet.Sort;
                    sort.SetRange(sheet.UsedRange);
                    sort.Header = Excel.XlYesNoGuess.xlYes;
                    sort.MatchCase = false;
                    sort.Orientation = Microsoft.Office.Interop.Excel.XlSortOrientation.xlSortColumns;
                    sort.SortMethod = Microsoft.Office.Interop.Excel.XlSortMethod.xlPinYin;
                    sort.Apply();

                    double sheetLength = noHeader.Rows.Count;
                    double rowsProc = 0;
                    ProgBar.Value = 0;

                    Excel.Workbook ChainageDataXLSX = xlApp.Workbooks.Open(ChainageDataDir.Text.Replace("\"", ""));

                    var findChainageTask = Task.Run(() => { FindChainage(xlApp); }); 

                    //Filters for each Source IP, Destination IP, Transmitter ID, and Reciever ID combination
                    Task.Run(() =>
                    {
                        visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);
                        //IP Source Filter (col D)
                        System.Array ipSrc;
                        
                        //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                        if (visibleRange.Columns[4].Value is Array)
                        {
                            ipSrc = visibleRange.Columns[4].Value;
                        }
                        else
                        {
                            ipSrc = new String[] { visibleRange.Columns[4].Value.ToString() };
                        }
                        //Converts all values to String, then finds all distinct values
                        String[] string_ipSrc = ipSrc.OfType<object>().Select(o => o.ToString()).ToArray();
                        String[] distinct_ipSrc = string_ipSrc.Distinct().ToArray();

                        //Filters for each distinct Source IP
                        foreach (string ip_src in distinct_ipSrc)
                        {
                            //Resets filters, then refilters by Source IP
                            sheet.AutoFilterMode = false;

                            fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);
                            visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                            //IP Destination Filter (col E)
                            System.Array ipDst;
                            //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                            if (visibleRange.Columns[5].Value is Array)
                            {
                                ipDst = visibleRange.Columns[5].Value;
                            }
                            else
                            {
                                ipDst = new String[] { visibleRange.Columns[5].Value.ToString() };
                            }
                            //Converts all values to String, then finds all distinct values
                            String[] string_ipDst = ipDst.OfType<object>().Select(o => o.ToString()).ToArray();
                            String[] distinct_ipDst = string_ipDst.Distinct().ToArray();

                            //Filters for each distinct Destination IP under each Source IP
                            foreach (string ip_dst in distinct_ipDst)
                            {
                                //Resets filters, then filters by Source IP and Destination IP
                                sheet.AutoFilterMode = false;
                                fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);

                                fullRange.AutoFilter(5, ip_dst, Excel.XlAutoFilterOperator.xlFilterValues);
                                visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                //Transmitter ID Filter (col F)
                                System.Array trnsmtrID;
                                //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                                if (visibleRange.Columns[6].Value is Array)
                                {
                                    trnsmtrID = visibleRange.Columns[6].Value;
                                }
                                else
                                {
                                    trnsmtrID = new String[] { visibleRange.Columns[6].Value.ToString() };
                                }
                                //Converts all values to String, then finds all distinct values
                                String[] string_TransmitterID = trnsmtrID.OfType<object>().Select(o => o.ToString()).ToArray();
                                String[] distinct_TransmitterID = string_TransmitterID.Distinct().ToArray();

                                //Filters for each distinct transmitter ID under each Destination IP under each Source IP
                                foreach (string trnsmtr_id in distinct_TransmitterID)
                                {
                                    //Resets filters, then refilters by Source IP, Destination IP, and Transmitter ID
                                    sheet.AutoFilterMode = false;
                                    fullRange.AutoFilter(4, ip_src, Excel.XlAutoFilterOperator.xlFilterValues);
                                    fullRange.AutoFilter(5, ip_dst, Excel.XlAutoFilterOperator.xlFilterValues);

                                    fullRange.AutoFilter(6, trnsmtr_id, Excel.XlAutoFilterOperator.xlFilterValues);
                                    visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                    //Reciever ID Filter (col G)
                                    System.Array rcvrID;
                                    //Checks if Column value is Array. If a column has only one value, it returns a String or Int instead of an Array
                                    if (visibleRange.Columns[7].Value is Array)
                                    {
                                        rcvrID = visibleRange.Columns[7].Value;
                                    }
                                    else
                                    {
                                        rcvrID = new String[] { visibleRange.Columns[7].Value.ToString() };
                                    }
                                    //Converts all values to String, then finds all distinct values
                                    String[] string_RecieverID = rcvrID.OfType<object>().Select(o => o.ToString()).ToArray();
                                    String[] distinct_RecieverID = string_RecieverID.Distinct().ToArray();

                                    //Filters for each distinct reciever ID under each transmitter ID under each Destination IP under each Source IP
                                    foreach (string rcvr_id in distinct_RecieverID)
                                    {
                                        Console.WriteLine(ip_src + "|" + ip_dst + "|" + trnsmtr_id + "|" + rcvr_id);

                                        fullRange.AutoFilter(7, rcvr_id, Excel.XlAutoFilterOperator.xlFilterValues);
                                        visibleRange = noHeader.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                        //Number of rows is equal to number of cells divided by number of columns
                                        Console.WriteLine(visibleRange.Count / 12);

                                        rowsProc += (visibleRange.Count / 12);

                                        //Only runs if there is visible data after the filters
                                        if (visibleRange.Count / 12 > 2)
                                        {
                                            visibleRange = sheet.UsedRange.SpecialCells(Excel.XlCellType.xlCellTypeVisible);

                                            visibleRange.Cells[1, 14].Value = "TSN Packets Lost";
                                            visibleRange.Cells[1, 15].Value = "RSN Packets Lost";

                                            double tgap, rgap;
                                            long prevRow = 1, rangeInd = 1, frInd = 0;
                                            int[] firstRows = new int[10];

                                            int[] tsngaps = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
                                            int[] rsngaps = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };

                                            foreach (Excel.Range r in visibleRange.Rows)
                                            {
                                                Console.WriteLine(r.Row);
                                                if (rangeInd == 2)
                                                {
                                                    r.Cells[1, 14].Value = 0;
                                                    r.Cells[1, 15].Value = 0;
                                                }
                                                if (rangeInd > 2)
                                                {
                                                    //Calculates gap in TSN values. A difference of over 1 means a packet was lost. A cell is left blank if the difference is 0 or less, meaning there was either an error or the values reset to 1
                                                    if (r.Cells[1, 8].Value > visibleRange.Cells[prevRow, 8].Value)
                                                    {
                                                        tgap = (r.Cells[1, 8].Value - visibleRange.Cells[prevRow, 8].Value) - 1;
                                                        r.Cells[1, 14].Value = tgap;

                                                        //Counts occourences of losses of 1, 2, 3, 4, 5, 6, 7, and 8+ packets
                                                        switch (tgap)
                                                        {
                                                            case var expression when tgap == 0:
                                                                break;
                                                            case var expression when tgap == 1:
                                                                tsngaps[0]++;
                                                                break;
                                                            case var expression when tgap == 2:
                                                                tsngaps[1]++;
                                                                break;
                                                            case var expression when tgap == 3:
                                                                tsngaps[2]++;
                                                                break;
                                                            case var expression when tgap == 4:
                                                                tsngaps[3]++;
                                                                break;
                                                            case var expression when tgap == 5:
                                                                tsngaps[4]++;
                                                                break;
                                                            case var expression when tgap == 6:
                                                                tsngaps[5]++;
                                                                break;
                                                            case var expression when tgap == 7:
                                                                tsngaps[6]++;
                                                                break;
                                                            case var expression when tgap >= 8:
                                                                tsngaps[7]++;
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }

                                                    //Calculates gap in RSN values. A difference of over 1 means a packet was lost. A cell is left blank if the difference is 0 or less, meaning there was either an error or the values reset to 1
                                                    if (r.Cells[1, 9].Value > visibleRange.Cells[prevRow, 9].Value)
                                                    {
                                                        rgap = (r.Cells[1, 9].Value - visibleRange.Cells[prevRow, 9].Value) - 1;
                                                        r.Cells[1, 15].Value = rgap;

                                                        //Counts occourences of losses of 1, 2, 3, 4, 5, 6, 7, and 8+ packet
                                                        switch (rgap)
                                                        {
                                                            case var expression when rgap == 0:
                                                                break;
                                                            case var expression when rgap == 1:
                                                                rsngaps[0]++;
                                                                break;
                                                            case var expression when rgap == 2:
                                                                rsngaps[1]++;
                                                                break;
                                                            case var expression when rgap == 3:
                                                                rsngaps[2]++;
                                                                break;
                                                            case var expression when rgap == 4:
                                                                rsngaps[3]++;
                                                                break;
                                                            case var expression when rgap == 5:
                                                                rsngaps[4]++;
                                                                break;
                                                            case var expression when rgap == 6:
                                                                rsngaps[5]++;
                                                                break;
                                                            case var expression when rgap == 7:
                                                                rsngaps[6]++;
                                                                break;
                                                            case var expression when rgap >= 8:
                                                                rsngaps[7]++;
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }
                                                }

                                                //If there are over 10 rows of data, take find the indexes of the first 10 visible rows
                                                if (rangeInd < 11)
                                                {
                                                    firstRows[frInd] = r.Row;
                                                    frInd++;
                                                }

                                                prevRow = r.Row;
                                                rangeInd++;
                                            }

                                            //Prints TSN and RSN Gap summary if there are more than 10 rows of data. If there are less then 10 rows, there is not enough room to print the summary
                                            if (!firstRows.Contains(0))
                                            {
                                                visibleRange.Cells[firstRows[0], 17].Value = "TSN Packets Lost";
                                                visibleRange.Cells[firstRows[1], 17].Value = "1 Packet";
                                                visibleRange.Cells[firstRows[2], 17].Value = "2 Packets";
                                                visibleRange.Cells[firstRows[3], 17].Value = "3 Packets";
                                                visibleRange.Cells[firstRows[4], 17].Value = "4 Packets";
                                                visibleRange.Cells[firstRows[5], 17].Value = "5 Packets";
                                                visibleRange.Cells[firstRows[6], 17].Value = "6 Packets";
                                                visibleRange.Cells[firstRows[7], 17].Value = "7 Packets";
                                                visibleRange.Cells[firstRows[8], 17].Value = ">=8 Packets";

                                                visibleRange.Cells[firstRows[0], 18].Value = "# of Occurrences";
                                                visibleRange.Cells[firstRows[1], 18].Value = tsngaps[0];
                                                visibleRange.Cells[firstRows[2], 18].Value = tsngaps[1];
                                                visibleRange.Cells[firstRows[3], 18].Value = tsngaps[2];
                                                visibleRange.Cells[firstRows[4], 18].Value = tsngaps[3];
                                                visibleRange.Cells[firstRows[5], 18].Value = tsngaps[4];
                                                visibleRange.Cells[firstRows[6], 18].Value = tsngaps[5];
                                                visibleRange.Cells[firstRows[7], 18].Value = tsngaps[6];
                                                visibleRange.Cells[firstRows[8], 18].Value = tsngaps[7];

                                                visibleRange.Cells[firstRows[0], 20].Value = "RSN Packets Lost";
                                                visibleRange.Cells[firstRows[1], 20].Value = "1 Packet";
                                                visibleRange.Cells[firstRows[2], 20].Value = "2 Packets";
                                                visibleRange.Cells[firstRows[3], 20].Value = "3 Packets";
                                                visibleRange.Cells[firstRows[4], 20].Value = "4 Packets";
                                                visibleRange.Cells[firstRows[5], 20].Value = "5 Packets";
                                                visibleRange.Cells[firstRows[6], 20].Value = "6 Packets";
                                                visibleRange.Cells[firstRows[7], 20].Value = "7 Packets";
                                                visibleRange.Cells[firstRows[8], 20].Value = ">=8 Packets";

                                                visibleRange.Cells[firstRows[0], 21].Value = "# of Occurrences";
                                                visibleRange.Cells[firstRows[1], 21].Value = tsngaps[0];
                                                visibleRange.Cells[firstRows[2], 21].Value = tsngaps[1];
                                                visibleRange.Cells[firstRows[3], 21].Value = tsngaps[2];
                                                visibleRange.Cells[firstRows[4], 21].Value = tsngaps[3];
                                                visibleRange.Cells[firstRows[5], 21].Value = tsngaps[4];
                                                visibleRange.Cells[firstRows[6], 21].Value = tsngaps[5];
                                                visibleRange.Cells[firstRows[7], 21].Value = tsngaps[6];
                                                visibleRange.Cells[firstRows[8], 21].Value = tsngaps[7];
                                            }

                                            sheet.AutoFilter.ShowAllData();
                                            visibleRange = null;

                                            //Increments progress bar value by number of rows proccessed divided by total number of rows in CSV
                                            this.Dispatcher.Invoke(() =>
                                            {
                                                ProgBar.Value = (rowsProc / sheetLength) * 100;
                                            });
                                        }
                                    }
                                }
                            }
                        }

                        //Waits for Chainage calculation to finish before graphing
                        findChainageTask.Wait();

                        //Creates Packet Loss to Time Graph
                        Excel.ChartObjects Charts = (Excel.ChartObjects)sheet.ChartObjects();
                        Excel.ChartObject TChartObj = (Excel.ChartObject)Charts.Add(1100, 10, 1500, 250);
                        Excel.Chart TPLChart = TChartObj.Chart;

                        //Sets column M (Packets Lost) as the data for the chart
                        Excel.Range TDataRange = sheet.get_Range("N2", "N" + fullRange.Rows.Count);
                        TPLChart.SetSourceData(TDataRange);

                        //Sets column J (Chainage) as the Data for the X-axis in the chart
                        Excel.Range TAxisData = sheet.get_Range("M2", "M" + fullRange.Rows.Count);
                        Excel.Axis TxAxis = (Excel.Axis)TPLChart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
                        TxAxis.CategoryNames = TAxisData;

                        TPLChart.Parent.Placement = Excel.XlPlacement.xlFreeFloating;

                        //Changes Y-axis units
                        TPLChart.Axes(Excel.XlAxisType.xlValue).MajorUnit = 5;

                        //Removes legend from chart
                        TPLChart.HasLegend = false;
                        TPLChart.ChartType = Excel.XlChartType.xlColumnClustered;

                        //Adds title
                        TPLChart.HasTitle = true;
                        TPLChart.ChartTitle.Text = "TSN Packet Loss";


                        //Creates Packet Loss to Time Graph
                        Excel.ChartObject RChartObj = (Excel.ChartObject)Charts.Add(2610, 10, 1500, 250);
                        Excel.Chart RPLChart = RChartObj.Chart;

                        //Sets column M (Packets Lost) as the data for the chart
                        Excel.Range RDataRange = sheet.get_Range("O2", "O" + fullRange.Rows.Count);
                        RPLChart.SetSourceData(RDataRange);

                        //Sets column J (Thales Time Header) as the Data for the X-axis in the chart
                        Excel.Range RAxisData = sheet.get_Range("M2", "M" + fullRange.Rows.Count);
                        Excel.Axis RxAxis = (Excel.Axis)RPLChart.Axes(Excel.XlAxisType.xlCategory, Excel.XlAxisGroup.xlPrimary);
                        RxAxis.CategoryNames = RAxisData;

                        //Prevents chart from moving while filtering
                        RPLChart.Parent.Placement = Excel.XlPlacement.xlFreeFloating;

                        //Changes Y-axis units
                        RPLChart.Axes(Excel.XlAxisType.xlValue).MajorUnit = 5;

                        //Removes legend from chart
                        RPLChart.HasLegend = false;
                        RPLChart.ChartType = Excel.XlChartType.xlColumnClustered;

                        //Adds title
                        RPLChart.HasTitle = true;
                        RPLChart.ChartTitle.Text = "RSN Packet Loss";

                        String savepath = System.IO.Directory.GetParent(filepath.Replace("\"", "")).FullName;

                        //Catches Error due to Save not being selected
                        try
                        {
                            book.SaveAs(savepath + "\\TSNRSNOutput.xlsx", Excel.XlFileFormat.xlOpenXMLWorkbook);
                        }
                        catch
                        {

                        }
                        xlApp.Quit();

                        //Enables buttons and resets progress bar value
                        this.Dispatcher.Invoke(() =>
                        {
                            Merge.IsEnabled = true;
                            Generate.IsEnabled = true;
                            Run.IsEnabled = true;

                            ProgBar.Value = 0;
                        });
                    });
                }
                //Enables buttons, resets progress bar value, and displays error
                catch (Exception TSNChainErr)
                {
                    Merge.IsEnabled = true;
                    Generate.IsEnabled = true;
                    Run.IsEnabled = true;

                    ProgBar.Value = 0;

                    xlApp.Quit();
                    MessageBox.Show(TSNChainErr.ToString());
                }
            }
        }
    }
}
