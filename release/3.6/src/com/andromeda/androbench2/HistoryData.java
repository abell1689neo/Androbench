package com.andromeda.androbench2;

public class HistoryData {
	String date;
	String target;
	int filesize_read;
	int filesize_write;
	int buffersize_seq;
	int buffersize_rnd;
	String useBuffer;	
	double avg_mbps_sr;
	double avg_mbps_sw;
	double avg_mbps_rr;
	double avg_iops_rr;
	double avg_mbps_rw;
	double avg_iops_rw;
	double perf_sqlite_insert;
	double perf_sqlite_update;
	double perf_sqlite_delete;
    double macro_browser_time;
    double macro_market_time;
    double macro_camera_time;
    double macro_camcorder_time;
	
	HistoryData(String date, String target, int filesize_read, int filesize_write, int buffersize_seq, int buffersize_rnd, String useBuffer, double avg_mbps_sr, double avg_mbps_sw, double avg_mbps_rr, double avg_iops_rr, double avg_mbps_rw, double avg_iops_rw, double perf_sqlite_insert, double perf_sqlite_update, double perf_sqlite_delete, double macro_browser_time, double macro_market_time, double macro_camera_time, double macro_camcorder_time){
		this.date = date;
		this.target = target;
		this.filesize_read = filesize_read;
		this.filesize_write = filesize_write;
		this.buffersize_rnd = buffersize_rnd;
		this.buffersize_seq = buffersize_seq;
		this.useBuffer = useBuffer;
		this.avg_mbps_sr = avg_mbps_sr;
		this.avg_mbps_sw = avg_mbps_sw;
		this.avg_mbps_rr = avg_mbps_rr;
		this.avg_iops_rr = avg_iops_rr;
		this.avg_mbps_rw = avg_mbps_rw;
		this.avg_iops_rw = avg_iops_rw;
		this.perf_sqlite_insert = perf_sqlite_insert;
		this.perf_sqlite_update = perf_sqlite_update;
		this.perf_sqlite_delete = perf_sqlite_delete;
	    this.macro_browser_time = macro_browser_time;
	    this.macro_market_time = macro_market_time;
	    this.macro_camera_time = macro_camera_time;
	    this.macro_camcorder_time = macro_camcorder_time;
		
	}
	public String getResult(){
		String result;
		
		result = "Date : " + date + "\n\n";
		
		if(avg_mbps_sr!=0 && avg_mbps_sw!=0 && avg_mbps_rr != 0 && avg_mbps_rw != 0){
			result += "# Micro-benchmark\nTarget: " + target + "\n" + "Use Buffer : " + useBuffer
			+ "\nSEQ RD: " + avg_mbps_sr + " MB/s"
			+ "\nSEQ WR: " + avg_mbps_sw + " MB/s"
			+ "\nRND RD: " + avg_iops_rr + " IOPS(" + buffersize_rnd + "K)"
			+ "\nRND WR: " + avg_iops_rw + " IOPS(" + buffersize_rnd + "K)\n\n";
		}
		
		if(perf_sqlite_insert != 0 && perf_sqlite_update != 0 && perf_sqlite_delete != 0){
			result += "# SQLite benchmark"
			+ "\nInsert: " + perf_sqlite_insert + " TPS"
			+ "\nUpdate: " + perf_sqlite_update + " TPS"
			+ "\nDelete: " + perf_sqlite_delete + " TPS\n\n";
		}
		
		if(macro_browser_time != 0 && macro_market_time != 0 && macro_camera_time != 0 && macro_camcorder_time != 0){
			result += "# Macro benchmark\nTarget: " + target
			+ "\nBrowser: " + macro_browser_time + " msec"
			+ "\nMarket: " + macro_market_time + " msec"
			+ "\nCamera: " + macro_camera_time + " msec"
			+ "\nCamcorder: " + macro_camcorder_time + " msec";
		}
		return result;
		
	}
	public String getDate(){
		return date;
	}
	public String getTarget(){
		return target;
	}
	public double getAvg_mbps_sr(){
		return avg_mbps_sr;
	}
	public double getAvg_mbps_sw(){
		return avg_mbps_sw;
	}
	public double getAvg_mbps_rr(){
		return avg_mbps_rr;
	}
	public double getAvg_iops_rr(){
		return avg_iops_rr;
	}
	public double getAvg_mbps_rw(){
		return avg_mbps_rw;
	}
	public double getAvg_iops_rw(){
		return avg_iops_rw;
	}
	public double getPerf_sqlite_insert(){
		return perf_sqlite_insert;
	}
	public double getPerf_sqlite_update(){
		return perf_sqlite_update;
	}
	public double getPerf_sqlite_delete(){
		return perf_sqlite_delete;
	}
	public double getMacro_browser_time(){
		return macro_browser_time;
	}
	public double getMacro_market_time(){
		return macro_market_time;
	}
	public double getMacro_camera_time(){
		return macro_camera_time;
	}
	public double getMacro_camcorder_time(){
		return macro_camcorder_time;
	}
}